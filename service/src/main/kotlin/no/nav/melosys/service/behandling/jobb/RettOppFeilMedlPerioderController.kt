package no.nav.melosys.service.behandling.jobb

import mu.KotlinLogging
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger { }

@Protected
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
     * @param batchStørrelse Maks antall behandlinger per kjøring. Default 1000.
     * @param offset Hvor mange behandlinger som skal hoppes over (for å fortsette fra forrige kjøring).
     */
    @PostMapping("/kjør")
    fun kjør(
        @RequestParam(required = false, defaultValue = "true") dryRun: Boolean,
        @RequestParam(required = false, defaultValue = "10") antallFeilFørStopp: Int,
        @RequestParam(required = false, defaultValue = "1000") batchStørrelse: Int,
        @RequestParam(required = false, defaultValue = "0") offset: Int
    ): ResponseEntity<Map<String, Any>> {
        log.info { "Starter RettOppFeilMedlPerioderJob (dryRun=$dryRun, antallFeilFørStopp=$antallFeilFørStopp, batchStørrelse=$batchStørrelse, offset=$offset)" }

        rettOppFeilMedlPerioderJob.kjørAsynkront(dryRun, antallFeilFørStopp, batchStørrelse, offset)

        return ResponseEntity.ok(
            mapOf(
                "melding" to "Job startet",
                "dryRun" to dryRun,
                "antallFeilFørStopp" to antallFeilFørStopp,
                "batchStørrelse" to batchStørrelse,
                "offset" to offset
            )
        )
    }

    /**
     * Returnerer totalt antall behandlinger som matcher kriteriene for begge scenarioer.
     * Brukes for å planlegge hvor mange batches som trengs.
     */
    @GetMapping("/totalt-antall")
    fun totaltAntall(): ResponseEntity<RettOppFeilMedlPerioderJob.TotaltAntall> =
        ResponseEntity.ok(rettOppFeilMedlPerioderJob.totaltAntall())

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

    /**
     * Returnerer antall oppføringer i rapporten uten å laste alle data.
     */
    @GetMapping("/rapport/stoerrelse")
    fun rapportStørrelse(): ResponseEntity<Map<String, Int>> =
        ResponseEntity.ok(mapOf("stoerrelse" to rettOppFeilMedlPerioderJob.rapportStørrelse()))

    /**
     * Tømmer rapporten for å frigjøre minne.
     * Bør kalles etter at rapporten er hentet hvis dataene ikke lenger trengs.
     */
    @DeleteMapping("/rapport")
    fun tømRapport(): ResponseEntity<Map<String, String>> {
        val størrelse = rettOppFeilMedlPerioderJob.rapportStørrelse()
        rettOppFeilMedlPerioderJob.tømRapport()
        log.info { "Tømte rapport med $størrelse oppføringer" }
        return ResponseEntity.ok(mapOf("melding" to "Rapport tømt ($størrelse oppføringer)"))
    }
}
