package no.nav.melosys.service.behandling;

import no.nav.security.token.support.core.api.Unprotected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Unprotected
@RestController
@RequestMapping("/admin/behandlinger")
public class BehandlingAdminController {
    private final BehandlingService behandlingService;
    private static final Logger log = LoggerFactory.getLogger(BehandlingAdminController.class);


    public BehandlingAdminController(BehandlingService behandlingService) {
        this.behandlingService = behandlingService;
    }

    @PutMapping("/{behandlingId}/avslutt")
    public ResponseEntity<Void> avsluttBehandling(@PathVariable Long behandlingId) {
        log.info("Forsøker å avslutte behandling {}", behandlingId);
        behandlingService.avsluttBehandling(behandlingId);

        return ResponseEntity.noContent().build();
    }
}
