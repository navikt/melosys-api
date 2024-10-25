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
import no.nav.melosys.integrasjon.trygdeavgift.AvgiftsdekningerFraTrygdedekning
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import no.nav.melosys.service.avgift.aarsavregning.totalbeloep.TotalBeløpBeregner
import no.nav.melosys.service.avgift.dto.InntektskildeRequest
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
        oppdaterTrygdeavgiftsgrunnlagRequest: OppdaterTrygdeavgiftsgrunnlagRequest
    ): Set<Trygdeavgiftsperiode> {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
        oppdaterBehandlingsresultatForNyeTrygdeAvgiftsperioder(behandlingsresultat);
        TrygdeavgiftValideringService.validerTrygdeavgiftberegningRequest(oppdaterTrygdeavgiftsgrunnlagRequest, behandlingsresultat)

        return if (erPliktigMedlemskapSkattePliktig(oppdaterTrygdeavgiftsgrunnlagRequest, behandlingsresultat)) {
            leggTilNyeTrygdeavgiftsperioderForPliktigMedlemskapSkattepliktig(oppdaterTrygdeavgiftsgrunnlagRequest, behandlingsresultat)
        } else {
            leggTilNyeTrygdeavgiftsperioder(behandlingsresultat, oppdaterTrygdeavgiftsgrunnlagRequest).also {
                behandlingsresultatService.lagreOgFlush(behandlingsresultat)
            }
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
        oppdaterTrygdeavgiftsgrunnlagRequest: OppdaterTrygdeavgiftsgrunnlagRequest
    ): Set<Trygdeavgiftsperiode> {
        val beregnetTrygdeavgift = beregnTrygdeAvgift(behandlingsresultat, oppdaterTrygdeavgiftsgrunnlagRequest)

        val nyeTrygdeavgiftsperioder = lagTrygdeAvgiftsperioderOgOppdaterBehandlingsresultat(
            behandlingsresultat,
            oppdaterTrygdeavgiftsgrunnlagRequest,
            beregnetTrygdeavgift
        )

        return nyeTrygdeavgiftsperioder
    }

    private fun beregnTrygdeAvgift(
        behandlingsresultat: Behandlingsresultat,
        oppdaterTrygdeavgiftsgrunnlagRequest: OppdaterTrygdeavgiftsgrunnlagRequest
    ): List<TrygdeavgiftsberegningResponse> {
        val innvilgedeMedlemskapsperioder = behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() }

        val medlemskapsperiodeDtos = mapTilMedlemskapsperiodeDtos(innvilgedeMedlemskapsperioder)
        val skatteforholdsperioderDtos = mapTilSkatteforholdsperiodeDtos(oppdaterTrygdeavgiftsgrunnlagRequest)
        val inntektsperioderDtos = mapInntektsperiodeDtos(oppdaterTrygdeavgiftsgrunnlagRequest)
        val foedselDato = hentFødselsdatoOmViHarTjenstligBehov(behandlingsresultat.id, innvilgedeMedlemskapsperioder)


        val beregnetTrygdeavgift = beregnTrygdeAvgift(medlemskapsperiodeDtos, skatteforholdsperioderDtos, inntektsperioderDtos, foedselDato)
        return beregnetTrygdeavgift
    }

    private fun lagTrygdeAvgiftsperioderOgOppdaterBehandlingsresultat(
        behandlingsresultat: Behandlingsresultat,
        oppdaterTrygdeavgiftsgrunnlagRequest: OppdaterTrygdeavgiftsgrunnlagRequest,
        beregnetTrygdeavgift: List<TrygdeavgiftsberegningResponse>,
    ): Set<Trygdeavgiftsperiode> {
        val nyeTrygdeavgiftsperioder = lagOgLeggTilNyeTrygdeavgiftsperioder(
            behandlingsresultat,
            oppdaterTrygdeavgiftsgrunnlagRequest.skatteforholdTilNorgeList,
            oppdaterTrygdeavgiftsgrunnlagRequest.inntektskilder,
            beregnetTrygdeavgift
        )

        val skalKunBetalesTilSkatt =
            trygdeavgiftMottakerService.getTrygdeavgiftMottaker(behandlingsresultat) == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT
        if (skalKunBetalesTilSkatt && !erAlleTrygdeavgiftbelopNull(beregnetTrygdeavgift)) {
            throw IllegalStateException("Trygdeavgift skal ikke betales til NAV. Beregnet trygdeavgift må derfor være 0.")
        }

        return nyeTrygdeavgiftsperioder
    }

    private fun erPliktigMedlemskapSkattePliktig(
        oppdaterTrygdeavgiftsgrunnlagRequest: OppdaterTrygdeavgiftsgrunnlagRequest,
        behandlingsresultat: Behandlingsresultat
    ): Boolean {
        val erPliktigMedlemskap = behandlingsresultat.medlemskapsperioder
            .filter { it.erInnvilget() }
            .all { it.erPliktig() }

        val inntektskilderErTomt = oppdaterTrygdeavgiftsgrunnlagRequest.inntektskilder.isEmpty()
        val alleSkatteforholdErSkattepliktige =
            oppdaterTrygdeavgiftsgrunnlagRequest.skatteforholdTilNorgeList.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }

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
        oppdaterTrygdeavgiftsgrunnlagRequest: OppdaterTrygdeavgiftsgrunnlagRequest,
        behandlingsresultat: Behandlingsresultat,
    ): Set<Trygdeavgiftsperiode> {
        if (oppdaterTrygdeavgiftsgrunnlagRequest.skatteforholdTilNorgeList.size != 1) {
            throw IllegalStateException("Det skal ikke være flere enn en skatteforholdsperiode når medlemskapet er pliktig og skattepliktig")
        }

        val innvilgedeMedlemskapsperioder = behandlingsresultat.medlemskapsperioder.filter { it.erInnvilget() }

        return innvilgedeMedlemskapsperioder.map {
            val skatteforholdTilNorge = SkatteforholdTilNorge().apply {
                this.fomDato = oppdaterTrygdeavgiftsgrunnlagRequest.skatteforholdTilNorgeList.first().fomDato
                this.tomDato = oppdaterTrygdeavgiftsgrunnlagRequest.skatteforholdTilNorgeList.first().tomDato
                this.skatteplikttype = oppdaterTrygdeavgiftsgrunnlagRequest.skatteforholdTilNorgeList.first().skatteplikttype
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
            SkatteforholdsperiodeDto(it.id, DatoPeriodeDto(it.fomDato, it.tomDato), it.skatteplikttype)
        }.toSet()


    private fun mapInntektsperiodeDtos(oppdaterTrygdeavgiftsgrunnlagRequest: OppdaterTrygdeavgiftsgrunnlagRequest): List<InntektsperiodeDto> {
        return oppdaterTrygdeavgiftsgrunnlagRequest.inntektskilder.map {
            val avgiftsPliktigInntekt = if (it.erMaanedsbelop) it.avgiftspliktigInntekt else TotalBeløpBeregner.månedligBeløpForTotalbeløp(
                it.fomDato,
                it.tomDato, it.avgiftspliktigInntekt!!
            )

            InntektsperiodeDto(
                id = it.id,
                periode = DatoPeriodeDto(it.fomDato, it.tomDato),
                inntektskilde = it.type,
                arbeidsgiverBetalerAvgift = it.arbeidsgiversavgiftBetales,
                månedsbeløp = PengerDto(avgiftsPliktigInntekt ?: 0.toBigDecimal()),
                erMaanedsbelop = it.erMaanedsbelop
            )
        }
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
        skatteForholdTilNorgeSubstitutt: List<SkatteforholdTilNorgeRequest>,
        inntektsperiodeDtosSubstitutt: List<InntektskildeRequest>,
        beregnetTrygdeavgift: List<TrygdeavgiftsberegningResponse>
    ): Set<Trygdeavgiftsperiode> {
        val skatteforholdTilNorge = skatteForholdTilNorgeSubstitutt.map { Pair(it.id, SkatteforholdTilNorgeRequest.tilSkatteforhold(it)) }
        val inntektsperioder = inntektsperiodeDtosSubstitutt.map { Pair(it.id, InntektskildeRequest.tilInntektskilde(it)) }

        return beregnetTrygdeavgift.map { // TODO
            lagTrygdeavgiftsperiode(
                it,
                behandlingsresultat,
                skatteforholdTilNorge,
                inntektsperioder,
            )
        }.toSet()
    }

    private fun lagTrygdeavgiftsperiode(
        trygdeavgiftsberegningResponse: TrygdeavgiftsberegningResponse,
        behandlingsresultat: Behandlingsresultat,
        skatteforholdTilNorge: List<Pair<UUID, SkatteforholdTilNorge>>,
        inntektsperioder: List<Pair<UUID, Inntektsperiode>>,
    ): Trygdeavgiftsperiode {
        val beregnetPeriode = trygdeavgiftsberegningResponse.beregnetPeriode
        val beregningsgrunnlag = trygdeavgiftsberegningResponse.grunnlag

        val trygdeAvgiftsperiode = Trygdeavgiftsperiode().apply {
            this.periodeFra = beregnetPeriode.periode.fom
            this.periodeTil = beregnetPeriode.periode.tom
            this.trygdesats = beregnetPeriode.sats
            this.trygdeavgiftsbeløpMd = beregnetPeriode.månedsavgift.tilPenger()

            this.grunnlagSkatteforholdTilNorge = skatteforholdTilNorge.find {
                it.first == beregningsgrunnlag.skatteforholdsperiodeId
            }?.second

            this.grunnlagInntekstperiode = inntektsperioder.find {
                it.first == beregningsgrunnlag.inntektsperiodeId
            }?.second
        }.apply {
            this.grunnlagMedlemskapsperiode = behandlingsresultat.medlemskapsperioder
                .find {
                    idToUUid(it.id) == beregningsgrunnlag.medlemskapsperiodeId
                }
            this.grunnlagMedlemskapsperiode.trygdeavgiftsperioder.add(this)
        }

        return trygdeAvgiftsperiode
    }


    private fun erAlleTrygdeavgiftbelopNull(beregnetTrygdeavgift: List<TrygdeavgiftsberegningResponse>): Boolean {
        return beregnetTrygdeavgift.all { it.beregnetPeriode.månedsavgift.verdi.compareTo(BigDecimal.ZERO) == 0 }
    }

    companion object {
        fun Medlemskapsperiode.idToUUID(): UUID {
            return idToUUid(this.id)
        }

        private fun idToUUid(id: Long): UUID {
            return UUID.nameUUIDFromBytes(id.toString().toByteArray())
        }
    }
}
