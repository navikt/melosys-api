package no.nav.melosys.service.behandling;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
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
    @Operation(summary = "Avslutt en behandling",
               description = "Avslutter en behandling med valgfri behandlingsresultattype. Kun default IKKE_FASTSATT kan overstyres.")
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

    @PutMapping("/batch/avslutt")
    @Operation(summary = "Avslutt flere behandlinger samtidig",
               description = "Avslutter flere behandlinger i en batch-operasjon med valgfri behandlingsresultattype. Kun default IKKE_FASTSATT kan overstyres. Returnerer liste over vellykkede og feilede behandlinger.")
    public ResponseEntity<BatchAvsluttResultat> avsluttBehandlinger(@RequestBody BatchAvsluttRequest request) {
        log.info("Admin forsøker å avslutte {} behandlinger med behandlingsresultattype {}",
                request.behandlingIDs().size(), request.behandlingsresultattype());

        List<Long> suksess = new ArrayList<>();
        List<BatchAvsluttFeil> feil = new ArrayList<>();

        for (Long behandlingID : request.behandlingIDs()) {
            try {
                if (request.behandlingsresultattype() != null) {
                    behandlingService.avsluttBehandling(behandlingID, request.behandlingsresultattype());
                } else {
                    behandlingService.avsluttBehandling(behandlingID);
                }
                suksess.add(behandlingID);
            } catch (FunksjonellException e) {
                log.warn("Feil ved avslutting av behandling {}: {}", behandlingID, e.getMessage());
                feil.add(new BatchAvsluttFeil(behandlingID, e.getMessage()));
            }
        }

        log.info("Batch avslutt fullført: {} suksess, {} feil", suksess.size(), feil.size());
        return ResponseEntity.ok(new BatchAvsluttResultat(suksess, feil));
    }

    public record BatchAvsluttRequest(
            List<Long> behandlingIDs,
            Behandlingsresultattyper behandlingsresultattype
    ) {}

    public record BatchAvsluttResultat(
            List<Long> suksess,
            List<BatchAvsluttFeil> feil
    ) {}

    public record BatchAvsluttFeil(
            Long behandlingID,
            String feilmelding
    ) {}
}
