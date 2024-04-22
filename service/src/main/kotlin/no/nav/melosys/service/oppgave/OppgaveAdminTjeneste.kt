package no.nav.melosys.service.oppgave

import no.nav.melosys.service.AdminTjeneste
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Unprotected
@RestController
@RequestMapping("/admin/oppgaver")
class OppgaveAdminTjeneste(
    private val oppgaveService: OppgaveService,
    @param:Value("\${Melosys-admin.apikey}") private val apiKey: String
) : AdminTjeneste {
    @PostMapping("/opprett/{saksnummer}")
    fun opprettOppgaveForSak(
        @RequestHeader(AdminTjeneste.API_KEY_HEADER) apiKey: String,
        @PathVariable saksnummer: String
    ): ResponseEntity<Void> {
        validerApikey(apiKey)
        log.info("Forsøker å opprette oppgave for sak $saksnummer")

        oppgaveService.opprettOppgaveForSak(saksnummer)

        return ResponseEntity.noContent().build()
    }

    override fun getApiKey(): String {
        return apiKey
    }

    companion object {
        private val log = LoggerFactory.getLogger(OppgaveAdminTjeneste::class.java)
    }
}
