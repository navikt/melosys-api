package no.nav.melosys.service.ftrl

import mu.KotlinLogging
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger { }

@Protected
@RestController
@RequestMapping("/admin/ftrl/finn-saker-for-årsavregning-ikke-skattepliktige")
class FinnSakerÅrsavregningIkkeSkattepliktigeController(
    private val finnSakerÅrsavregningIkkeSkattepliktige: FinnSakerÅrsavregningIkkeSkattepliktige
) {

    @PostMapping("/finn")
    fun finnPersonerOgSendVedtakMeldinger(
        @RequestParam(required = false, defaultValue = "true") dryrun: Boolean,
        @RequestParam(required = false, defaultValue = "0") antallFeilFørStopAvJob: Int,
        @RequestParam(required = false) saksnummer: String?,
    ): ResponseEntity<Unit> {
        log.info(
            "finnSakerÅrsavregningIkkeSkattepliktige - dryrun $dryrun, " +
                "antallFeilFørStopAvJob: $antallFeilFørStopAvJob saksnummer: $saksnummer"
        )

        finnSakerÅrsavregningIkkeSkattepliktige.finnSakerAsynkront(dryrun, antallFeilFørStopAvJob, saksnummer)

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/status")
    fun status(): ResponseEntity<Map<String, Any?>> =
        ResponseEntity<Map<String, Any?>>(finnSakerÅrsavregningIkkeSkattepliktige.status(), HttpStatus.OK)

    @GetMapping("/jsonrapport", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun jsonrapport(): ResponseEntity<String> {
        return ResponseEntity(finnSakerÅrsavregningIkkeSkattepliktige.sakerFunnetJsonString(), HttpStatus.OK)
    }

}
