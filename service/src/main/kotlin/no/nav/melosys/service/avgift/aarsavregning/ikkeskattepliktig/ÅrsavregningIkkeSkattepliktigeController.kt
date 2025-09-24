package no.nav.melosys.service.avgift.aarsavregning.ikkeskattepliktig

import mu.KotlinLogging
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

private val log = KotlinLogging.logger { }

@Protected
@RestController
@RequestMapping("/admin/aarsavregninger/saker/ikke-skattepliktige")
class ÅrsavregningIkkeSkattepliktigeController(
    private val årsavregningIkkeSkattepliktigeProsessGenerator: ÅrsavregningIkkeSkattepliktigeProsessGenerator
) {

    @PostMapping("/finn")
    fun finnPersonerOgLagProsessinstanser(
        @RequestParam(required = true, defaultValue = "true") fomDato: LocalDate,
        @RequestParam(required = true, defaultValue = "true") tomDato: LocalDate,
        @RequestParam(required = false, defaultValue = "true") dryrun: Boolean,
        @RequestParam(required = false, defaultValue = "0") antallFeilFørStopAvJob: Int,
        @RequestParam(required = false) saksnummer: String?,
    ): ResponseEntity<Unit> {
        log.info(
            "finnSakerÅrsavregningIkkeSkattepliktige - dryrun $dryrun, " +
                "antallFeilFørStopAvJob: $antallFeilFørStopAvJob saksnummer: $saksnummer"
        )

        årsavregningIkkeSkattepliktigeProsessGenerator.finnSakerAsynkront(dryrun, antallFeilFørStopAvJob, saksnummer, fomDato, tomDato)

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/status")
    fun status(): ResponseEntity<Map<String, Any?>> =
        ResponseEntity<Map<String, Any?>>(årsavregningIkkeSkattepliktigeProsessGenerator.status(), HttpStatus.OK)

    @GetMapping("/rapport", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun jsonrapport(): ResponseEntity<String> {
        return ResponseEntity(årsavregningIkkeSkattepliktigeProsessGenerator.sakerFunnetJsonString(), HttpStatus.OK)
    }

}
