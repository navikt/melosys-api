package no.nav.melosys.service.oppgave;

import no.nav.melosys.service.AdminTjeneste;
import no.nav.melosys.service.eessi.jobb.FeilregistrerX100OppgaverJobb;
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
    private final FeilregistrerX100OppgaverJobb feilregistrerX100OppgaverJobb;
    private final String apiKey;
    private static final Logger log = LoggerFactory.getLogger(OppgaveAdminTjeneste.class);

    public OppgaveAdminTjeneste(OppgaveService oppgaveService,
                                FeilregistrerX100OppgaverJobb feilregistrerX100OppgaverJobb,
                                @Value("${Melosys-admin.apikey}") String apiKey) {
        this.oppgaveService = oppgaveService;
        this.feilregistrerX100OppgaverJobb = feilregistrerX100OppgaverJobb;
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

    @PutMapping("/feilregistrer/x100")
    public ResponseEntity<Void> feilregistrerX100Oppgaver(@RequestHeader(API_KEY_HEADER) String apiKey) {
        validerApikey(apiKey);

        feilregistrerX100OppgaverJobb.feilregistrerX100Oppgaver();

        return ResponseEntity.noContent().build();
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }
}
