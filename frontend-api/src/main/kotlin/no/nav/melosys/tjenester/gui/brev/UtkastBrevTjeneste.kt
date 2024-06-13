package no.nav.melosys.tjenester.gui.brev

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import no.nav.melosys.service.brev.UtkastBrevService
import no.nav.melosys.service.brev.bestilling.OppdaterUtkastService
import no.nav.melosys.service.tilgang.Aksesskontroll
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.tjenester.gui.dto.brev.BrevbestillingRequest
import no.nav.melosys.tjenester.gui.dto.brev.UtkastBrevResponse
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Protected
@RestController
@RequestMapping("/brev/utkast")
@Api(tags = ["brev", "utkast"])
class UtkastBrevTjeneste(
    private val utkastBrevService: UtkastBrevService,
    private val aksesskontroll: Aksesskontroll
) {
    @GetMapping(value = ["/{behandlingID}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ApiOperation(
        value = "Henter alle brevutkast for en behandling",
        response = UtkastBrevResponse::class,
        responseContainer = "List"
    )
    fun hentUtkast(@PathVariable behandlingID: Long): List<UtkastBrevResponse> {
        aksesskontroll.autoriser(behandlingID)

        return utkastBrevService.hentUtkast(behandlingID).map(UtkastBrevResponse::av)
    }

    @PostMapping(value = ["/{behandlingID}"])
    @ApiOperation(value = "Lagrer et brevutkast på en behandling")
    fun lagreUtkast(
        @PathVariable behandlingID: Long,
        @RequestBody brevbestillingRequest: BrevbestillingRequest
    ): ResponseEntity<Unit> {
        aksesskontroll.autoriser(behandlingID)

        val saksbehandlerID = SubjectHandler.getInstance().getUserID()
        utkastBrevService.lagreUtkast(behandlingID, saksbehandlerID, brevbestillingRequest.tilUtkast())
        return ResponseEntity.noContent().build()
    }

    @PutMapping(value = ["/{behandlingID}/{utkastBrevID}"])
    @ApiOperation(value = "Oppdaterer et eksisterende utkast")
    fun oppdaterUtkast(
        @PathVariable behandlingID: Long,
        @PathVariable utkastBrevID: Long,
        @RequestBody brevbestillingRequest: BrevbestillingRequest
    ): ResponseEntity<Unit> {
        aksesskontroll.autoriser(behandlingID)

        val saksbehandlerID = SubjectHandler.getInstance().getUserID()
        utkastBrevService.oppdaterUtkast(
            OppdaterUtkastService.RequestDto(
                utkastBrevID,
                behandlingID,
                saksbehandlerID,
                brevbestillingRequest.tilUtkast()
            )
        )
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping(value = ["/{behandlingID}/{utkastBrevID}"])
    @ApiOperation(value = "Sletter et brevutkast")
    fun slettUtkast(@PathVariable behandlingID: Long, @PathVariable utkastBrevID: Long): ResponseEntity<Unit> {
        aksesskontroll.autoriser(behandlingID)

        utkastBrevService.slettUtkast(utkastBrevID)
        return ResponseEntity.noContent().build()
    }
}
