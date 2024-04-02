package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.service.avgift.dto.InntektskildeRequest
import no.nav.melosys.service.avgift.dto.OppdaterTrygdeavgiftsgrunnlagRequest
import no.nav.melosys.service.avgift.dto.SkatteforholdTilNorgeRequest
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.threeten.extra.LocalDateRange
import java.time.DateTimeException
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker.*

@Service
class TrygdeavgiftsgrunnlagService(private val behandlingsresultatService: BehandlingsresultatService, private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService) {

    @Transactional
    fun oppdaterTrygdeavgiftsgrunnlag(
        behandlingID: Long, request: OppdaterTrygdeavgiftsgrunnlagRequest
    ): Trygdeavgiftsgrunnlag {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        val medlemAvFolketrygden = behandlingsresultat.medlemAvFolketrygden
        val erFTRL_KAP_2_2_1 = medlemAvFolketrygden.erBestemmelse(Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1)

        validerMedlemskapsperioder(medlemAvFolketrygden, erFTRL_KAP_2_2_1)
        if(erFTRL_KAP_2_2_1) {
            validerÅpenSluttdato(behandlingsresultat, request)
        } else {
            validerTrygdeavgiftsgrunnlag(request, medlemAvFolketrygden.medlemskapsperioder)
        }

        fjernTrygdeavgiftsperioderOmDeFinnes(medlemAvFolketrygden.fastsattTrygdeavgift)

        return lagreTrygdeavgiftsgrunnlag(lagTrygdeavgiftsgrunnlag(behandlingsresultat, request)).medlemAvFolketrygden.fastsattTrygdeavgift?.trygdeavgiftsgrunnlag
            ?: throw TekniskException("Noe skjedde ved lagring av trygdeavgiftsgrunnlaget")
    }

    private fun validerÅpenSluttdato(behandlingsresultat: Behandlingsresultat, request: OppdaterTrygdeavgiftsgrunnlagRequest) {
        val midlertidigBehandlingsresultat = lagTrygdeavgiftsgrunnlag(behandlingsresultat, request)
        val trygdeavgiftsGrunnlag = midlertidigBehandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag
        val medlemAvFolketrygden = behandlingsresultat.medlemAvFolketrygden
        val skatteforholdsperiodeHarÅpenSluttdato =
            request.skatteforholdTilNorgeList.all { it.tomDato == null } || request.skatteforholdTilNorgeList.isEmpty()
        val inntektPeriodeHarÅpenSluttdato = request.inntektskilder.all { it.tomDato == null } || request.inntektskilder.isEmpty()
        val medlemskapsperiodeHarÅpenSluttdato = medlemAvFolketrygden.utledMedlemskapsperiodeTom() == null
        val erSkattepliktigIHelePerioden = request.skatteforholdTilNorgeList.all { it.skatteplikttype.equals(Skatteplikttype.SKATTEPLIKTIG) }

        if (erSkattepliktigIHelePerioden && !skatteforholdsperiodeHarÅpenSluttdato && medlemskapsperiodeHarÅpenSluttdato) {
            throw FunksjonellException("Skatteforholdsperiode må ha åpen sluttdato når medlemskapsperiode har åpen sluttdato")
        }

        if (erSkattepliktigIHelePerioden && !inntektPeriodeHarÅpenSluttdato && medlemskapsperiodeHarÅpenSluttdato) {
            throw FunksjonellException("Skatteforholdsperiode må ha åpen sluttdato når medlemskapsperiode har åpen sluttdato")
        }

        if (!erSkattepliktigIHelePerioden && !(skatteforholdsperiodeHarÅpenSluttdato && inntektPeriodeHarÅpenSluttdato) && medlemskapsperiodeHarÅpenSluttdato) {
            throw FunksjonellException("Skatteforholdsperiode må ha åpen sluttdato når medlemskapsperiode har åpen sluttdato")
        }

        if ((skatteforholdsperiodeHarÅpenSluttdato || inntektPeriodeHarÅpenSluttdato)) {
            validerFakturaMottaker(trygdeavgiftsGrunnlag)
        }
    }

    private fun validerFakturaMottaker(trygdeavgiftsGrunnlag: Trygdeavgiftsgrunnlag) {
        val trygdeavgiftMottaker = trygdeavgiftMottakerService.getTrygdeavgiftMottaker(trygdeavgiftsGrunnlag)
        if(trygdeavgiftMottaker == TRYGDEAVGIFT_BETALES_TIL_NAV || trygdeavgiftMottaker == TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT){
            throw FunksjonellException("Faktura kan ikke opprettes for medlemskapsperiode med åpen sluttdato. Angi sluttdato på medlemskapsperiode")
        }
    }

    private fun lagTrygdeavgiftsgrunnlag(
        behandlingsresultat: Behandlingsresultat,
        request: OppdaterTrygdeavgiftsgrunnlagRequest
    ): Behandlingsresultat {
        val medlemAvFolketrygden = behandlingsresultat.medlemAvFolketrygden

        medlemAvFolketrygden.fastsattTrygdeavgift = eksisterendeEllerNyFastsattTrygdeavgift(medlemAvFolketrygden)
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag =
            eksisterendeEllerNyttTrygdeavgiftsgrunnlag(medlemAvFolketrygden).apply {
                this.skatteforholdTilNorge = lagSkatteforholdTilNorge(request)
                this.inntektsperioder = lagInntektsperioder(request)
            }
        return behandlingsresultat
    }

