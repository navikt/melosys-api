package no.nav.melosys.service.oppgave;

import no.nav.melosys.service.AdminTjeneste;
import no.nav.security.token.support.core.api.Unprotected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Unprotected
@RestController
@RequestMapping("/admin/oppgaver")
public class OppgaveAdminTjeneste implements AdminTjeneste {

    private final OppgaveService oppgaveService;
    private final String apiKey;
    private static final Logger log = LoggerFactory.getLogger(OppgaveAdminTjeneste.class);

    public OppgaveAdminTjeneste(OppgaveService oppgaveService, @Value("${Melosys-admin.apikey}") String apiKey) {
        this.oppgaveService = oppgaveService;
        this.apiKey = apiKey;
    }

    @PostMapping("/opprett/{saksnummer}")
    public ResponseEntity<Void> opprettOppgaveForSak(@RequestHeader(API_KEY_HEADER) String apiKey,
                                                       @PathVariable String saksnummer) {
        validerApikey(apiKey);

        log.info("Forsøker å opprette oppgave for sak {}", saksnummer);
        oppgaveService.opprettOppgaveForSak(saksnummer);

        return ResponseEntity.noContent().build();
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }
}
