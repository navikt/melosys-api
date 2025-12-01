package no.nav.melosys.service.behandling.jobb

import mu.KotlinLogging
import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.api.Unprotected
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
     * @param startFraBehandlingId Start fra behandlinger med id > denne verdien. Bruk sisteBehandledeId fra /status for å fortsette.
     */
    @PostMapping("/kjør")
    fun kjør(
        @RequestParam(required = false, defaultValue = "true") dryRun: Boolean,
        @RequestParam(required = false, defaultValue = "10") antallFeilFørStopp: Int,
        @RequestParam(required = false, defaultValue = "1000") batchStørrelse: Int,
        @RequestParam(required = false, defaultValue = "0") startFraBehandlingId: Long
    ): ResponseEntity<Map<String, Any>> {
        log.info { "Starter RettOppFeilMedlPerioderJob (dryRun=$dryRun, antallFeilFørStopp=$antallFeilFørStopp, batchStørrelse=$batchStørrelse, startFraBehandlingId=$startFraBehandlingId)" }

        rettOppFeilMedlPerioderJob.kjørAsynkront(dryRun, antallFeilFørStopp, batchStørrelse, startFraBehandlingId)

        return ResponseEntity.ok(
            mapOf(
                "melding" to "Job startet",
                "dryRun" to dryRun,
                "antallFeilFørStopp" to antallFeilFørStopp,
                "batchStørrelse" to batchStørrelse,
                "startFraBehandlingId" to startFraBehandlingId
            )
        )
    }

    /**
     * Kjører EN batch og returnerer resultatene direkte.
     * Dette er en stateless endpoint - hvert kall er uavhengig og frigjør minne etter respons.
     *
     * Brukes av Python-script (scripts/rett_opp_batch_runner.py) for batch-prosessering
     * uten å akkumulere data i minnet. API-nøkkel kreves fortsatt via X-MELOSYS-ADMIN-APIKEY header.
     *
     * @param dryRun Hvis true, vil bare analysere uten å gjøre endringer. Default true.
     * @param batchStørrelse Maks antall behandlinger å prosessere i denne batchen. Default 10 (pga 30s NAIS timeout).
     * @param startFraBehandlingId Start fra behandlinger med id > denne verdien. Bruk nextStartFraBehandlingId fra forrige respons.
     */
    @PostMapping("/kjor-en-batch")
    @Unprotected // Kjøres via pyton script - API-nøkkel kreves fortsatt via ApiKeyInterceptor
    fun kjørEnBatch(
        @RequestParam(required = false, defaultValue = "true") dryRun: Boolean,
        @RequestParam(required = false, defaultValue = "10") batchStørrelse: Int,
        @RequestParam(required = false, defaultValue = "0") startFraBehandlingId: Long
    ): ResponseEntity<RettOppFeilMedlPerioderJob.BatchResultat> {
        log.info { "Kjører én batch (dryRun=$dryRun, batchStørrelse=$batchStørrelse, startFraBehandlingId=$startFraBehandlingId)" }

        val resultat = rettOppFeilMedlPerioderJob.kjørEnBatch(dryRun, batchStørrelse, startFraBehandlingId)

        return ResponseEntity.ok(resultat)
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
