package no.nav.melosys.saksflyt.kontroll;

import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.impl.SaksflytAsyncArbeider;
import no.nav.security.token.support.core.api.Unprotected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Unprotected
@RestController
@RequestMapping("/internal")
public class SaksflytKontrollRestService {

    private final Logger log = LoggerFactory.getLogger(SaksflytKontrollRestService.class);

    private final SaksflytAsyncArbeider saksflytAsyncArbeider;
    private final ProsessinstansRepository prosessinstansRepository;

    public SaksflytKontrollRestService(SaksflytAsyncArbeider saksflytAsyncArbeider, ProsessinstansRepository prosessinstansRepository) {
        this.saksflytAsyncArbeider = saksflytAsyncArbeider;
        this.prosessinstansRepository = prosessinstansRepository;
    }

    @GetMapping("/saksflyt/kontroll")
    public void kontrollerProsessinstanser() {
        prosessinstansRepository.findAllByStatus(ProsessStatus.RESTARTET)
            .forEach(this::restartProsessinstans);
    }

    private void restartProsessinstans(Prosessinstans prosessinstans) {
        log.info("Restarter prosessinstans {}", prosessinstans.getId());
        prosessinstans.setStatus(ProsessStatus.KLAR);
        saksflytAsyncArbeider.behandleProsessinstans(
            prosessinstansRepository.save(prosessinstans)
        );
    }
}
