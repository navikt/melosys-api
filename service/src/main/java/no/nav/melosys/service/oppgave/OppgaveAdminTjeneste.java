package no.nav.melosys.service.oppgave;

import no.nav.melosys.service.AdminTjeneste;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Unprotected
@RestController
@RequestMapping("/admin/oppgaver")
public class OppgaveAdminTjeneste implements AdminTjeneste {

    private final OppgaveService oppgaveService;
    private final String apiKey;

    public OppgaveAdminTjeneste(@Qualifier("system") OppgaveService oppgaveService,
                                @Value("${Melosys-admin.apikey}") String apiKey) {
        this.oppgaveService = oppgaveService;
        this.apiKey = apiKey;
    }

    /**
     * Oppdateroppgave-endepunktet støtter ikke per nå å endre oppgaver med status FERDIGSTILT
     * Skaper dermed en ny oppgave og knytter denne til saken
     */
    @PostMapping("/opprett/{saksnummer}")
    public ResponseEntity<String> opprettOppgaveTilSak(@RequestHeader(API_KEY_HEADER) String apiKey,
                                                       @PathVariable String saksnummer) {
        validerApikey(apiKey);

        String oppdatertOppgaveId = oppgaveService.opprettOppgaveTilSak(saksnummer);
        return ResponseEntity.ok(String.format("Opprettet oppgave med id %s for sak %s", oppdatertOppgaveId, saksnummer));
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }
}
