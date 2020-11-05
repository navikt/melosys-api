package no.nav.melosys.statistikk.utstedt_a1.api;

import java.util.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.statistikk.utstedt_a1.integrasjon.dto.UtstedtA1Melding;
import no.nav.melosys.statistikk.utstedt_a1.service.UtstedtA1Service;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

@Protected
@RestController
@RequestMapping("/admin/utstedtA1")
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class UtstedtA1AdminTjeneste {
    private static final Logger log = LoggerFactory.getLogger(UtstedtA1AdminTjeneste.class);

    private final UtstedtA1Service utstedtA1Service;
    private final BehandlingRepository behandlingRepository;

    @Autowired
    public UtstedtA1AdminTjeneste(UtstedtA1Service utstedtA1Service, BehandlingRepository behandlingRepository) {
        this.utstedtA1Service = utstedtA1Service;
        this.behandlingRepository = behandlingRepository;
    }

    @PostMapping("/{behandlingID}/publiserMelding")
    public ResponseEntity<UtstedtA1Melding> publiser(@PathVariable long behandlingID) throws FunksjonellException, TekniskException {
        UtstedtA1Melding utstedtA1Melding = utstedtA1Service.sendMeldingOmUtstedtA1(behandlingID);
        return ResponseEntity.ok(utstedtA1Melding);
    }

    /*
    Endepunkt som skal brukes én gang for å sende meldinger om vedtak med tilbakevirkende kraft (alle vedtak fattet
    siden Melosys gikk i produksjon). Skal fjernes etter bruk. Løsning for MELOSYS-4180.
     */
    @PostMapping("/publiserMelding/eksirendeBehandlinger")
    public ResponseEntity<Map<String, Set<Long>>> publiserEksisterendeBehandlinger(@RequestBody Set<Long> behandlinger) {
        List<Behandling> eksisterendeBehandlinger = new ArrayList<>();
        if (behandlinger == null || behandlinger.isEmpty()) {
            behandlingRepository.findAll().iterator().forEachRemaining(eksisterendeBehandlinger::add);
        } else {
            behandlingRepository.findAllById(behandlinger).iterator().forEachRemaining(eksisterendeBehandlinger::add);
        }
        log.info("Hentet {} behandlinger", eksisterendeBehandlinger.size());

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
        return ResponseEntity.ok(Map.of(
            "sendteBehandlinger", sendteBehandlinger,
            "feiledeBehandlinger", feiledeBehandlinger));
    }
}
