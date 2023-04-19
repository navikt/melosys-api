package no.nav.melosys.service.avgift

import no.nav.melosys.domain.avgift.Inntektskilde
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlaget
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
    ): Trygdeavgiftsgrunnlaget {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
        val medlemAvFolketrygden = behandlingsresultat.medlemAvFolketrygden
        val medlemskapsperioder = medlemAvFolketrygden.medlemskapsperioder

        if (medlemskapsperioder.isEmpty()) {
            throw FunksjonellException("Kan ikke oppdatere trygdeavgiftsgrunnlaget før medlemskapsperioder finnes")
        }

        val fastsattTrygdeavgift = medlemAvFolketrygden.fastsattTrygdeavgift ?: FastsattTrygdeavgift().apply {
            this.trygdeavgiftstype = Trygdeavgift_typer.FORELØPIG
            this.medlemAvFolketrygden = medlemAvFolketrygden
        }

        val trygdeavgiftsgrunnlaget = fastsattTrygdeavgift.trygdeavgiftsgrunnlaget ?: Trygdeavgiftsgrunnlaget().apply {
            this.fastsattTrygdeavgift = fastsattTrygdeavgift
        }

        val fomDato = medlemskapsperioder.minByOrNull { it.fom }?.fom
            ?: throw FunksjonellException("Klarte ikke finne startdatoen på medlemskapet")
        val tomDato = medlemskapsperioder.maxByOrNull { it.tom }?.tom
            ?: throw FunksjonellException("Klarte ikke finne sluttdatoen på medlemskapet")

        fastsattTrygdeavgift.trygdeavgiftsgrunnlaget = trygdeavgiftsgrunnlaget.apply {
            this.skatteforholdTilNorge.clear()
            this.skatteforholdTilNorge.add(lagSkatteforholdTilNorge(request, fomDato, tomDato, trygdeavgiftsgrunnlaget))
            this.inntektskilder.clear()
            this.inntektskilder.addAll(lagInntektskilder(request, fomDato, tomDato, trygdeavgiftsgrunnlaget))
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
        trygdeavgiftsgrunnlaget: Trygdeavgiftsgrunnlaget
    ): SkatteforholdTilNorge = SkatteforholdTilNorge().apply {
        this.fomDato = fomDato
        this.tomDato = tomDato
        this.skatteplikttype = request.skatteplikttype
        this.trygdeavgiftsgrunnlaget = trygdeavgiftsgrunnlaget
    }

    private fun lagInntektskilder(
        request: OppdaterTrygdeavgiftsgrunnlagRequest,
        fomDato: LocalDate,
        tomDato: LocalDate,
        trygdeavgiftsgrunnlaget: Trygdeavgiftsgrunnlaget,
    ): Set<Inntektskilde> =
        (request.inntektskilder.map { inntektskildeRequest: InntektskildeRequest ->
            Inntektskilde().apply {
                this.fomDato = fomDato
                this.tomDato = tomDato
                this.inntektskildetype = inntektskildeRequest.type
                this.avgiftspliktigInntektMnd = inntektskildeRequest.avgiftspliktigInntektMnd
                this.isArbeidsgiversavgiftBetalesTilSkatt = inntektskildeRequest.arbeidsgiversavgiftBetales
                this.isTrygdeavgiftBetalesTilSkatt = trygdeavgiftBetalesTilSkatt(request.skatteplikttype, this)
                this.trygdeavgiftsgrunnlaget = trygdeavgiftsgrunnlaget
            }
        }).toSet()

    private fun trygdeavgiftBetalesTilSkatt(skatteplikttype: Skatteplikttype, inntektskilde: Inntektskilde): Boolean {
        return skatteplikttype == Skatteplikttype.IKKE_SKATTEPLIKTIG || listOf(
            Inntektskildetype.NÆRINGSINNTEKT_FRA_NORGE, Inntektskildetype.FN_SKATTEFRITAK
        ).contains(inntektskilde.inntektskildetype) || inntektskilde.inntektskildetype == Inntektskildetype.INNTEKT_FRA_UTLANDET && inntektskilde.isArbeidsgiversavgiftBetalesTilSkatt
    }

    @Transactional(readOnly = true)
    fun hentTrygdeavgiftsgrunnlaget(behandlingsresultatID: Long): Trygdeavgiftsgrunnlaget? {
        return behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID).getMedlemAvFolketrygden()
            ?.getFastsattTrygdeavgift()?.getTrygdeavgiftsgrunnlaget()
    }
}
