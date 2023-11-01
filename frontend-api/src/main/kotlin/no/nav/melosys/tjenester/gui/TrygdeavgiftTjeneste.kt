package no.nav.melosys.tjenester.gui

import io.getunleash.Unleash
import io.swagger.annotations.Api
import no.nav.melosys.featuretoggle.ToggleName.REFAKTORERING_ORDINÆR_TRYGDEAVGIFT
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.BeregnetTrygdeavgiftDto
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.TrygdeavgiftMottakerDto
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.TrygdeavgiftsgrunnlagDto
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Protected
@RestController
@Api(tags = ["trygdeavgift"])
@RequestMapping("/behandlinger/{behandlingID}/trygdeavgift")
class TrygdeavgiftTjeneste(
    private val trygdeavgiftsgrunnlagService: TrygdeavgiftsgrunnlagService,
    private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val aksesskontroll: Aksesskontroll,
    private val unleash: Unleash
) {

    @GetMapping("/grunnlag")
    fun hentTrygdeavgiftsgrunnlag(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<TrygdeavgiftsgrunnlagDto> {
        aksesskontroll.autoriser(behandlingID)

        return trygdeavgiftsgrunnlagService.hentTrygdeavgiftsgrunnlag(behandlingID)
            ?.let { ResponseEntity.ok(TrygdeavgiftsgrunnlagDto(it)) } ?: ResponseEntity.noContent().build()
    }

    @GetMapping("/mottaker")
    fun hentTrygdeavgiftMottaker(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<TrygdeavgiftMottakerDto> {
        aksesskontroll.autoriser(behandlingID)

        return trygdeavgiftsgrunnlagService.hentTrygdeavgiftsgrunnlag(behandlingID)
            ?.let { ResponseEntity.ok(TrygdeavgiftMottakerDto(
                if (unleash.isEnabled(REFAKTORERING_ORDINÆR_TRYGDEAVGIFT)) trygdeavgiftMottakerService.getTrygdeavgiftMottaker(it)
                else it.fastsattTrygdeavgift.trygdeavgiftMottaker))
            } ?: ResponseEntity.noContent().build()
    }

    @PutMapping("/grunnlag")
    fun oppdaterTrygdeavgiftsgrunnlag(
        @PathVariable("behandlingID") behandlingID: Long,
        @RequestBody trygdeavgiftsgrunnlagDto: TrygdeavgiftsgrunnlagDto
    ): ResponseEntity<TrygdeavgiftsgrunnlagDto> {
        aksesskontroll.autoriserSkrivOgTilordnet(behandlingID)

        return ResponseEntity.ok(
            TrygdeavgiftsgrunnlagDto(
                trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
                    behandlingID,
                    trygdeavgiftsgrunnlagDto.tilRequest()
                )
            )
        )
    }

    @GetMapping("/beregning")
    fun hentTrygdeavgift(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<BeregnetTrygdeavgiftDto> {
        aksesskontroll.autoriser(behandlingID)
        return ResponseEntity.ok(
            BeregnetTrygdeavgiftDto.av(trygdeavgiftsberegningService.hentTrygdeavgiftsberegning(behandlingID))
        )
    }

    @PutMapping("/beregning")
    fun beregnTrygdeavgift(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<BeregnetTrygdeavgiftDto> {
        aksesskontroll.autoriserSkrivOgTilordnet(behandlingID)
        return ResponseEntity.ok(
            BeregnetTrygdeavgiftDto.av(trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(behandlingID))
        )
    }

    @GetMapping("/fakturamottaker")
    fun hentFakturamottaker(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<String> {
        aksesskontroll.autoriser(behandlingID)
        return ResponseEntity.ok(trygdeavgiftsberegningService.finnFakturamottaker(behandlingID))
    }
}
