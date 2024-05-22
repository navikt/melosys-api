package no.nav.melosys.tjenester.gui

import io.swagger.annotations.Api
import no.nav.melosys.service.MedlemAvFolketrygdenService
import no.nav.melosys.service.avgift.TrygdeavgiftMottakerService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.BeregnetTrygdeavgiftDto
import no.nav.melosys.tjenester.gui.dto.trygdeavgift.FakturamottakerDto
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
    private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
    private val medlemAvFolketrygdenService: MedlemAvFolketrygdenService,
    private val trygdeavgiftMottakerService: TrygdeavgiftMottakerService,
    private val aksesskontroll: Aksesskontroll
) {

    @GetMapping("/mottaker")
    fun hentTrygdeavgiftMottaker(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<TrygdeavgiftMottakerDto> {
        aksesskontroll.autoriser(behandlingID)

        return medlemAvFolketrygdenService.hentMedlemAvFolketrygden(behandlingID).fastsattTrygdeavgift
            ?.let {
                ResponseEntity.ok(
                    TrygdeavgiftMottakerDto(
                        trygdeavgiftMottakerService.getTrygdeavgiftMottaker(it)
                    )
                )
            } ?: ResponseEntity.noContent().build()
    }

    @PutMapping("/beregning")
    fun beregnTrygdeavgiftsperioder(
        @PathVariable("behandlingID") behandlingID: Long,
        @RequestBody trygdeavgiftsgrunnlagDto: TrygdeavgiftsgrunnlagDto
    ): ResponseEntity<BeregnetTrygdeavgiftDto> {
        aksesskontroll.autoriserSkrivOgTilordnet(behandlingID)
        val trygdeavgiftsperiodeSet = trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
            behandlingID,
            trygdeavgiftsgrunnlagDto.tilRequest()
        )

        return ResponseEntity.ok(
            BeregnetTrygdeavgiftDto.av(trygdeavgiftsperiodeSet)
        )
    }

    @GetMapping("/beregning")
    fun hentBeregnetTrygdeavgift(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<BeregnetTrygdeavgiftDto> {
        aksesskontroll.autoriser(behandlingID)

        val trygdeavgiftsperiodeSet = trygdeavgiftsberegningService.hentTrygdeavgiftsberegning(behandlingID)

        return ResponseEntity.ok(
            BeregnetTrygdeavgiftDto.av(trygdeavgiftsperiodeSet)
        )
    }

    @GetMapping("/fakturamottaker")
    fun hentFakturamottaker(@PathVariable("behandlingID") behandlingID: Long): ResponseEntity<FakturamottakerDto> {
        aksesskontroll.autoriser(behandlingID)
        return ResponseEntity.ok(FakturamottakerDto(trygdeavgiftsberegningService.finnFakturamottakerNavn(behandlingID)))
    }
}
