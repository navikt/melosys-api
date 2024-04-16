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
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer
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

@Service
class TrygdeavgiftsgrunnlagService(private val behandlingsresultatService: BehandlingsresultatService) {

    @Transactional
    fun oppdaterTrygdeavgiftsgrunnlag(
        behandlingID: Long, request: OppdaterTrygdeavgiftsgrunnlagRequest
    ): Trygdeavgiftsgrunnlag {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        val medlemAvFolketrygden = behandlingsresultat.medlemAvFolketrygden

        validerMedlemskapsperioder(medlemAvFolketrygden)
        validerTrygdeavgiftsgrunnlag(request, medlemAvFolketrygden)
        fjernTrygdeavgiftsperioderOmDeFinnes(medlemAvFolketrygden.fastsattTrygdeavgift)
        behandlingsresultatService.lagreOgFlush(behandlingsresultat)

        return lagreTrygdeavgiftsgrunnlag(behandlingsresultat, request).medlemAvFolketrygden.fastsattTrygdeavgift?.trygdeavgiftsgrunnlag
            ?: throw TekniskException("Noe skjedde ved lagring av trygdeavgiftsgrunnlaget")
    }

    private fun lagreTrygdeavgiftsgrunnlag(
        behandlingsresultat: Behandlingsresultat,
        request: OppdaterTrygdeavgiftsgrunnlagRequest
    ): Behandlingsresultat {
        val medlemAvFolketrygden = behandlingsresultat.medlemAvFolketrygden
        medlemAvFolketrygden.fastsattTrygdeavgift = eksisterendeEllerNyFastsattTrygdeavgift(medlemAvFolketrygden)
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag = eksisterendeEllerNyttTrygdeavgiftsgrunnlag(medlemAvFolketrygden)
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag.skatteforholdTilNorge.clear()
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag.inntektsperioder.clear()

        val trygdeavgiftsgrunnlag = medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag
        trygdeavgiftsgrunnlag.skatteforholdTilNorge.addAll(lagSkatteforholdTilNorge(request, trygdeavgiftsgrunnlag))
        trygdeavgiftsgrunnlag.inntektsperioder.addAll(lagInntektsperioder(request, trygdeavgiftsgrunnlag))

        return behandlingsresultatService.lagreOgFlush(behandlingsresultat)
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
    }

    private fun validerTrygdeavgiftsgrunnlag(request: OppdaterTrygdeavgiftsgrunnlagRequest, medlemAvFolketrygden: MedlemAvFolketrygden) {
        val medlemskapsperioderErÅpen = medlemAvFolketrygden.utledMedlemskapsperiodeTom() == null
        val erSkattepliktigIHelePerioden = request.skatteforholdTilNorgeList.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }

        if (medlemskapsperioderErÅpen) {
            val skatteforholdsperiodeErÅpen = request.skatteforholdTilNorgeList.sortedBy { it.fomDato }.last().tomDato == null

            if (erSkattepliktigIHelePerioden && !skatteforholdsperiodeErÅpen) {
                throw FunksjonellException("Skatteforholdsperiode/inntektsperiode kan ikke ha sluttdato når medlemskapsperiode ikke har sluttdato")
            }

            if (!erSkattepliktigIHelePerioden) {
                throw FunksjonellException("Faktura kan ikke opprettes for medlemskapsperiode uten sluttdato. Angi sluttdato på medlemskapsperiode")
            }

            if (erSkattepliktigIHelePerioden && skatteforholdsperiodeErÅpen && request.skatteforholdTilNorgeList.size == 1) {
                return
            }
        }

        val innvilgedeMedlemskapsperioder = medlemAvFolketrygden.medlemskapsperioder.filter { it.erInnvilget() }

        validerAtSkatteforholdTilNorgeDekkerInnvilgedeMedlemskapsperioderOgOverlapperIkke(
            request.skatteforholdTilNorgeList,
            innvilgedeMedlemskapsperioder
        )

        val erPliktigMedlem = innvilgedeMedlemskapsperioder.all { it.erPliktig() }

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
        request: OppdaterTrygdeavgiftsgrunnlagRequest,
        trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag
    ): List<SkatteforholdTilNorge> =
        (request.skatteforholdTilNorgeList.map { skatteforholdTilNorgeRequest: SkatteforholdTilNorgeRequest ->
            SkatteforholdTilNorge().apply {
                this.fomDato = skatteforholdTilNorgeRequest.fomDato
                this.tomDato = skatteforholdTilNorgeRequest.tomDato
                this.skatteplikttype = skatteforholdTilNorgeRequest.skatteplikttype
                this.trygdeavgiftsgrunnlag = trygdeavgiftsgrunnlag
            }
        })

    private fun lagInntektsperioder(
        request: OppdaterTrygdeavgiftsgrunnlagRequest,
        trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag,
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
                this.trygdeavgiftsgrunnlag = trygdeavgiftsgrunnlag
            }
        })

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
            return lagreTrygdeavgiftsgrunnlag(behandlingsresultat, request).medlemAvFolketrygden.fastsattTrygdeavgift?.trygdeavgiftsgrunnlag
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
