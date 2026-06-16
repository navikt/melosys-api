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

/**
 * MELOSYS-8125 — admin-endepunkt for å sende innhentingsbrev i etterkant for allerede opprettede
 * årsavregningsbehandlinger (skattepliktige, skatteår 2024 fra MELOSYS-8045-kjøringen).
 */
@Protected
@RestController
@RequestMapping("/admin/aarsavregninger/saker/skattepliktige/innhentingsbrev")
class SkattepliktigeInnhentingsbrevController(
    private val skattepliktigeInnhentingsbrevService: SkattepliktigeInnhentingsbrevService
) {

    @Operation(
        summary = "Send innhentingsbrev for allerede opprettede årsavregninger (dryrun eller skarpt)",
        description = "Sender brevet «Innhenting av inntektsopplysninger» for eksisterende AARSAVREGNING-behandlinger. " +
            "Med skarp=false (default) er det en simulering — ingen brev sendes. " +
            "Med skarp=true sendes faktiske brev, kappet av maksAntall. " +
            "Bruk /status for fremdrift og /rapport for detaljert resultat."
    )
    @PostMapping("/run")
    fun run(
        @RequestBody
        @Parameter(description = "Liste med saker (gjelderPeriode + identifikator), skarp-flagg, og valgfritt maksAntall")
        request: InnhentingsbrevRunRequest
    ): ResponseEntity<Map<String, Any?>> {
        val modus = if (request.skarp) "SKARP" else "DRYRUN"
        log.info {
            "Starter $modus innhentingsbrev for ${request.skattehendelser.size} saker, maksAntall=${request.maksAntall}"
        }

        skattepliktigeInnhentingsbrevService.prosesserSkattehendelserAsynkront(
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
                "statusEndpoint" to "/admin/aarsavregninger/saker/skattepliktige/innhentingsbrev/status",
                "rapportEndpoint" to "/admin/aarsavregninger/saker/skattepliktige/innhentingsbrev/rapport"
            )
        )
    }

    @Operation(summary = "Hent status for pågående eller siste kjøring")
    @GetMapping("/status")
    fun status(): ResponseEntity<Map<String, Any?>> =
        ResponseEntity(skattepliktigeInnhentingsbrevService.status(), HttpStatus.OK)

    @Operation(summary = "Hent detaljert rapport med alle saker fra siste kjøring")
    @GetMapping("/rapport", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun rapport(): ResponseEntity<String> =
        ResponseEntity(skattepliktigeInnhentingsbrevService.rapportJsonString(), HttpStatus.OK)
}

data class InnhentingsbrevRunRequest(
    val skattehendelser: List<InnhentingsbrevItem>,
    val skarp: Boolean = false,
    val maksAntall: Int? = null,
)
