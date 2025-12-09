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
     * Starter jobben for å rette opp Del 2 saker som ble feilaktig oppdatert av AvsluttArt13BehandlingJobb.
     *
     * Del 2: Fagsaker med flere behandlinger der A003 har blitt ugyldiggjort (X008/X006).
     *
     * Del 2a: Siste behandling har resultat REGISTRERT_UNNTAK
     * - Saksstatus endres IKKE
     * - Kun MEDL-perioder på HENLEGGELSE-behandlinger settes til avvist
     *
     * Del 2b: Alle behandlinger har resultat HENLEGGELSE
     * - Saksstatus settes til ANNULLERT
     * - Alle MEDL-perioder settes til avvist
     *
     * @param dryRun Hvis true, vil jobben kun logge hva som ville blitt endret uten å gjøre endringer.
     *               Default er true for å unngå utilsiktede endringer.
     * @param antallFeilFørStopp Antall feil før jobben stopper. 0 = ingen grense.
     * @param batchStørrelse Maks antall behandlinger per kjøring. Default 1000.
     * @param startFraBehandlingId Start fra behandlinger med id > denne verdien. Bruk sisteBehandledeId fra /status for å fortsette.
     * @param brukSafeListeFilter Hvis true, prosesser kun behandlingIder fra forhåndsanalysert SAFE-liste.
     * @param saksnummer Hvis oppgitt, prosesser kun behandlinger for denne fagsaken. Nyttig for testing av én sak.
     */
    @PostMapping("/kjør")
    fun kjør(
        @RequestParam(required = false, defaultValue = "true") dryRun: Boolean,
        @RequestParam(required = false, defaultValue = "10") antallFeilFørStopp: Int,
        @RequestParam(required = false, defaultValue = "1000") batchStørrelse: Int,
        @RequestParam(required = false, defaultValue = "0") startFraBehandlingId: Long,
        @RequestParam(required = false, defaultValue = "false") brukSafeListeFilter: Boolean,
        @RequestParam(required = false) saksnummer: String?
    ): ResponseEntity<Map<String, Any>> {
        val filter = if (brukSafeListeFilter) rettOppFeilMedlPerioderJob.knownSafeIds else null
        val filterNavn = if (brukSafeListeFilter) "Safe" else "ingen"

        log.info { "Starter RettOppFeilMedlPerioderJob Del 2 (dryRun=$dryRun, antallFeilFørStopp=$antallFeilFørStopp, batchStørrelse=$batchStørrelse, startFraBehandlingId=$startFraBehandlingId, filter=$filterNavn (${filter?.size ?: 0}), saksnummer=${saksnummer ?: "alle"})" }

        rettOppFeilMedlPerioderJob.kjørAsynkront(dryRun, antallFeilFørStopp, batchStørrelse, startFraBehandlingId, filter, saksnummer)

        return ResponseEntity.ok(
            mapOf(
                "melding" to "Job startet (Del 2)",
                "dryRun" to dryRun,
                "antallFeilFørStopp" to antallFeilFørStopp,
                "batchStørrelse" to batchStørrelse,
                "startFraBehandlingId" to startFraBehandlingId,
                "brukSafeListeFilter" to brukSafeListeFilter,
                "filterNavn" to filterNavn,
                "filterStørrelse" to (filter?.size ?: 0),
                "saksnummer" to (saksnummer ?: "alle")
            )
        )
    }

    /**
     * Returnerer totalt antall behandlinger som matcher kriteriene.
     */
    @GetMapping("/totalt-antall")
    fun totaltAntall(): ResponseEntity<Map<String, Long>> =
        ResponseEntity.ok(mapOf("antall" to rettOppFeilMedlPerioderJob.totaltAntall()))

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
     */
    @DeleteMapping("/rapport")
    fun tømRapport(): ResponseEntity<Map<String, String>> {
        val størrelse = rettOppFeilMedlPerioderJob.rapportStørrelse()
        rettOppFeilMedlPerioderJob.tømRapport()
        log.info { "Tømte rapport med $størrelse oppføringer" }
        return ResponseEntity.ok(mapOf("melding" to "Rapport tømt ($størrelse oppføringer)"))
    }
}
