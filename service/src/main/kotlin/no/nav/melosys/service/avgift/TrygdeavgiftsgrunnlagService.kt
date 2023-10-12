package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.avgift.dto.InntektskildeRequest
import no.nav.melosys.service.avgift.dto.OppdaterTrygdeavgiftsgrunnlagRequest
import no.nav.melosys.service.avgift.dto.SkatteforholdTilNorgeRequest
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.threeten.extra.LocalDateRange
import java.time.DateTimeException

@Service
class TrygdeavgiftsgrunnlagService(private val behandlingsresultatService: BehandlingsresultatService, private val trygdeavgiftsMottakerService: TrygdeavgiftsMottakerService) {

    @Transactional
    fun oppdaterTrygdeavgiftsgrunnlag(
        behandlingID: Long, request: OppdaterTrygdeavgiftsgrunnlagRequest
    ): Trygdeavgiftsgrunnlag {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        val medlemAvFolketrygden = behandlingsresultat.medlemAvFolketrygden

        validerMedlemskapsperioder(medlemAvFolketrygden)
        validerTrygdeavgiftsgrunnlag(request, medlemAvFolketrygden.medlemskapsperioder)
        fjernTrygdeavgiftsperioderOmDeFinnes(medlemAvFolketrygden.fastsattTrygdeavgift)

        medlemAvFolketrygden.fastsattTrygdeavgift = eksisterendeEllerNyFastsattTrygdeavgift(medlemAvFolketrygden)
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag =
            eksisterendeEllerNyttTrygdeavgiftsgrunnlag(medlemAvFolketrygden).apply {
                this.skatteforholdTilNorge = lagSkatteforholdTilNorge(request)
                this.inntektsperioder = lagInntektsperioder(request)
            }
        behandlingsresultatService.lagre(behandlingsresultat)

        return hentTrygdeavgiftsgrunnlag(behandlingID)
            ?: throw FunksjonellException("Noe skjedde ved lagring av trygdeavgiftsgrunnlaget")
    }

    fun fjernTrygdeavgiftsperioderOmDeFinnes(fastsattTrygdeavgift: FastsattTrygdeavgift?) {
        fastsattTrygdeavgift?.trygdeavgiftsperioder?.clear()
    }

    private fun validerMedlemskapsperioder(medlemAvFolketrygden: MedlemAvFolketrygden) {
        if (medlemAvFolketrygden.medlemskapsperioder.isEmpty()) {
            throw FunksjonellException("Kan ikke oppdatere trygdeavgiftsgrunnlaget før medlemskapsperioder finnes")
        }
        medlemAvFolketrygden.utledMedlemskapsperiodeFom()
            ?: throw FunksjonellException("Klarte ikke finne startdatoen på medlemskapet")
        medlemAvFolketrygden.utledMedlemskapsperiodeTom()
            ?: throw FunksjonellException("Klarte ikke finne sluttdatoen på medlemskapet")
    }

    private fun validerTrygdeavgiftsgrunnlag(request: OppdaterTrygdeavgiftsgrunnlagRequest, medlemskapsperioder: Collection<Medlemskapsperiode>) {
        val innvilgedeMedlemskapsperioder = medlemskapsperioder.filter { it.erInnvilget() }
        validerAtInntekstperioderDekkerInnvilgedeMedlemskapsperioder(
            request.inntektskilder,
            innvilgedeMedlemskapsperioder
        )
        validerAtSkatteforholdTilNorgeDekkerInnvilgedeMedlemskapsperioderOgOverlapperIkke(
            request.skatteforholdTilNorgeList,
            innvilgedeMedlemskapsperioder
        )
    }

    private fun eksisterendeEllerNyFastsattTrygdeavgift(medlemAvFolketrygden: MedlemAvFolketrygden): FastsattTrygdeavgift =
        medlemAvFolketrygden.fastsattTrygdeavgift ?: FastsattTrygdeavgift().apply {
            this.trygdeavgiftstype = Trygdeavgift_typer.FORELØPIG
            this.medlemAvFolketrygden = medlemAvFolketrygden
        }

    private fun eksisterendeEllerNyttTrygdeavgiftsgrunnlag(medlemAvFolketrygden: MedlemAvFolketrygden): Trygdeavgiftsgrunnlag =
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag ?: Trygdeavgiftsgrunnlag()


    private fun lagSkatteforholdTilNorge(
        request: OppdaterTrygdeavgiftsgrunnlagRequest
    ): List<SkatteforholdTilNorge> =
        (request.skatteforholdTilNorgeList.map { skatteforholdTilNorgeRequest: SkatteforholdTilNorgeRequest ->
            SkatteforholdTilNorge().apply {
                this.fomDato = skatteforholdTilNorgeRequest.fomDato
                this.tomDato = skatteforholdTilNorgeRequest.tomDato
                this.skatteplikttype = skatteforholdTilNorgeRequest.skatteplikttype
            }
        })

