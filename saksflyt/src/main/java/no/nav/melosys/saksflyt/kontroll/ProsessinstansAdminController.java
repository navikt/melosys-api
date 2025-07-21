package no.nav.melosys.saksflyt.kontroll;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import no.nav.melosys.saksflyt.kontroll.dto.HentProsessinstansDto;
import no.nav.melosys.saksflyt.kontroll.dto.RestartProsessinstanserRequest;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Protected
@RestController
@Tags({
    @Tag(name = "prosessinstanser"),
    @Tag(name = "admin")
})
@RequestMapping("/admin/prosessinstanser")
public class ProsessinstansAdminController {

    private static final Logger log = LoggerFactory.getLogger(ProsessinstansAdminController.class);

    private final ProsessinstansAdminService prosessinstansAdminService;

    public ProsessinstansAdminController(ProsessinstansAdminService prosessinstansAdminService) {
        this.prosessinstansAdminService = prosessinstansAdminService;
    }

    @GetMapping("/feilede")
    public ResponseEntity<List<HentProsessinstansDto>> hentFeiledeProsessinstanser() {
        return ResponseEntity.ok(prosessinstansAdminService.hentFeiledeProsessinstanser());
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<HentProsessinstansDto> hentProsessinstansDto(
        @PathVariable UUID uuid
    ) {
        return ResponseEntity.ok(prosessinstansAdminService.hentProsessinstansDto(uuid));
    }

    @GetMapping("/laaste")
    public ResponseEntity<List<HentProsessinstansDto>> hentFastlåsteProsessinstanser() {
        return ResponseEntity.ok(prosessinstansAdminService.hentFastlåsteProsessinstanser());
    }

    @PostMapping("/feilede/restart")
    public ResponseEntity<List<HentProsessinstansDto>> restartAlleFeiledeProsessinstanser() {
        log.info("Forsøker å restarte alle feilede prosessinstanser");

        return ResponseEntity.ok(prosessinstansAdminService.restartAlleFeiledeProsessinstanser());
    }

    @PostMapping("/restart")
    public ResponseEntity<Void> restartProsessinstans(@RequestBody RestartProsessinstanserRequest request) {
        log.info("Forsøker å restarte prosessinstanser {}", request.getUuids());
        prosessinstansAdminService.restartProsessinstanser(request.getUuids());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/hopp-over-steg/{uuid}")
    public ResponseEntity<String> hoppOverStegStegProsessinstans(@PathVariable UUID uuid) {
        log.info("Forsøker å hoppe over steg for prosessinstans {}", uuid);
        var nyttSteg = prosessinstansAdminService.hoppOverStegProsessinstans(uuid);

        prosessinstansAdminService.restartProsessinstanser(Collections.singletonList(uuid));

        return ResponseEntity.ok("SIST_FULLFORTE_STEG for prosessinstans %s satt til %s og prosessinstans restartet".formatted(uuid, nyttSteg.getKode()));
    }

    @PostMapping("/ferdigstill/{uuid}")
    public ResponseEntity<Void> ferdigstillProsessinstans(@PathVariable UUID uuid) {
        log.info("Ferdigstiller prosessinstans {}", uuid);

        prosessinstansAdminService.ferdigstillProsessinstans(uuid);

        return ResponseEntity.noContent().build();
    }
}
