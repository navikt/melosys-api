package no.nav.melosys.service.oppgave

import mu.KotlinLogging
import no.nav.melosys.service.AdminController
import no.nav.melosys.service.oppgave.migrering.MigreringsRapport
import no.nav.melosys.service.oppgave.migrering.OppgaveIdMigrering
import no.nav.melosys.service.oppgave.migrering.OppgaveMigrering
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger { }

@Unprotected
@RestController
@RequestMapping("/admin/oppgaver/migrering")
class OppgaveMigreringAdminController(
    private val oppgaveMigrering: OppgaveMigrering,
    private val oppgaveIdMigrering: OppgaveIdMigrering,
    private val migreringsRapport: MigreringsRapport,
    @Value("\${Melosys-admin.apikey}") private val apiKey: String
) : AdminController {

    @PostMapping("")
    fun migrer(
        @RequestHeader(AdminController.API_KEY_HEADER) apiKey: String?,
        @RequestParam(required = false) bruker: String?,
        @RequestParam(required = false) saksnummer: String?,
        @RequestParam(required = false, defaultValue = "true") dryrun: Boolean,
        @RequestBody(required = false) options: OppgaveMigrering.Options = OppgaveMigrering.Options()
    ): ResponseEntity<Unit> {
        log.info("Migrer oppgave for bruker $bruker for sak $saksnummer dryrun $dryrun")
        validerApikey(apiKey)

        oppgaveMigrering.go(bruker, saksnummer, dryrun, options)

        return ResponseEntity.noContent().build()
    }

    @PostMapping("/oppgave-id-pa-behandling")
    fun migrerOppgaveIdPaBehandling(
        @RequestHeader(AdminController.API_KEY_HEADER) apiKey: String?,
        @RequestParam(required = false, defaultValue = "true") dryrun: Boolean,
        @RequestBody(required = false) options: OppgaveMigrering.Options = OppgaveMigrering.Options()
    ): ResponseEntity<Unit> {
        log.info("Migrer alle behandlinger for å sette oppgave_id, dryrun=$dryrun")
        validerApikey(apiKey)

        oppgaveIdMigrering.go(dryrun)

        return ResponseEntity.noContent().build()
    }

    @PostMapping("/stop")
    fun stopMigreing(
        @RequestHeader(AdminController.API_KEY_HEADER) apiKey: String?,
    ): ResponseEntity<Unit> {
        log.info("Stopper migreing!")
        validerApikey(apiKey)

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


    override fun getApiKey(): String {
        return apiKey
    }
}
