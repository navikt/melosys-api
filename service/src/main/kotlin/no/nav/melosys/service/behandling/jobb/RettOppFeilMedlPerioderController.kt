package no.nav.melosys.service.behandling.jobb

import mu.KotlinLogging
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger { }

@Unprotected
@RestController
@RequestMapping("/admin/rett-opp-feil-medl-perioder")
class RettOppFeilMedlPerioderController(
    private val rettOppFeilMedlPerioderJob: RettOppFeilMedlPerioderJob
) {

    /**
     * Starter jobben for å rette opp saker som ble feilaktig oppdatert av AvsluttArt13BehandlingJobb.
     *
     * @param dryRun Hvis true, vil jobben kun logge hva som ville blitt endret uten å gjøre endringer.
     *               Default er true for å unngå utilsiktede endringer.
     * @param antallFeilFørStopp Antall feil før jobben stopper. 0 = ingen grense.
     */
    @PostMapping("/kjør")
    fun kjør(
        @RequestParam(required = false, defaultValue = "true") dryRun: Boolean,
        @RequestParam(required = false, defaultValue = "10") antallFeilFørStopp: Int
    ): ResponseEntity<Map<String, Any>> {
        log.info { "Starter RettOppFeilMedlPerioderJob (dryRun=$dryRun, antallFeilFørStopp=$antallFeilFørStopp)" }

        rettOppFeilMedlPerioderJob.kjørAsynkront(dryRun, antallFeilFørStopp)

        return ResponseEntity.ok(mapOf(
            "melding" to "Job startet",
            "dryRun" to dryRun,
            "antallFeilFørStopp" to antallFeilFørStopp
        ))
    }

    /**
     * Henter status for jobben.
     */
    @GetMapping("/status")
    fun status(): ResponseEntity<Map<String, Any?>> =
        ResponseEntity(rettOppFeilMedlPerioderJob.status(), HttpStatus.OK)

    /**
     * Stopper jobben hvis den kjører.
     */
    @PostMapping("/stopp")
    fun stopp(): ResponseEntity<Map<String, Any>> {
        log.info { "Stopper RettOppFeilMedlPerioderJob" }
        rettOppFeilMedlPerioderJob.stopp()
        return ResponseEntity.ok(mapOf("melding" to "Stoppforespørsel sendt"))
    }

    /**
     * Henter JSON-rapport med detaljert info om alle behandlede saker.
     * Denne rapporten kan brukes lokalt for analyse og feilsøking.
     */
    @GetMapping("/rapport", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun jsonRapport(): ResponseEntity<String> =
        ResponseEntity(rettOppFeilMedlPerioderJob.sakerFunnetJsonString(), HttpStatus.OK)
}
