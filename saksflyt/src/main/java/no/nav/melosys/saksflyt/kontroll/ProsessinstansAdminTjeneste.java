package no.nav.melosys.saksflyt.kontroll;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.impl.SaksflytAsyncDelegate;
import no.nav.melosys.saksflyt.kontroll.dto.HentProsessinstansDto;
import no.nav.melosys.saksflyt.kontroll.dto.RestartProsessinstanserRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/prosessinstanser")
public class ProsessinstansAdminTjeneste {

    private final Logger log = LoggerFactory.getLogger(ProsessinstansAdminTjeneste.class);

    private final SaksflytAsyncDelegate saksflytAsyncDelegate;
    private final ProsessinstansRepository prosessinstansRepository;

    public ProsessinstansAdminTjeneste(SaksflytAsyncDelegate saksflytAsyncDelegate, ProsessinstansRepository prosessinstansRepository) {
        this.saksflytAsyncDelegate = saksflytAsyncDelegate;
        this.prosessinstansRepository = prosessinstansRepository;
    }

    @GetMapping("/feilede")
    public ResponseEntity<List<HentProsessinstansDto>> hentFeiledeProsessinstanser() {
        return ResponseEntity.ok(
            prosessinstansRepository.findAllByStatus(ProsessStatus.FEILET).stream()
            .map(HentProsessinstansDto::new)
            .collect(Collectors.toList())
        );
    }

    @PostMapping("/restart")
    public ResponseEntity<Void> restartProsessinstans(@RequestBody RestartProsessinstanserRequest request) throws FunksjonellException {
        log.info("Restarter prosessinstanser {}", request.getUuids());
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
}
