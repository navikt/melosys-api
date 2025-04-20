package no.nav.melosys.tjenester.gui.brev

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
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
@Tags(
    Tag(name = "brev"),
    Tag(name = "utkast"),
)
class UtkastBrevController(
    private val utkastBrevService: UtkastBrevService,
    private val aksesskontroll: Aksesskontroll
) {
    @GetMapping(value = ["/{behandlingID}"], produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Henter alle brevutkast for en behandling"
    )
    fun hentUtkast(@PathVariable behandlingID: Long): List<UtkastBrevResponse> {
        aksesskontroll.autoriser(behandlingID)

        return utkastBrevService.hentUtkast(behandlingID).map(UtkastBrevResponse::av)
    }

    @PostMapping(value = ["/{behandlingID}"])
    @Operation(summary = "Lagrer et brevutkast på en behandling")
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
    @Operation(summary = "Oppdaterer et eksisterende utkast")
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
    @Operation(summary = "Sletter et brevutkast")
    fun slettUtkast(@PathVariable behandlingID: Long, @PathVariable utkastBrevID: Long): ResponseEntity<Unit> {
        aksesskontroll.autoriser(behandlingID)

        utkastBrevService.slettUtkast(utkastBrevID)
        return ResponseEntity.noContent().build()
    }
}
