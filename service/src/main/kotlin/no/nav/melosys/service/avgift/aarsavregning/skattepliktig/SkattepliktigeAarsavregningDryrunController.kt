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
        summary = "Kjøring av skattehendelser (dryrun eller skarpt)",
        description = "Prosesserer skattehendelser for skattepliktige. " +
            "Med skarp=false (default) er det en simulering — ingen prosessinstanser opprettes. " +
            "Med skarp=true opprettes faktiske AARSAVREGNING-prosessinstanser, kappet av maksAntall. " +
            "Bruk /status for fremdrift og /rapport for detaljert resultat."
    )
    @PostMapping("/run")
    fun run(
        @RequestBody
        @Parameter(description = "Liste med skattehendelser, skarp-flagg, og valgfritt maksAntall")
        request: SkattehendelseRunRequest
    ): ResponseEntity<Map<String, Any?>> {
        val modus = if (request.skarp) "SKARP" else "DRYRUN"
        log.info {
            "Starter $modus for ${request.skattehendelser.size} skattehendelser, maksAntall=${request.maksAntall}"
        }

        skattepliktigeAarsavregningDryrunService.prosesserSkattehendelserAsynkront(
            request.skattehendelser,
            request.skarp,
            request.maksAntall,
        )

        return ResponseEntity.ok(
            mapOf(
                "melding" to "$modus startet",
                "skarp" to request.skarp,
                "maksAntall" to request.maksAntall,
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

data class SkattehendelseRunRequest(
    val skattehendelser: List<SkattehendelseDryrunItem>,
    val skarp: Boolean = false,
    val maksAntall: Int? = null,
)
