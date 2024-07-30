package no.nav.melosys.service.behandling;

import no.nav.melosys.service.AdminController;
import no.nav.security.token.support.core.api.Unprotected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/behandlinger")
public class BehandlingAdminController implements AdminController {
    private final String apiKey;
    private final BehandlingService behandlingService;
    private static final Logger log = LoggerFactory.getLogger(BehandlingAdminController.class);


    public BehandlingAdminController(@Value("${Melosys-admin.apikey}") String apiKey, BehandlingService behandlingService) {
        this.apiKey = apiKey;
        this.behandlingService = behandlingService;
    }

    @PutMapping("/{behandlingId}/avslutt")
    public ResponseEntity<Void> avsluttBehandling(@PathVariable Long behandlingId, @RequestHeader(API_KEY_HEADER) String apiKey) {
        validerApikey(apiKey);

        log.info("Forsøker å avslutte behandling {}", behandlingId);
        behandlingService.avsluttBehandling(behandlingId);

        return ResponseEntity.noContent().build();
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }
}