    private fun lagreTrygdeavgiftsgrunnlag(
        behandlingsresultat: Behandlingsresultat,
    ): Behandlingsresultat {
        return behandlingsresultatService.lagre(behandlingsresultat)
    }

    fun fjernTrygdeavgiftsperioderOmDeFinnes(fastsattTrygdeavgift: FastsattTrygdeavgift?) {
        fastsattTrygdeavgift?.trygdeavgiftsperioder?.clear()
    }

    private fun validerMedlemskapsperioder(medlemAvFolketrygden: MedlemAvFolketrygden, sluttdatoKanVæreÅpen: Boolean) {
        if (medlemAvFolketrygden.medlemskapsperioder.isEmpty()) {
            throw FunksjonellException("Kan ikke oppdatere trygdeavgiftsgrunnlaget før medlemskapsperioder finnes")
        }
        medlemAvFolketrygden.utledMedlemskapsperiodeFom()
            ?: throw FunksjonellException("Klarte ikke finne startdatoen på medlemskapet")
        if(!sluttdatoKanVæreÅpen) {
            medlemAvFolketrygden.utledMedlemskapsperiodeTom()
                ?: throw FunksjonellException("Klarte ikke finne sluttdatoen på medlemskapet")
        }
    }

    private fun validerTrygdeavgiftsgrunnlag(request: OppdaterTrygdeavgiftsgrunnlagRequest, medlemskapsperioder: Collection<Medlemskapsperiode>) {
        val innvilgedeMedlemskapsperioder = medlemskapsperioder.filter { it.erInnvilget() }

        validerAtSkatteforholdTilNorgeDekkerInnvilgedeMedlemskapsperioderOgOverlapperIkke(
            request.skatteforholdTilNorgeList,
            innvilgedeMedlemskapsperioder
        )

        val erPliktigMedlem = innvilgedeMedlemskapsperioder.all { it.medlemskapstype.equals(Medlemskapstyper.PLIKTIG) }
        val erSkattepliktigIHelePerioden = request.skatteforholdTilNorgeList.all { it.skatteplikttype.equals(Skatteplikttype.SKATTEPLIKTIG) }

        if (!(erPliktigMedlem && erSkattepliktigIHelePerioden)) {
            validerAtInntekstperioderDekkerInnvilgedeMedlemskapsperioder(
                request.inntektskilder,
                innvilgedeMedlemskapsperioder
            )
        }
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
        return behandlingsresultatService.hentBehandlingsresultat(behandlingID).medlemAvFolketrygden?.fastsattTrygdeavgift?.trygdeavgiftsgrunnlag
    }

    @Transactional(readOnly = false)
    fun hentTrygdeavgiftsgrunnlagEllerOpprinneligTrygdeavgiftsgrunnlag(behandlingID: Long): Trygdeavgiftsgrunnlag? {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        val behandling = behandlingsresultat.behandling
        val trygdeavgiftsgrunnlag = behandlingsresultat.medlemAvFolketrygden?.fastsattTrygdeavgift?.trygdeavgiftsgrunnlag

        if (trygdeavgiftsgrunnlag == null && (behandling.erAndregangsbehandling()) && behandling.opprinneligBehandling != null) {
            return hentOgLagreOpprinneligBehandlingTrygdeavgiftsgrunnlag(behandling, behandlingsresultat)
        }

        return trygdeavgiftsgrunnlag
    }

    private fun hentOgLagreOpprinneligBehandlingTrygdeavgiftsgrunnlag(
        behandling: Behandling,
        behandlingsresultat: Behandlingsresultat
    ): Trygdeavgiftsgrunnlag? {
        val opprinneligTrygdeavgiftsgrunnlag = hentTrygdeavgiftsgrunnlag(behandling.opprinneligBehandling.id)

        if (opprinneligTrygdeavgiftsgrunnlag !== null && behandlingsresultat.medlemAvFolketrygden != null) {
            val request = OppdaterTrygdeavgiftsgrunnlagRequest(
                opprinneligTrygdeavgiftsgrunnlag.skatteforholdTilNorge.map { SkatteforholdTilNorgeRequest(it) },
                opprinneligTrygdeavgiftsgrunnlag.inntektsperioder.map { InntektskildeRequest(it) })

            val midlertidigBehandlingsresultat = lagTrygdeavgiftsgrunnlag(behandlingsresultat, request)
            return lagreTrygdeavgiftsgrunnlag(midlertidigBehandlingsresultat).medlemAvFolketrygden.fastsattTrygdeavgift?.trygdeavgiftsgrunnlag
                ?: throw TekniskException("Klarte ikke lagre opprinnelig behandlings trygdeavgiftsgrunnlag")
        }

        return opprinneligTrygdeavgiftsgrunnlag
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
