package no.nav.melosys.service.oppgave

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import mu.KotlinLogging
import no.nav.security.token.support.core.api.Protected
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Protected
@RestController
@Tags(
    Tag(name = "oppgave"),
    Tag(name = "admin")
)
@RequestMapping("/admin/oppgaver")
class OppgaveAdminController(
    private val oppgaveService: OppgaveService,
    private val feilmerketNøkkelordOpprydding: FeilmerketNøkkelordOpprydding
) {
    private val log = KotlinLogging.logger {}

    @PostMapping("/opprett/{saksnummer}")
    fun opprettOppgaveForSak(@PathVariable saksnummer: String): ResponseEntity<Void> {
        log.info("Forsøker å opprette oppgave for sak $saksnummer")

        oppgaveService.opprettOppgaveForSak(saksnummer)

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/nokkelord-opprydding/rapport")
    fun nøkkelordOppryddingRapport(
        @RequestParam(defaultValue = "4530") enhet: String
    ): ResponseEntity<NøkkelordRapport> =
        ResponseEntity.ok(feilmerketNøkkelordOpprydding.finnFeilmerkede(enhet))

    @PostMapping("/nokkelord-opprydding")
    fun ryddFeilmerketNøkkelord(
        @RequestParam(defaultValue = "4530") enhet: String,
        @RequestParam(defaultValue = "true") dryRun: Boolean
    ): ResponseEntity<Map<String, Any?>> {
        log.info("Starter nøkkelord-opprydding for enhet $enhet (dryRun=$dryRun)")
        feilmerketNøkkelordOpprydding.ryddAsynkront(enhet, dryRun)
        return ResponseEntity.accepted().body(
            mapOf(
                "enhet" to enhet,
                "dryRun" to dryRun,
                "melding" to "Oppryddingen er startet. Følg med på /admin/oppgaver/nokkelord-opprydding/status"
            )
        )
    }

    @GetMapping("/nokkelord-opprydding/status")
    fun nøkkelordOppryddingStatus(): ResponseEntity<Map<String, Any?>> =
        ResponseEntity.ok(feilmerketNøkkelordOpprydding.status())
}
