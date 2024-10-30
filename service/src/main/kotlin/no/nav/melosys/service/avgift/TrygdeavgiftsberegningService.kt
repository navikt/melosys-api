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
import no.nav.melosys.service.avgift.aarsavregning.totalbeloep.TotalBeløpBeregner
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
        inntektsPerioder: List<InntektsperiodeDto>,
        skatteforholdsperioder2: List<SkatteforholdTilNorge> = emptyList(),
        inntektsPerioder2: List<Inntektsperiode> = emptyList(),
    ): Set<Trygdeavgiftsperiode> {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
        oppdaterBehandlingsresultatForNyeTrygdeAvgiftsperioder(behandlingsresultat);
        TrygdeavgiftValideringService.validerForTrygdeavgiftberegning(behandlingsresultat, skatteforholdsperioder, inntektsPerioder)

        return if (erPliktigMedlemskapSkattePliktig(skatteforholdsperioder, inntektsPerioder, behandlingsresultat)) {
            leggTilNyeTrygdeavgiftsperioderForPliktigMedlemskapSkattepliktig(skatteforholdsperioder, behandlingsresultat)
        } else {
            val inntektsperioder2Pair = inntektsPerioder2.map { Pair(UUID.randomUUID(), it) }
            val inntektsperiodeDtos = inntektsperioder2Pair.map { it.second.tilInntektsperiodeDto(it.first) }

            val skatteforholdsperioder2Pair = skatteforholdsperioder2.map { Pair(UUID.randomUUID(), it) }
            val skatteforholdsperioderDtos = skatteforholdsperioder2Pair.map { it.second.tilSkatteforholdDto(it.first) }

            val beregnetTrygdeavgift = beregnTrygdeAvgift(behandlingsresultat, skatteforholdsperioderDtos, inntektsperiodeDtos)

            val nyeTrygdeavgiftsperioder = beregnetTrygdeavgift.map { response ->
                lagTrygdeavgiftsperiode(response, skatteforholdsperioder2Pair, inntektsperioder2Pair, behandlingsresultat)
            }.toSet()

            nyeTrygdeavgiftsperioder.also {
                validerTrygdeavgiftBetalesTilNav(behandlingsresultat, beregnetTrygdeavgift)
                behandlingsresultatService.lagreOgFlush(behandlingsresultat)
            }
        }
    }

    private fun lagTrygdeavgiftsperiode(
        response: TrygdeavgiftsberegningResponse,
        skatteforholdsperioder2Pair: List<Pair<UUID, SkatteforholdTilNorge>>,
        inntektsperioder2Pair: List<Pair<UUID, Inntektsperiode>>,
        behandlingsresultat: Behandlingsresultat
    ): Trygdeavgiftsperiode {
        val trygdeavgiftsperiode = Trygdeavgiftsperiode().apply {
            periodeFra = response.beregnetPeriode.periode.fom
            periodeTil = response.beregnetPeriode.periode.tom
            trygdesats = response.beregnetPeriode.sats
            trygdeavgiftsbeløpMd = response.beregnetPeriode.månedsavgift.tilPenger()
            grunnlagSkatteforholdTilNorge = skatteforholdsperioder2Pair.find { it.first == response.grunnlag.skatteforholdsperiodeId }?.second
            grunnlagInntekstperiode = inntektsperioder2Pair.find { it.first == response.grunnlag.inntektsperiodeId }?.second
        }.apply {
            grunnlagMedlemskapsperiode = behandlingsresultat.medlemskapsperioder
                .find {
                    idToUUid(it.id) == response.grunnlag.medlemskapsperiodeId
                }
            grunnlagMedlemskapsperiode.trygdeavgiftsperioder.add(this)
        }

        return trygdeavgiftsperiode
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

    private fun validerTrygdeavgiftBetalesTilNav(
        behandlingsresultat: Behandlingsresultat,
        beregnetTrygdeavgift: List<TrygdeavgiftsberegningResponse>
    ) {
        val erAlleTrygdeavgiftbelopNull = beregnetTrygdeavgift.all { it.beregnetPeriode.månedsavgift.verdi.compareTo(BigDecimal.ZERO) == 0 }
        val skalKunBetalesTilSkatt =
            trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat) == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT
        if (skalKunBetalesTilSkatt && !erAlleTrygdeavgiftbelopNull) {
            throw IllegalStateException("Trygdeavgift skal ikke betales til NAV. Beregnet trygdeavgift må derfor være 0.")
        }
    }

    companion object {

        private fun Inntektsperiode.tilInntektsperiodeDto(id: UUID): InntektsperiodeDto {
            val mndsBelop = if (isErMaanedsbelop) {
                PengerDto(avgiftspliktigInntekt)
            } else {
                val kalkulertBelop = TotalBeløpBeregner.månedligBeløpForTotalbeløp(fomDato, tomDato, avgiftspliktigInntekt.verdi)
                PengerDto(kalkulertBelop)
            }

            return InntektsperiodeDto(
                id,
                DatoPeriodeDto(fomDato, tomDato),
                type,
                isArbeidsgiversavgiftBetalesTilSkatt,
                mndsBelop,
                true
            )
        }

        private fun SkatteforholdTilNorge.tilSkatteforholdDto(id: UUID) = SkatteforholdsperiodeDto(
            id,
            DatoPeriodeDto(fomDato, tomDato),
            skatteplikttype
        )

        private fun idToUUid(id: Long): UUID {
            return UUID.nameUUIDFromBytes(id.toString().toByteArray())
        }
    }
}
