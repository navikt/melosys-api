package no.nav.melosys.service.oppgave

import mu.KotlinLogging
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.service.AdminTjeneste
import no.nav.melosys.service.oppgave.migrering.MigreringsRapport
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
class OppgaveMigreringAdminTjeneste(
    private val oppgaveMigrering: OppgaveMigrering,
    private val migreringsRapport: MigreringsRapport,
    @Value("\${Melosys-admin.apikey}") private val apiKey: String
) : AdminTjeneste {
    @PostMapping("")
    fun migrer(
        @RequestHeader(AdminTjeneste.API_KEY_HEADER) apiKey: String?,
        @RequestParam(required = false) bruker: String?,
        @RequestParam(required = false) saksnummer: String?,
        @RequestParam(required = false, defaultValue = "true") dryrun: Boolean,
        @RequestBody(required = false) saksFilter: OppgaveMigrering.SaksFilter =
            OppgaveMigrering.SaksFilter(
                sakstyper = listOf(Sakstyper.EU_EOS),
                sakstemar = listOf(Sakstemaer.MEDLEMSKAP_LOVVALG),
                behandlingstemaer = listOf(Behandlingstema.UTSENDT_SELVSTENDIG),
                behandlingstyper = listOf(Behandlingstyper.FØRSTEGANG)
            )
    ): ResponseEntity<Unit> {
        log.info("Migrer oppgave for bruker $bruker for sak $saksnummer dryrun $dryrun")
        validerApikey(apiKey)

        oppgaveMigrering.go(bruker, saksnummer, dryrun, saksFilter)

        return ResponseEntity.noContent().build()
    }

    @PostMapping("/stop")
    fun stopMigreing(
        @RequestHeader(AdminTjeneste.API_KEY_HEADER) apiKey: String?,
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
