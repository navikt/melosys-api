package no.nav.melosys.statistikk.utstedt_a1.api;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import no.nav.melosys.repository.VedtakMetadataRepository;
import no.nav.melosys.service.AdminTjeneste;
import no.nav.melosys.statistikk.utstedt_a1.service.UtstedtA1Service;
import no.nav.security.token.support.core.api.Unprotected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Unprotected
@RestController
@RequestMapping("/admin/utstedtA1")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class UtstedtA1AdminTjeneste implements AdminTjeneste {
    private static final Logger log = LoggerFactory.getLogger(UtstedtA1AdminTjeneste.class);

    private final UtstedtA1Service utstedtA1Service;
    private final VedtakMetadataRepository vedtakMetadataRepository;
    private final String apiKey;

    public UtstedtA1AdminTjeneste(
        UtstedtA1Service utstedtA1Service,
        VedtakMetadataRepository vedtakMetadataRepository,
        @Value("${Melosys-admin.apikey}") String apiKey
    ) {
        this.utstedtA1Service = utstedtA1Service;
        this.vedtakMetadataRepository = vedtakMetadataRepository;
        this.apiKey = apiKey;
    }

    @PostMapping("/{behandlingID}/publiserMelding")
    public ResponseEntity<Void> publiserMelding(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @PathVariable long behandlingID) {
        validerApikey(apiKey);

        log.info("Forsøker å produserer melding om utstedt A1 for behandling {}", behandlingID);
        utstedtA1Service.sendMeldingOmUtstedtA1(behandlingID);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/publiserMelding/eksisterendeBehandlinger")
    public ResponseEntity<Map<String, Set<Long>>> publiserEksisterendeBehandlinger(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @RequestParam("fom") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fom
    ) {
        validerApikey(apiKey);

        return ResponseEntity.ok(
            publiserEksisterendeBehandlinger(vedtakMetadataRepository
                .findBehandlingsresultatIdByRegistrertDatoIsGreaterThanEqual(
                    fom.atStartOfDay(ZoneId.systemDefault()).toInstant())
            ));
    }

    private Map<String, Set<Long>> publiserEksisterendeBehandlinger(Collection<Long> eksisterendeBehandlinger) {
        Set<Long> sendteBehandlinger = new HashSet<>();
        Set<Long> feiledeBehandlinger = new HashSet<>();
        for (Long behandlingID : eksisterendeBehandlinger) {
            try {
                utstedtA1Service.sendMeldingOmUtstedtA1(behandlingID);
                sendteBehandlinger.add(behandlingID);
            } catch (Exception e) {
                feiledeBehandlinger.add(behandlingID);
                log.error("Melding om utstedt A1 for behandling {} ble ikke sendt", behandlingID, e);
            }
        }

        log.info("Sendt melding om utstedt A1 for {} behandlinger", sendteBehandlinger.size());
        log.info("Melding om utstedt A1 feilet for {} behandlinger", feiledeBehandlinger.size());
        return Map.of(
            "sendteBehandlinger", sendteBehandlinger,
            "feiledeBehandlinger", feiledeBehandlinger);
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }
}
