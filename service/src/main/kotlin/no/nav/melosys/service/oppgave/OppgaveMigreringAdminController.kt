package no.nav.melosys.service.oppgave

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import mu.KotlinLogging
import no.nav.melosys.service.oppgave.migrering.MigreringsRapport
import no.nav.melosys.service.oppgave.migrering.OppgaveIdMigrering
import no.nav.melosys.service.oppgave.migrering.OppgaveIdMigreringRapport
import no.nav.melosys.service.oppgave.migrering.OppgaveMigrering
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger { }

@RestController
@RequestMapping("/admin/oppgaver/migrering")
@Tags(
    Tag(name = "oppgave-migrering"),
    Tag(name = "admin")
)
class OppgaveMigreringAdminController(
    private val oppgaveMigrering: OppgaveMigrering,
    private val oppgaveIdMigrering: OppgaveIdMigrering,
    private val migreringsRapport: MigreringsRapport,
    private val migreringIdMigreringRapport: OppgaveIdMigreringRapport
) {
    @PostMapping("")
    fun migrer(
        @RequestParam(required = false) bruker: String?,
        @RequestParam(required = false) saksnummer: String?,
        @RequestParam(required = false, defaultValue = "true") dryrun: Boolean,
        @RequestBody(required = false) options: OppgaveMigrering.Options = OppgaveMigrering.Options()
    ): ResponseEntity<Unit> {
        log.info("Migrer oppgave for bruker $bruker for sak $saksnummer dryrun $dryrun")
        oppgaveMigrering.go(bruker, saksnummer, dryrun, options)

        return ResponseEntity.noContent().build()
    }


    @PostMapping("/stop")
    fun stopMigreing(): ResponseEntity<Unit> {
        log.info("Stopper migreing!")
        oppgaveMigrering.stop()

        return ResponseEntity.noContent().build()
    }


    @GetMapping("/status")
    fun status(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity<Map<String, Any>>(migreringsRapport.status(), HttpStatus.OK)
    }

    @GetMapping("/jsonrapport", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun jsonrapport(): ResponseEntity<String> {
        return ResponseEntity(migreringsRapport.migreringsSakListeSomJsonString(), HttpStatus.OK)
    }


    @PostMapping("/oppgave-id-pa-behandling")
    fun migrerOppgaveIdPaBehandling(
        @RequestParam(required = false, defaultValue = "true") dryrun: Boolean,
        @RequestBody(required = false) options: OppgaveMigrering.Options = OppgaveMigrering.Options()
    ): ResponseEntity<Unit> {
        log.info("Migrer alle behandlinger for å sette oppgave_id, dryrun=$dryrun")
        oppgaveIdMigrering.migrerOppgaver(dryrun)

        return ResponseEntity.noContent().build()
    }

    @PostMapping("/oppgave-id-pa-behandling/stop")
    fun stoppMigrerOppgaveIdPaBehandling(): ResponseEntity<Unit> {
        log.info("Stopp migrering for å sette oppgave_id")
        oppgaveIdMigrering.stop()

        return ResponseEntity.noContent().build()
    }


    @GetMapping("/oppgave-id-pa-behandling/status")
    fun hentStatusForMigrerOppgaveIdPaBehandling(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity<Map<String, Any>>(migreringIdMigreringRapport.status(), HttpStatus.OK)
    }

    @PostMapping("/oppgave-id-pa-behandling/jsonrapport")
    fun hentJsonRapportForMigrerOppgaveIdPaBehandling(
        @RequestParam(required = false, defaultValue = "true") dryrun: Boolean,
        @RequestBody(required = false) options: OppgaveMigrering.Options = OppgaveMigrering.Options()
    ): ResponseEntity<String> {
        log.info("Migrer alle behandlinger for å sette oppgave_id, dryrun=$dryrun")
        return ResponseEntity(migreringIdMigreringRapport.migreringsOppgaveMigrertListeSomJsonString(), HttpStatus.OK)
    }
}
