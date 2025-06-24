package no.nav.melosys.service.oppgave

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import mu.KotlinLogging
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Tags(
    Tag(name = "oppgave"),
    Tag(name = "admin")
)
@RequestMapping("/admin/oppgaver")
class OppgaveAdminController(private val oppgaveService: OppgaveService) {
    private val log = KotlinLogging.logger {}

    @PostMapping("/opprett/{saksnummer}")
    fun opprettOppgaveForSak(@PathVariable saksnummer: String): ResponseEntity<Void> {
        log.info("Forsøker å opprette oppgave for sak $saksnummer")

        oppgaveService.opprettOppgaveForSak(saksnummer)

        return ResponseEntity.noContent().build()
    }
}
