package no.nav.melosys.saksflyt.kontroll;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import no.nav.melosys.domain.saksflyt.ProsessStatus;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.ProsessinstansKø;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SaksflytKontroll {

    private static final Logger log = LoggerFactory.getLogger(SaksflytKontroll.class);

    private static final long SYV_MINUTTER = 420000;
    private static final long FEM_MINUTTER = 300000;

    private final ProsessinstansKø prosessinstansKø;
    private final ProsessinstansRepository prosessinstansRepository;

    public SaksflytKontroll(ProsessinstansKø prosessinstansKø, ProsessinstansRepository prosessinstansRepository) {
        this.prosessinstansKø = prosessinstansKø;
        this.prosessinstansRepository = prosessinstansRepository;
    }

    @Scheduled(
        fixedRate = SYV_MINUTTER,
        initialDelay = FEM_MINUTTER
    )
    public void sjekkProsessinstansFinnesISaksflyt() {
        log.debug("Kjører kontroll for prosessinstanser");
        Map<UUID, Prosessinstans> prosessinstanser = prosessinstansRepository.findAllByStatus(ProsessStatus.KLAR)
            .stream().collect(Collectors.toMap(Prosessinstans::getId, p -> p));

        prosessinstansKø.hentProsessinstanser().forEach(p -> prosessinstanser.remove(p.getId()));

        prosessinstanser.values().forEach(prosessinstans -> {
            log.info("Prosessinstans {} lagt inn i binge etter kontroll", prosessinstans.getId());
            prosessinstansKø.leggTil(prosessinstans);
        });
    }
}
