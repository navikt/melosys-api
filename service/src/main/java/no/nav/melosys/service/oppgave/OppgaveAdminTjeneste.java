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

    @PostMapping("/gjenaapne/{saksnummer}")
    public ResponseEntity<String> gjenåpneOppgave(@RequestHeader(API_KEY_HEADER) String apiKey,
                                                  @PathVariable String saksnummer) {
        validerApikey(apiKey);

        String oppdatertOppgaveId = oppgaveService.gjenåpneOppgaveMedFagsaksnummer(saksnummer);
        return ResponseEntity.ok(String.format("Gjenåpnet oppgave med id %s", oppdatertOppgaveId));
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }
}
