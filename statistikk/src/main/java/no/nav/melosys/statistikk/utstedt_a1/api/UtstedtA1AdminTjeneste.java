package no.nav.melosys.statistikk.utstedt_a1.api;

import java.util.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.AdminTjeneste;
import no.nav.melosys.statistikk.utstedt_a1.service.UtstedtA1Service;
import no.nav.security.token.support.core.api.Unprotected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
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
    private final BehandlingRepository behandlingRepository;
    private final String apiKey;

    @Autowired
    public UtstedtA1AdminTjeneste(
        UtstedtA1Service utstedtA1Service,
        BehandlingRepository behandlingRepository,
        @Value("${Melosys-admin.apikey}") String apiKey
    ) {
        this.utstedtA1Service = utstedtA1Service;
        this.behandlingRepository = behandlingRepository;
        this.apiKey = apiKey;
    }

    @PostMapping("/{behandlingID}/publiserMelding")
    public ResponseEntity<Void> publiserMelding(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @PathVariable long behandlingID
    ) throws FunksjonellException, TekniskException {
        validerApikey(apiKey);
        utstedtA1Service.sendMeldingOmUtstedtA1(behandlingID);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/publiserMelding/eksisterendeBehandlingerFom")
    public ResponseEntity<Map<String, Set<Long>>> publiserEksisterendeBehandlingerFomDato(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @RequestBody EksisterendeBehandlingerFomDto eksisterendeBehandlingerFomDto
    ) throws SikkerhetsbegrensningException {
        validerApikey(apiKey);
        return ResponseEntity.ok(
            publiserEksisterendeBehandlinger(behandlingRepository.findByRegistrertDatoIsGreaterThanEqual(
                eksisterendeBehandlingerFomDto.getFom()))
        );
    }

    /*
    Endepunkt som skal brukes én gang for å sende meldinger om vedtak med tilbakevirkende kraft (alle vedtak fattet
    siden Melosys gikk i produksjon). Skal fjernes etter bruk. Løsning for MELOSYS-4180.
     */
    @PostMapping("/publiserMelding/eksirendeBehandlinger")
    public ResponseEntity<Map<String, Set<Long>>> publiserEksisterendeBehandlinger(
        @RequestHeader(API_KEY_HEADER) String apiKey,
        @RequestBody Set<Long> behandlinger
    ) throws SikkerhetsbegrensningException {
        validerApikey(apiKey);

        List<Behandling> eksisterendeBehandlinger = new ArrayList<>();
        if (behandlinger == null || behandlinger.isEmpty()) {
            behandlingRepository.findAll().iterator().forEachRemaining(eksisterendeBehandlinger::add);
        } else {
            behandlingRepository.findAllById(behandlinger).iterator().forEachRemaining(eksisterendeBehandlinger::add);
        }
        log.info("Hentet {} behandlinger", eksisterendeBehandlinger.size());

        return ResponseEntity.ok(publiserEksisterendeBehandlinger(eksisterendeBehandlinger));
    }

    private Map<String, Set<Long>> publiserEksisterendeBehandlinger(List<Behandling> eksisterendeBehandlinger) {
        Set<Long> sendteBehandlinger = new HashSet<>();
        Set<Long> feiledeBehandlinger = new HashSet<>();
        for (Behandling behandling : eksisterendeBehandlinger) {
            try {
                utstedtA1Service.sendMeldingOmUtstedtA1(behandling.getId());
                sendteBehandlinger.add(behandling.getId());
            } catch (TekniskException | FunksjonellException e) {
                feiledeBehandlinger.add(behandling.getId());
                log.error("Melding om utstedt A1 for behandling {} ble ikke sendt", behandling.getId());
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
