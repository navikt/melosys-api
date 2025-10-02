package no.nav.melosys.service.avgift.aarsavregning.ikkeskattepliktig

import io.swagger.v3.oas.annotations.Parameter
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
        @RequestParam(required = true)
        @Parameter(description = "Startdato for søk", example = "2024-01-01")
        fomDato: LocalDate,

        @RequestParam(required = true)
        @Parameter(description = "Sluttdato for søk", example = "2024-12-31")
        tomDato: LocalDate,

        @RequestParam(required = false, defaultValue = "false")
        @Parameter(description = "Kjør som skarpt så vil lage lage Prosessinstanser", example = "false")
        lagProsessinstanser: Boolean = true,

        @RequestParam(required = false, defaultValue = "0")
        @Parameter(description = "Antall feil før jobben stopper", example = "0")
        antallFeilFørStopAvJob: Int = 0,

        @RequestParam(required = false)
        @Parameter(description = "Saksnummer")
        saksnummer: String?,
    ): ResponseEntity<Map<String, Any?>> {

        log.info("lagProsessinstanser: $lagProsessinstanser")

        årsavregningIkkeSkattepliktigeProsessGenerator.finnSakerOgLagProsessinstanserAsynkront(
            !lagProsessinstanser, antallFeilFørStopAvJob, saksnummer, fomDato, tomDato
        )

        return ResponseEntity.ok(
            mapOf(
                "fomDato" to fomDato,
                "tomDato" to tomDato,
                "lagProsessinstanser" to lagProsessinstanser,
                "dryrun" to !lagProsessinstanser,
                "antallFeilFørStopAvJob" to antallFeilFørStopAvJob,
                "saksnummer" to saksnummer
            )
        )
    }

    @GetMapping("/status")
    fun status(): ResponseEntity<Map<String, Any?>> =
        ResponseEntity<Map<String, Any?>>(årsavregningIkkeSkattepliktigeProsessGenerator.status(), HttpStatus.OK)

    @GetMapping("/rapport", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun jsonrapport(): ResponseEntity<String> {
        return ResponseEntity(årsavregningIkkeSkattepliktigeProsessGenerator.sakerFunnetJsonString(), HttpStatus.OK)
    }

}
