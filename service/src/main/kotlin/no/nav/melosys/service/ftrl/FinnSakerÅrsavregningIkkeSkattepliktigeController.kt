package no.nav.melosys.service.ftrl

import mu.KotlinLogging
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger { }

@Unprotected // Kun for local kjøring mot q2 - TODO: Husk å sette denne tilbake
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
}
