package no.nav.melosys.service.behandling;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Protected
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

    @PutMapping("/{behandlingID}/avslutt")
    public ResponseEntity<Void> avsluttBehandling(@PathVariable Long behandlingID, @RequestParam(required = false) Behandlingsresultattyper behandlingsresultattype) {

        if (behandlingsresultattype != null) {
            log.info("Admin forsøker å avslutte behandling {} med behandlingsresultattype {}", behandlingID, behandlingsresultattype);
            behandlingService.avsluttBehandling(behandlingID, behandlingsresultattype);
        } else {
            log.info("Admin forsøker å avslutte behandling {}", behandlingID);
            behandlingService.avsluttBehandling(behandlingID);
        }

        return ResponseEntity.noContent().build();
    }
}
