package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import no.nav.melosys.integrasjon.trygdeavgift.dto.MedlemskapsperiodeDto.Companion.tilMedlemskapsperiodeDtos
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Service
class TrygdeavgiftsberegningService(
    private val behandlingService: BehandlingService,
    private val eregFasade: EregFasade,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val persondataService: PersondataService,
    private val trygdeavgiftConsumer: TrygdeavgiftConsumer,
) {

    @Transactional
    fun beregnOgLagreTrygdeavgift(
        behandlingsresultatID: Long,
        skatteforholdsperioder: List<SkatteforholdsperiodeDto>,
        inntektsPerioder: List<InntektsperiodeDto>
    ): Set<Trygdeavgiftsperiode> {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
        oppdaterBehandlingsresultatForNyeTrygdeAvgiftsperioder(behandlingsresultat);
        TrygdeavgiftValideringService.validerForTrygdeavgiftberegning(behandlingsresultat, skatteforholdsperioder, inntektsPerioder)

        return if (erPliktigMedlemskapSkattePliktig(skatteforholdsperioder, inntektsPerioder, behandlingsresultat)) {
            leggTilNyeTrygdeavgiftsperioderForPliktigMedlemskapSkattepliktig(skatteforholdsperioder, behandlingsresultat)
        } else {
            leggTilNyeTrygdeavgiftsperioder(behandlingsresultat, skatteforholdsperioder, inntektsPerioder)
                .also { behandlingsresultatService.lagreOgFlush(behandlingsresultat) }
        }
    }

    @Transactional(readOnly = true)
    fun hentTrygdeavgiftsberegning(behandlingsresultatID: Long): Set<Trygdeavgiftsperiode> {
        return behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
            .trygdeavgiftsperioder
    }

    @Transactional(readOnly = true)
    fun hentOpprinneligTrygdeavgiftsperioder(behandlingsresultatID: Long): Set<Trygdeavgiftsperiode> {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
        val behandling = behandlingsresultat.behandling
        behandling.opprinneligBehandling?.let {
            return behandlingsresultatService.hentBehandlingsresultat(it.id).trygdeavgiftsperioder
                ?: emptySet()
        }
        return emptySet()
    }

    @Transactional(readOnly = true)
    fun finnFakturamottakerNavn(behandlingID: Long): String {
        val fagsak = behandlingService.hentBehandling(behandlingID).fagsak
        fagsak.finnFullmektig(Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT)
            .let {
                if (it == null)
                    return persondataService.hentSammensattNavn(fagsak.hentBrukersAktørID())
                if (it.erPerson())
                    return persondataService.hentSammensattNavn(it.personIdent)
                return eregFasade.hentOrganisasjonNavn(it.orgnr)
            }
    }

    private fun leggTilNyeTrygdeavgiftsperioder(
        behandlingsresultat: Behandlingsresultat,
        skatteforholdsperioder: List<SkatteforholdsperiodeDto>,
        inntektsPerioder: List<InntektsperiodeDto>
    ): Set<Trygdeavgiftsperiode> {
        val beregnetTrygdeavgift = beregnTrygdeAvgift(behandlingsresultat, skatteforholdsperioder, inntektsPerioder)

        val skatteforholdTilNorgePair = skatteforholdsperioder.map { Pair(it.id, SkatteforholdsperiodeDto.tilSkatteforhold(it)) }
        val inntektsperioderPair = inntektsPerioder.map { Pair(it.id, InntektsperiodeDto.tilInntektskilde(it)) }


        val nyeTrygdeavgiftsperioder = beregnetTrygdeavgift.map {
            lagTrygdeavgiftsperiode(
                it,
                behandlingsresultat,
                skatteforholdTilNorgePair,
                inntektsperioderPair
            )
        }.toSet()

        val skalKunBetalesTilSkatt =
            trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat) == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT
        if (skalKunBetalesTilSkatt && !erAlleTrygdeavgiftbelopNull(beregnetTrygdeavgift)) {
            throw IllegalStateException("Trygdeavgift skal ikke betales til NAV. Beregnet trygdeavgift må derfor være 0.")
        }

        return nyeTrygdeavgiftsperioder
    }

    private fun beregnTrygdeAvgift(
        behandlingsresultat: Behandlingsresultat,
        skatteforholdsperioderTemp: List<SkatteforholdsperiodeDto>,
        inntektsPerioderTemp: List<InntektsperiodeDto>
    ): List<TrygdeavgiftsberegningResponse> {
        val innvilgedeMedlemskapsperioder = behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() }
        val medlemskapsperiodeDtos = innvilgedeMedlemskapsperioder.tilMedlemskapsperiodeDtos()
        val foedselDato = hentFødselsdatoOmViHarTjenstligBehov(behandlingsresultat.id, innvilgedeMedlemskapsperioder)


        val beregnetTrygdeavgift = beregnTrygdeAvgift(medlemskapsperiodeDtos, skatteforholdsperioderTemp.toSet(), inntektsPerioderTemp, foedselDato)

        return beregnetTrygdeavgift
    }

    private fun erPliktigMedlemskapSkattePliktig(
        skatteforholdsperioderTemp: List<SkatteforholdsperiodeDto>,
        inntektsPerioderTemp: List<InntektsperiodeDto>,
        behandlingsresultat: Behandlingsresultat
    ): Boolean {
        val erPliktigMedlemskap = behandlingsresultat.medlemskapsperioder
            .filter { it.erInnvilget() }
            .all { it.erPliktig() }

        val inntektskilderErTomt = inntektsPerioderTemp.isEmpty()
        val alleSkatteforholdErSkattepliktige =
            skatteforholdsperioderTemp.all { it.skatteforhold == Skatteplikttype.SKATTEPLIKTIG }

        return erPliktigMedlemskap && inntektskilderErTomt && alleSkatteforholdErSkattepliktige
    }

    private fun oppdaterBehandlingsresultatForNyeTrygdeAvgiftsperioder(behandlingsresultat: Behandlingsresultat) {
        behandlingsresultat.trygdeavgiftType = Trygdeavgift_typer.FORELØPIG
        behandlingsresultat.medlemskapsperioder.forEach {
            it.trygdeavgiftsperioder.clear()
        }

        behandlingsresultatService.lagreOgFlush(behandlingsresultat)
    }

    private fun leggTilNyeTrygdeavgiftsperioderForPliktigMedlemskapSkattepliktig(
        skatteforholdsperioderTemp: List<SkatteforholdsperiodeDto>,
        behandlingsresultat: Behandlingsresultat,
    ): Set<Trygdeavgiftsperiode> {
        if (skatteforholdsperioderTemp.size != 1) {
            throw IllegalStateException("Det skal ikke være flere enn en skatteforholdsperiode når medlemskapet er pliktig og skattepliktig")
        }

        val innvilgedeMedlemskapsperioder = behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() }

        return innvilgedeMedlemskapsperioder.map {
            val skatteforholdTilNorge = SkatteforholdTilNorge().apply {
                this.fomDato = skatteforholdsperioderTemp.first().periode.fom
                this.tomDato = skatteforholdsperioderTemp.first().periode.tom
                this.skatteplikttype = skatteforholdsperioderTemp.first().skatteforhold
            }
            val trygdeavgiftsperiode = Trygdeavgiftsperiode().apply {
                this.periodeFra = it.fom
                this.periodeTil = it.tom
                this.trygdesats = BigDecimal.ZERO
                this.trygdeavgiftsbeløpMd = Penger(BigDecimal.ZERO)
                this.grunnlagMedlemskapsperiode = it
                this.grunnlagSkatteforholdTilNorge = skatteforholdTilNorge
            }
            it.trygdeavgiftsperioder.add(trygdeavgiftsperiode)

            trygdeavgiftsperiode
        }.toSet()
    }

    private fun beregnTrygdeAvgift(
        medlemskapsperiodeDtos: Set<MedlemskapsperiodeDto>,
        skatteforholdsperioderDtos: Set<SkatteforholdsperiodeDto>,
        inntektsperioderDtos: List<InntektsperiodeDto>,
        foedselDato: LocalDate?
    ): List<TrygdeavgiftsberegningResponse> {
        val trygdeavgiftsberegningRequest = TrygdeavgiftsberegningRequest(
            medlemskapsperiodeDtos,
            skatteforholdsperioderDtos,
            inntektsperioderDtos,
            foedselDato
        )
        val beregnetTrygdeavgift = trygdeavgiftConsumer.beregnTrygdeavgift(trygdeavgiftsberegningRequest)
        return beregnetTrygdeavgift;
    }

    private fun hentFødselsdatoOmViHarTjenstligBehov(behandlingsresultatID: Long, medlemskapsperioder: List<Medlemskapsperiode>): LocalDate? {
        if (medlemskapsperioder.any { it.erPliktig() }) {
            val fagsak = behandlingService.hentBehandling(behandlingsresultatID).fagsak
            return persondataService.hentPerson(fagsak.hentBrukersAktørID()).fødselsdato
        }
        return null
    }

    private fun lagTrygdeavgiftsperiode(
        trygdeavgiftsberegningResponse: TrygdeavgiftsberegningResponse,
        behandlingsresultat: Behandlingsresultat,
        skatteforholdTilNorge: List<Pair<UUID, SkatteforholdTilNorge>>,
        inntektsPerioderTemp: List<Pair<UUID, Inntektsperiode>>
    ): Trygdeavgiftsperiode {
        val beregnetPeriode = trygdeavgiftsberegningResponse.beregnetPeriode
        val beregningsgrunnlag = trygdeavgiftsberegningResponse.grunnlag

        val trygdeAvgiftsperiode = Trygdeavgiftsperiode().apply {
            periodeFra = beregnetPeriode.periode.fom
            periodeTil = beregnetPeriode.periode.tom
            trygdesats = beregnetPeriode.sats
            trygdeavgiftsbeløpMd = beregnetPeriode.månedsavgift.tilPenger()

            grunnlagSkatteforholdTilNorge = skatteforholdTilNorge.find {
                it.first == beregningsgrunnlag.skatteforholdsperiodeId
            }?.second

            grunnlagInntekstperiode = inntektsPerioderTemp.find {
                it.first == beregningsgrunnlag.inntektsperiodeId
            }?.second

        }.apply {
            grunnlagMedlemskapsperiode = behandlingsresultat.medlemskapsperioder
                .find {
                    idToUUid(it.id) == beregningsgrunnlag.medlemskapsperiodeId
                }
            grunnlagMedlemskapsperiode.trygdeavgiftsperioder.add(this)
        }

        return trygdeAvgiftsperiode
    }

    private fun erAlleTrygdeavgiftbelopNull(beregnetTrygdeavgift: List<TrygdeavgiftsberegningResponse>): Boolean {
        return beregnetTrygdeavgift.all { it.beregnetPeriode.månedsavgift.verdi.compareTo(BigDecimal.ZERO) == 0 }
    }

    companion object {
        private fun idToUUid(id: Long): UUID {
            return UUID.nameUUIDFromBytes(id.toString().toByteArray())
        }
    }
}
