package no.nav.melosys.saksflyt.kontroll;

import java.util.List;
import java.util.UUID;

import no.nav.melosys.saksflyt.kontroll.dto.HentProsessinstansDto;
import no.nav.melosys.saksflyt.kontroll.dto.RestartProsessinstanserRequest;
import no.nav.melosys.service.AdminTjeneste;
import no.nav.security.token.support.core.api.Unprotected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Unprotected
@RestController
@RequestMapping("/admin/prosessinstanser")
public class ProsessinstansAdminTjeneste implements AdminTjeneste {

    private final Logger log = LoggerFactory.getLogger(ProsessinstansAdminTjeneste.class);

    private final ProsessinstansAdminService prosessinstansAdminService;
    private final String apiKey;

    public ProsessinstansAdminTjeneste(ProsessinstansAdminService prosessinstansAdminService,
                                       @Value("${Melosys-admin.apikey}") String apiKey) {
        this.prosessinstansAdminService = prosessinstansAdminService;
        this.apiKey = apiKey;
    }

    @GetMapping("/feilede")
    public ResponseEntity<List<HentProsessinstansDto>> hentFeiledeProsessinstanser(
        @RequestHeader(API_KEY_HEADER) String apiKey) {

        validerApikey(apiKey);
        return ResponseEntity.ok(prosessinstansAdminService.hentFeiledeProsessinstanser());
    }

    @PostMapping("/feilede/restart")
    public ResponseEntity<List<HentProsessinstansDto>> restartAlleFeiledeProsessinstanser(
        @RequestHeader(API_KEY_HEADER) String apiKey) {

        validerApikey(apiKey);
        return ResponseEntity.ok(prosessinstansAdminService.restartAlleFeiledeProsessinstanser());
    }

    @PostMapping("/restart")
    public ResponseEntity<Void> restartProsessinstans(@RequestHeader(API_KEY_HEADER) String apiKey,
                                                      @RequestBody RestartProsessinstanserRequest request) {
        validerApikey(apiKey);

        log.info("Forsøker å restarte prosessinstanser {}", request.getUuids());
        prosessinstansAdminService.restartProsessinstanser(request.getUuids());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/skip-steg/{uuid}")
    public ResponseEntity<String> skipStegProsessinstans(@RequestHeader(API_KEY_HEADER) String apiKey,
                                                         @PathVariable UUID uuid) {
        validerApikey(apiKey);

        log.info("Forsøker å hoppe over steg for prosessinstans {}", uuid);
        var nyttSteg = prosessinstansAdminService.skipStegProsessinstans(uuid);

        return ResponseEntity.ok("SIST_FULLFORTE_STEG for prosessinstans %s satt til %s".formatted(uuid, nyttSteg.getKode()));
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }
}