    private fun lagInntektsperioder(
        request: OppdaterTrygdeavgiftsgrunnlagRequest,
    ): List<Inntektsperiode> =
        (request.inntektskilder.map { inntektskildeRequest: InntektskildeRequest ->
            Inntektsperiode().apply {
                this.fomDato = inntektskildeRequest.fomDato
                this.tomDato = inntektskildeRequest.tomDato
                this.type = inntektskildeRequest.type
                this.isArbeidsgiversavgiftBetalesTilSkatt = inntektskildeRequest.arbeidsgiversavgiftBetales
                this.avgiftspliktigInntektMnd =
                    if (inntektskildeRequest.avgiftspliktigInntektMnd == null) null
                    else Penger(inntektskildeRequest.avgiftspliktigInntektMnd)
                this.isOrdinærTrygdeavgiftBetalesTilSkatt =
                    !ordinærTrygdeavgiftBetalesTilNav(request, inntektskildeRequest) //TODO fjern når REFAKTORERING_ORDINÆR_TRYGDEAVGIFT toggle er fjernet
            }
        })

    private fun ordinærTrygdeavgiftBetalesTilNav(
        request: OppdaterTrygdeavgiftsgrunnlagRequest, inntektskildeRequest: InntektskildeRequest
    ): Boolean {
        return request.skatteforholdTilNorgeList.any { it.skatteplikttype == Skatteplikttype.IKKE_SKATTEPLIKTIG } || inntektskildeRequest.type ==
            Inntektskildetype.FN_SKATTEFRITAK
    }

    @Transactional(readOnly = true)
    fun hentTrygdeavgiftsgrunnlag(behandlingID: Long): Trygdeavgiftsgrunnlag? {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        val behandling = behandlingsresultat.behandling
        val trygdeavgiftsgrunnlag = behandlingsresultat.medlemAvFolketrygden?.fastsattTrygdeavgift?.trygdeavgiftsgrunnlag
        if (trygdeavgiftsgrunnlag == null && behandling.erNyVurdering() && behandling.opprinneligBehandling != null) {
            val forrigeBehandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.opprinneligBehandling.id)
            return forrigeBehandlingsresultat.medlemAvFolketrygden?.fastsattTrygdeavgift?.trygdeavgiftsgrunnlag
        } else {
            return trygdeavgiftsgrunnlag
        }
    }

    companion object {
        fun validerAtInntekstperioderDekkerInnvilgedeMedlemskapsperioder(
            inntektsperioder: List<InntektskildeRequest>,
            innvilgedeMedlemskapsperioder: List<Medlemskapsperiode>
        ) {
            val inntektsperiodeDateRange = inntektsperioder.sortedBy { it.fomDato }
                .map { inntektsperiode -> LocalDateRange.ofClosed(inntektsperiode.fomDato, inntektsperiode.tomDato) }

            var samletInntektsperiodeDateRange: LocalDateRange? = null
            try {
                for (range in inntektsperiodeDateRange) {
                    samletInntektsperiodeDateRange = if (samletInntektsperiodeDateRange == null) {
                        range
                    } else {
                        samletInntektsperiodeDateRange.union(range)
                    }
                }
            } catch (ex: DateTimeException) {
                throw FunksjonellException("Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
            }

            val sortertMedlemskapsperiode = innvilgedeMedlemskapsperioder.sortedBy { it.fom }
            if (LocalDateRange.ofClosed(
                    sortertMedlemskapsperiode.first().fom,
                    sortertMedlemskapsperiode.last().tom
                ) != samletInntektsperiodeDateRange
            ) {
                throw FunksjonellException("Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
            }
        }


        fun validerAtSkatteforholdTilNorgeDekkerInnvilgedeMedlemskapsperioderOgOverlapperIkke(
            skatteforholdTilNorge: List<SkatteforholdTilNorgeRequest>,
            innvilgedeMedlemskapsperioder: List<Medlemskapsperiode>
        ) {
            val skatteforholdDateRange = skatteforholdTilNorge.sortedBy { it.fomDato }
                .map { skatteforhold -> LocalDateRange.ofClosed(skatteforhold.fomDato, skatteforhold.tomDato) }

            validerAtDetIkkeFinnesOverlapp(skatteforholdDateRange)

            var samletSkatteforholdDateRange: LocalDateRange? = null
            try {
                for (range in skatteforholdDateRange) {
                    samletSkatteforholdDateRange = if (samletSkatteforholdDateRange == null) {
                        range
                    } else {
                        samletSkatteforholdDateRange.union(range)
                    }
                }
            } catch (ex: DateTimeException) {
                throw FunksjonellException("Skatteforholdsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
            }

            val sortertMedlemskapsperiode = innvilgedeMedlemskapsperioder.sortedBy { it.fom }
            if (LocalDateRange.ofClosed(
                    sortertMedlemskapsperiode.first().fom,
                    sortertMedlemskapsperiode.last().tom
                ) != samletSkatteforholdDateRange
            ) {
                throw FunksjonellException("Skatteforholdsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
            }
        }

        private fun validerAtDetIkkeFinnesOverlapp(dateRanges: List<LocalDateRange>) {
            for (i in dateRanges.indices) {
                val range1 = dateRanges[i]

                for (j in dateRanges.indices) {
                    if (i != j) {
                        val range2 = dateRanges[j]

                        if (range1.overlaps(range2)) {
                            throw FunksjonellException("Skatteforholdsperiodene kan ikke overlappe")
                        }
                    }
                }
            }
        }
    }
}
