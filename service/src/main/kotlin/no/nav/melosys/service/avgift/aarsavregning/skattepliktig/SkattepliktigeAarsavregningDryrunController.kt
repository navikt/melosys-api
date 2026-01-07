package no.nav.melosys.service.avgift.aarsavregning.skattepliktig

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import mu.KotlinLogging
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger { }

@Protected
@RestController
@RequestMapping("/admin/aarsavregninger/saker/skattepliktige")
class SkattepliktigeAarsavregningDryrunController(
    private val skattepliktigeAarsavregningDryrunService: SkattepliktigeAarsavregningDryrunService
) {

    @Operation(
        summary = "Dryrun for skattehendelser",
        description = "Simulerer behandling av skattehendelser og teller opp antall saker som ville blitt berørt. " +
            "Ingen prosessinstanser opprettes. Bruk /status for å følge fremdrift og /rapport for detaljert resultat."
    )
    @PostMapping("/dryrun")
    fun dryrun(
        @RequestBody
        @Parameter(description = "Liste med skattehendelser som skal simuleres")
        request: SkattehendelseDryrunRequest
    ): ResponseEntity<Map<String, Any?>> {
        log.info { "Starter dryrun for ${request.skattehendelser.size} skattehendelser" }

        skattepliktigeAarsavregningDryrunService.prosesserSkattehendelserAsynkront(request.skattehendelser)

        return ResponseEntity.ok(
            mapOf(
                "melding" to "Dryrun startet",
                "antallHendelser" to request.skattehendelser.size,
                "statusEndpoint" to "/admin/aarsavregninger/saker/skattepliktige/status",
                "rapportEndpoint" to "/admin/aarsavregninger/saker/skattepliktige/rapport"
            )
        )
    }

    @Operation(summary = "Hent status for pågående eller siste dryrun")
    @GetMapping("/status")
    fun status(): ResponseEntity<Map<String, Any?>> =
        ResponseEntity(skattepliktigeAarsavregningDryrunService.status(), HttpStatus.OK)

    @Operation(summary = "Hent detaljert rapport med alle saker fra siste dryrun")
    @GetMapping("/rapport", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun rapport(): ResponseEntity<String> =
        ResponseEntity(skattepliktigeAarsavregningDryrunService.rapportJsonString(), HttpStatus.OK)
}

data class SkattehendelseDryrunRequest(
    val skattehendelser: List<SkattehendelseDryrunItem>
)
