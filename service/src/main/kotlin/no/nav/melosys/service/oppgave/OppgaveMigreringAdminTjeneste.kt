package no.nav.melosys.service.oppgave

import mu.KotlinLogging
import no.nav.melosys.service.AdminTjeneste
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger { }

@Unprotected
@RestController
@RequestMapping("/admin/oppgaver/migrering")
class OppgaveMigreringAdminTjeneste(
    private val oppgaveMigrering: OppgaveMigrering,
    @Value("\${Melosys-admin.apikey}") private val apiKey: String
) : AdminTjeneste {
    @PostMapping("")
    fun migrer(
        @RequestHeader(AdminTjeneste.API_KEY_HEADER) apiKey: String?,
        @RequestParam(required = false) bruker: String?,
        @RequestParam(required = false) saksnummer: String?,
        @RequestParam(required = false, defaultValue = "true") dryrun: Boolean
    ): ResponseEntity<Void> {
        log.info("Migrer oppgave for bruker $bruker for sak $saksnummer dryrun $dryrun")
        validerApikey(apiKey)

        oppgaveMigrering.go()

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/status")
    fun status(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity<Map<String, Any>>(oppgaveMigrering.status(), HttpStatus.OK)
    }

    override fun getApiKey(): String {
        return apiKey
    }
}
