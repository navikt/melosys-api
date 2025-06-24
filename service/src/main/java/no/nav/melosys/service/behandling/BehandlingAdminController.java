package no.nav.melosys.service.behandling;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import no.nav.security.token.support.core.api.Unprotected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Tags({
    @Tag(name = "behandling"),
    @Tag(name = "admin")
})
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
