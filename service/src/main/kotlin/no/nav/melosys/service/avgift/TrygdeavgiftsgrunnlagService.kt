package no.nav.melosys.service.avgift

import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgift_typer
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.avgift.dto.InntektskildeRequest
import no.nav.melosys.service.avgift.dto.OppdaterTrygdeavgiftsgrunnlagRequest
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class TrygdeavgiftsgrunnlagService(private val behandlingsresultatService: BehandlingsresultatService) {

    @Transactional
    fun oppdaterTrygdeavgiftsgrunnlaget(
        behandlingsresultatID: Long, request: OppdaterTrygdeavgiftsgrunnlagRequest
    ): Trygdeavgiftsgrunnlag {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
        val medlemAvFolketrygden = behandlingsresultat.medlemAvFolketrygden
        val medlemskapsperioder = medlemAvFolketrygden.medlemskapsperioder

        if (medlemskapsperioder.isEmpty()) {
            throw FunksjonellException("Kan ikke oppdatere trygdeavgiftsgrunnlaget før medlemskapsperioder finnes")
        }

        val fastsattTrygdeavgift = medlemAvFolketrygden.fastsattTrygdeavgift ?: FastsattTrygdeavgift().apply {
            this.trygdeavgiftstype = Trygdeavgift_typer.FORELØPIG
        }
        val trygdeavgiftsgrunnlag = fastsattTrygdeavgift.trygdeavgiftsgrunnlag ?: Trygdeavgiftsgrunnlag()

        val fomDato = medlemskapsperioder.minByOrNull { it.fom }?.fom
            ?: throw FunksjonellException("Klarte ikke finne startdatoen på medlemskapet")
        val tomDato = medlemskapsperioder.maxByOrNull { it.tom }?.tom
            ?: throw FunksjonellException("Klarte ikke finne sluttdatoen på medlemskapet")

        fastsattTrygdeavgift.trygdeavgiftsgrunnlag = trygdeavgiftsgrunnlag.apply {
            this.skatteforholdTilNorge.clear()
            this.skatteforholdTilNorge.add(lagSkatteforholdTilNorge(request, fomDato, tomDato, trygdeavgiftsgrunnlag))
            this.inntektsperioder.clear()
            this.inntektsperioder.addAll(lagInntektsperioder(request, fomDato, tomDato, trygdeavgiftsgrunnlag))
        }
        medlemAvFolketrygden.fastsattTrygdeavgift = fastsattTrygdeavgift
        behandlingsresultatService.lagre(behandlingsresultat)

        return hentTrygdeavgiftsgrunnlaget(behandlingsresultatID)
            ?: throw FunksjonellException("Noe skjedde ved lagring av trygdeavgiftsgrunnlaget")
    }

    private fun lagSkatteforholdTilNorge(
        request: OppdaterTrygdeavgiftsgrunnlagRequest,
        fomDato: LocalDate,
        tomDato: LocalDate,
        trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag
    ): SkatteforholdTilNorge = SkatteforholdTilNorge().apply {
        this.fomDato = fomDato
        this.tomDato = tomDato
        this.skatteplikttype = request.skatteplikttype
        this.trygdeavgiftsgrunnlag = trygdeavgiftsgrunnlag
    }

    private fun lagInntektsperioder(
        request: OppdaterTrygdeavgiftsgrunnlagRequest,
        fomDato: LocalDate,
        tomDato: LocalDate,
        trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag,
    ): Set<Inntektsperiode> =
        (request.inntektskilder.map { inntektskildeRequest: InntektskildeRequest ->
            Inntektsperiode().apply {
                this.fomDato = fomDato
                this.tomDato = tomDato
                this.type = inntektskildeRequest.type
                this.avgiftspliktigInntektMnd = inntektskildeRequest.avgiftspliktigInntektMnd
                this.isArbeidsgiversavgiftBetalesTilSkatt = inntektskildeRequest.arbeidsgiversavgiftBetales
                this.isTrygdeavgiftBetalesTilSkatt = trygdeavgiftBetalesTilSkatt(request.skatteplikttype, this)
                this.trygdeavgiftsgrunnlag = trygdeavgiftsgrunnlag
            }
        }).toSet()

    private fun trygdeavgiftBetalesTilSkatt(
        skatteplikttype: Skatteplikttype,
        inntektsperiode: Inntektsperiode
    ): Boolean {
        return skatteplikttype == Skatteplikttype.IKKE_SKATTEPLIKTIG || listOf(
            Inntektskildetype.NÆRINGSINNTEKT_FRA_NORGE, Inntektskildetype.FN_SKATTEFRITAK
        ).contains(inntektsperiode.type) || inntektsperiode.type == Inntektskildetype.INNTEKT_FRA_UTLANDET && inntektsperiode.isArbeidsgiversavgiftBetalesTilSkatt
    }

    @Transactional(readOnly = true)
    fun hentTrygdeavgiftsgrunnlaget(behandlingsresultatID: Long): Trygdeavgiftsgrunnlag? {
        return behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID).getMedlemAvFolketrygden()
            ?.getFastsattTrygdeavgift()?.getTrygdeavgiftsgrunnlag()
    }
}
