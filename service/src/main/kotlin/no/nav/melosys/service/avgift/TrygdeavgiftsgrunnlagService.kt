package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
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
    fun oppdaterTrygdeavgiftsgrunnlag(
        behandlingsresultatID: Long, request: OppdaterTrygdeavgiftsgrunnlagRequest
    ): Trygdeavgiftsgrunnlag {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID)
        val medlemAvFolketrygden = behandlingsresultat.medlemAvFolketrygden
        val medlemskapsperioder = medlemAvFolketrygden.medlemskapsperioder

        if (medlemskapsperioder.isEmpty()) {
            throw FunksjonellException("Kan ikke oppdatere trygdeavgiftsgrunnlaget før medlemskapsperioder finnes")
        }

        val fomDato = utledFomDato(medlemskapsperioder)
        val tomDato = utledTomDato(medlemskapsperioder)

        medlemAvFolketrygden.fastsattTrygdeavgift = eksisterendeEllerNyFastsattTrygdeavgift(medlemAvFolketrygden)
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag =
            eksisterendeEllerNyttTrygdeavgiftsgrunnlag(medlemAvFolketrygden).apply {
                this.skatteforholdTilNorge = lagSkatteforholdTilNorge(request, fomDato, tomDato)
                this.inntektsperioder = lagInntektsperioder(request, fomDato, tomDato)
            }
        behandlingsresultatService.lagre(behandlingsresultat)

        return hentTrygdeavgiftsgrunnlag(behandlingsresultatID)
            ?: throw FunksjonellException("Noe skjedde ved lagring av trygdeavgiftsgrunnlaget")
    }

    private fun utledFomDato(medlemskapsperioder: Collection<Medlemskapsperiode>): LocalDate =
        medlemskapsperioder.minByOrNull { it.fom }?.fom
            ?: throw FunksjonellException("Klarte ikke finne startdatoen på medlemskapet")

    private fun utledTomDato(medlemskapsperioder: Collection<Medlemskapsperiode>): LocalDate =
        medlemskapsperioder.maxByOrNull { it.tom }?.tom
            ?: throw FunksjonellException("Klarte ikke finne sluttdatoen på medlemskapet")

    private fun eksisterendeEllerNyFastsattTrygdeavgift(medlemAvFolketrygden: MedlemAvFolketrygden): FastsattTrygdeavgift =
        medlemAvFolketrygden.fastsattTrygdeavgift ?: FastsattTrygdeavgift().apply {
            this.trygdeavgiftstype = Trygdeavgift_typer.FORELØPIG
        }

    private fun eksisterendeEllerNyttTrygdeavgiftsgrunnlag(medlemAvFolketrygden: MedlemAvFolketrygden): Trygdeavgiftsgrunnlag =
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag ?: Trygdeavgiftsgrunnlag()


    private fun lagSkatteforholdTilNorge(
        request: OppdaterTrygdeavgiftsgrunnlagRequest,
        fomDato: LocalDate,
        tomDato: LocalDate,
    ): Set<SkatteforholdTilNorge> = setOf(SkatteforholdTilNorge().apply {
        this.fomDato = fomDato
        this.tomDato = tomDato
        this.skatteplikttype = request.skatteplikttype
    })

    private fun lagInntektsperioder(
        request: OppdaterTrygdeavgiftsgrunnlagRequest,
        fomDato: LocalDate,
        tomDato: LocalDate,
    ): Set<Inntektsperiode> =
        (request.inntektskilder.map { inntektskildeRequest: InntektskildeRequest ->
            Inntektsperiode().apply {
                this.fomDato = fomDato
                this.tomDato = tomDato
                this.type = inntektskildeRequest.type
                this.avgiftspliktigInntektMnd = inntektskildeRequest.avgiftspliktigInntektMnd
                this.isArbeidsgiversavgiftBetalesTilSkatt = inntektskildeRequest.arbeidsgiversavgiftBetales
                this.isTrygdeavgiftBetalesTilSkatt = this.utledTrygdeavgiftBetalesTilSkatt(request.skatteplikttype)
            }
        }).toSet()

    @Transactional(readOnly = true)
    fun hentTrygdeavgiftsgrunnlag(behandlingsresultatID: Long): Trygdeavgiftsgrunnlag? {
        return behandlingsresultatService.hentBehandlingsresultat(behandlingsresultatID).getMedlemAvFolketrygden()
            ?.getFastsattTrygdeavgift()?.getTrygdeavgiftsgrunnlag()
    }
}
