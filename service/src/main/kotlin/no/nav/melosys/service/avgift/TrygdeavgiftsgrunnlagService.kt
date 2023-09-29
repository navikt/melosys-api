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

@Service
class TrygdeavgiftsgrunnlagService(private val behandlingsresultatService: BehandlingsresultatService) {

    @Transactional
    fun oppdaterTrygdeavgiftsgrunnlag(
        behandlingID: Long, request: OppdaterTrygdeavgiftsgrunnlagRequest
    ): Trygdeavgiftsgrunnlag {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        val medlemAvFolketrygden = behandlingsresultat.medlemAvFolketrygden

        validerAtMedlemskapsperioderFinnes(medlemAvFolketrygden.medlemskapsperioder)
        fjernTrygdeavgiftsperioderOmDeFinnes(medlemAvFolketrygden.fastsattTrygdeavgift)

        medlemAvFolketrygden.utledMedlemskapsperiodeFom()
            ?: throw FunksjonellException("Klarte ikke finne startdatoen på medlemskapet")
        medlemAvFolketrygden.utledMedlemskapsperiodeTom()
            ?: throw FunksjonellException("Klarte ikke finne sluttdatoen på medlemskapet")

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

    private fun fjernTrygdeavgiftsperioderOmDeFinnes(fastsattTrygdeavgift: FastsattTrygdeavgift?) {
        fastsattTrygdeavgift?.trygdeavgiftsperioder?.clear()
    }

    private fun validerAtMedlemskapsperioderFinnes(medlemskapsperioder: Collection<Medlemskapsperiode>) {
        if (medlemskapsperioder.isEmpty()) {
            throw FunksjonellException("Kan ikke oppdatere trygdeavgiftsgrunnlaget før medlemskapsperioder finnes")
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
                this.avgiftspliktigInntektMnd =
                    if (inntektskildeRequest.avgiftspliktigInntektMnd == null) null
                    else Penger(inntektskildeRequest.avgiftspliktigInntektMnd)
                this.isArbeidsgiversavgiftBetalesTilSkatt = inntektskildeRequest.arbeidsgiversavgiftBetales
                this.isOrdinærTrygdeavgiftBetalesTilSkatt =
                    !ordinærTrygdeavgiftBetalesTilNav(request, inntektskildeRequest)
            }
        })

    private fun ordinærTrygdeavgiftBetalesTilNav(
        request: OppdaterTrygdeavgiftsgrunnlagRequest, inntektskildeRequest: InntektskildeRequest
    ): Boolean {
        return request.skatteforholdTilNorgeList.maxBy { it.tomDato }.skatteplikttype == Skatteplikttype.IKKE_SKATTEPLIKTIG || inntektskildeRequest.type ==
            Inntektskildetype
            .FN_SKATTEFRITAK
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
}
