package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.trygdeavgift.AvgiftsdekningerFraTrygdedekning
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import no.nav.melosys.service.avgift.dto.OppdaterTrygdeavgiftsgrunnlagRequest
import no.nav.melosys.service.avgift.dto.SkatteforholdTilNorgeRequest
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@Service
class TrygdeavgiftsberegningService
    (
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
        oppdaterTrygdeavgiftsgrunnlagRequest: OppdaterTrygdeavgiftsgrunnlagRequest
    ): Set<Trygdeavgiftsperiode> {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
        behandlingsresultat.trygdeavgiftType = Trygdeavgift_typer.FORELØPIG
        behandlingsresultat.medlemskapsperioder.forEach {
            it.trygdeavgiftsperioder.clear()
        }

        behandlingsresultatService.lagreOgFlush(behandlingsresultat)

        TrygdeavgiftValideringService.validerTrygdeavgiftberegningRequest(oppdaterTrygdeavgiftsgrunnlagRequest, behandlingsresultat)

        val innvilgedeMedlemskapsperioder = behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() }

        val medlemskapsperiodeDtos = mapTilMedlemskapsperiodeDtos(innvilgedeMedlemskapsperioder)
        val skatteforholdsperioderDtos = mapTilSkatteforholdsperiodeDtos(oppdaterTrygdeavgiftsgrunnlagRequest)
        val inntektsperioderDtos = mapInntektsperiodeDtos(oppdaterTrygdeavgiftsgrunnlagRequest)

        val trygdeavgiftsberegningRequest = TrygdeavgiftsberegningRequest(
            medlemskapsperiodeDtos,
            skatteforholdsperioderDtos,
            inntektsperioderDtos,
            hentFødselsdatoOmViHarTjenstligBehov(behandlingsresultatID, innvilgedeMedlemskapsperioder)
        )

        val beregnetTrygdeavgift = trygdeavgiftConsumer.beregnTrygdeavgift(trygdeavgiftsberegningRequest)

        val nyeTrygdeavgiftsperioder = lagOgLeggTilNyeTrygdeavgiftsperioder(
            behandlingsresultat,
            skatteforholdsperioderDtos,
            inntektsperioderDtos,
            beregnetTrygdeavgift
        )

        val skalKunBetalesTilSkatt =
            trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat) == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT
        if (skalKunBetalesTilSkatt && !erAlleTrygdeavgiftbelopNull(beregnetTrygdeavgift)) {
            throw IllegalStateException("Trygdeavgift skal ikke betales til NAV. Beregnet trygdeavgift må derfor være 0.")
        }

        behandlingsresultatService.lagreOgFlush(behandlingsresultat)
        return nyeTrygdeavgiftsperioder
    }

    private fun mapTilMedlemskapsperiodeDtos(medlemskapsperioder: List<Medlemskapsperiode>) =
        medlemskapsperioder.map {
            MedlemskapsperiodeDto(
                it.idToUUID(),
                DatoPeriodeDto(it.fom, it.tom),
                AvgiftsdekningerFraTrygdedekning.avgiftsdekningerFraTrygdedekning(it.trygdedekning),
                it.medlemskapstype
            )
        }.toSet()


    private fun mapTilSkatteforholdsperiodeDtos(oppdaterTrygdeavgiftsgrunnlagRequest: OppdaterTrygdeavgiftsgrunnlagRequest) =
        oppdaterTrygdeavgiftsgrunnlagRequest.skatteforholdTilNorgeList.map {
            SkatteforholdsperiodeDto(it.toUUID(), DatoPeriodeDto(it.fomDato, it.tomDato), it.skatteplikttype)
        }.toSet()


    private fun mapInntektsperiodeDtos(oppdaterTrygdeavgiftsgrunnlagRequest: OppdaterTrygdeavgiftsgrunnlagRequest) =
        oppdaterTrygdeavgiftsgrunnlagRequest.inntektskilder.map {
            InntektsperiodeDto(
                UUID.randomUUID(),
                DatoPeriodeDto(it.fomDato, it.tomDato),
                it.type,
                it.arbeidsgiversavgiftBetales,
                PengerDto(it.avgiftspliktigInntektMnd ?: 0.toBigDecimal())
            )
        }

    private fun hentFødselsdatoOmViHarTjenstligBehov(behandlingsresultatID: Long, medlemskapsperioder: List<Medlemskapsperiode>): LocalDate? {
        if (medlemskapsperioder.any { it.erPliktig() }) {
            val fagsak = behandlingService.hentBehandling(behandlingsresultatID).fagsak
            return persondataService.hentPerson(fagsak.hentBrukersAktørID()).fødselsdato
        }
        return null
    }

    private fun lagOgLeggTilNyeTrygdeavgiftsperioder(
        behandlingsresultat: Behandlingsresultat,
        skatteForholdTilNorgeDtos: Set<SkatteforholdsperiodeDto>,
        inntektsperiodeDtos: List<InntektsperiodeDto>,
        beregnetTrygdeavgift: List<TrygdeavgiftsberegningResponse>,
    ): Set<Trygdeavgiftsperiode> {

        val skatteforholdTilNorge = skatteForholdTilNorgeDtos.map {
            Pair(
                it.id,
                SkatteforholdTilNorge().apply {
                    this.fomDato = it.periode.fom
                    this.tomDato = it.periode.tom
                    this.skatteplikttype = it.skatteforhold
                })
        }

        val inntektsperioder = inntektsperiodeDtos.map {
            Pair(
                it.id,
                Inntektsperiode().apply {
                    this.fomDato = it.periode.fom
                    this.tomDato = it.periode.tom
                    this.type = it.inntektskilde
                    this.isArbeidsgiversavgiftBetalesTilSkatt = it.arbeidsgiverBetalerAvgift == true
                    this.avgiftspliktigInntektMnd = it.månedsbeløp?.tilPenger()
                })
        }

        return beregnetTrygdeavgift.map {
            lagTrygdeavgiftsperiode(
                behandlingsresultat,
                skatteforholdTilNorge,
                inntektsperioder,
                it
            )
        }.toSet()
    }

    private fun lagTrygdeavgiftsperiode(
        behandlingsresultat: Behandlingsresultat,
        skatteforholdTilNorge: List<Pair<UUID, SkatteforholdTilNorge>>,
        inntektsperioder: List<Pair<UUID, Inntektsperiode>>,
        trygdeavgiftsberegningResponse: TrygdeavgiftsberegningResponse,
    ): Trygdeavgiftsperiode {
        val beregnetPeriode = trygdeavgiftsberegningResponse.beregnetPeriode
        val beregningsgrunnlag = trygdeavgiftsberegningResponse.grunnlag

        return Trygdeavgiftsperiode().apply {
            this.periodeFra = beregnetPeriode.periode.fom
            this.periodeTil = beregnetPeriode.periode.tom
            this.trygdesats = beregnetPeriode.sats
            this.trygdeavgiftsbeløpMd = beregnetPeriode.månedsavgift.tilPenger()
            this.grunnlagMedlemskapsperiode = behandlingsresultat.medlemskapsperioder
                .find {
                    idToUUid(it.id) == beregningsgrunnlag.medlemskapsperiodeId
                }
            this.grunnlagSkatteforholdTilNorge = skatteforholdTilNorge.find {
                it.first == beregningsgrunnlag.skatteforholdsperiodeId
            }?.second
            this.grunnlagInntekstperiode = inntektsperioder.find {
                it.first == beregningsgrunnlag.inntektsperiodeId
            }?.second
        }.apply {
            this.grunnlagMedlemskapsperiode.trygdeavgiftsperioder.add(this)
        }
    }

    private fun erAlleTrygdeavgiftbelopNull(beregnetTrygdeavgift: List<TrygdeavgiftsberegningResponse>): Boolean {
        return beregnetTrygdeavgift.all { it.beregnetPeriode.månedsavgift.verdi.compareTo(BigDecimal.ZERO) == 0 }
    }

    @Transactional(readOnly = true)
    fun hentTrygdeavgiftsberegning(behandlingsresultatID: Long): Set<Trygdeavgiftsperiode> {
        return behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
            .trygdeavgiftsperioder
    }

    @Transactional(readOnly = true)
    fun hentOpprinneligTrygdeavgiftsperioder(behandlingsresultatID: Long): Set<Trygdeavgiftsperiode> {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
        val behandliong = behandlingsresultat.behandling
        behandliong.opprinneligBehandling?.let {
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

    companion object {
        fun SkatteforholdTilNorgeRequest.toUUID(): UUID {
            return UUID.nameUUIDFromBytes("${this.fomDato}${this.tomDato}${this.skatteplikttype}".toByteArray())
        }

        fun Medlemskapsperiode.idToUUID(): UUID {
            return idToUUid(this.id)
        }

        private fun idToUUid(id: Long): UUID {
            return UUID.nameUUIDFromBytes(id.toString().toByteArray())
        }
    }
}
