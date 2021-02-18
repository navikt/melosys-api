package no.nav.melosys.saksflyt.kontroll;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.impl.SaksflytAsyncDelegate;
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

    private final SaksflytAsyncDelegate saksflytAsyncDelegate;
    private final ProsessinstansRepository prosessinstansRepository;
    private final String apiKey;

    public ProsessinstansAdminTjeneste(SaksflytAsyncDelegate saksflytAsyncDelegate,
                                       ProsessinstansRepository prosessinstansRepository,
                                       @Value("${Melosys-admin.apikey}") String apiKey) {
        this.saksflytAsyncDelegate = saksflytAsyncDelegate;
        this.prosessinstansRepository = prosessinstansRepository;
        this.apiKey = apiKey;
    }

    @GetMapping("/feilede")
    public ResponseEntity<List<HentProsessinstansDto>> hentFeiledeProsessinstanser(
        @RequestHeader(API_KEY_HEADER) String apiKey) throws SikkerhetsbegrensningException {

        validerApikey(apiKey);
        return ResponseEntity.ok(prosessinstansRepository.findAllByStatus(ProsessStatus.FEILET).stream().map(
            HentProsessinstansDto::new).collect(Collectors.toList()));
    }

    @PostMapping("/feilede/restart")
    public ResponseEntity<List<HentProsessinstansDto>> restartAlleFeiledeProsessinstanser(
        @RequestHeader(API_KEY_HEADER) String apiKey) throws SikkerhetsbegrensningException {
        validerApikey(apiKey);
        Collection<Prosessinstans> prosessinstanser = prosessinstansRepository.findAllByStatus(ProsessStatus.FEILET);
        prosessinstanser.forEach(p -> p.setStatus(ProsessStatus.RESTARTET));

        prosessinstansRepository
            .saveAll(prosessinstanser)
            .forEach(saksflytAsyncDelegate::behandleProsessinstans);
        return ResponseEntity.ok(
            prosessinstanser.stream().map(HentProsessinstansDto::new).collect(Collectors.toList()));
    }

    @PostMapping("/restart")
    public ResponseEntity<Void> restartProsessinstans(@RequestHeader(API_KEY_HEADER) String apiKey,
                                                      @RequestBody RestartProsessinstanserRequest request) throws FunksjonellException {
        validerApikey(apiKey);

        log.info("Forsøker å restarte prosessinstanser {}", request.getUuids());
        Collection<Prosessinstans> prosessinstanser = prosessinstansRepository.findAllById(request.getUuids());

        for (var prosessinstans : prosessinstanser) {
            if (prosessinstans.getStatus() != ProsessStatus.FEILET) {
                throw new FunksjonellException("Prosessinstans " + prosessinstans.getId() + " har status " + prosessinstans.getStatus());
            }
        }

        prosessinstanser.forEach(p -> p.setStatus(ProsessStatus.RESTARTET));

        prosessinstansRepository
            .saveAll(prosessinstanser)
            .forEach(saksflytAsyncDelegate::behandleProsessinstans);

        return ResponseEntity.ok().build();
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }
}
