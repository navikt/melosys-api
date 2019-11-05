package no.nav.melosys.saksflyt.kontroll;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SaksflytKontroll {

    private static final Logger log = LoggerFactory.getLogger(SaksflytKontroll.class);

    private final Binge binge;
    private final ProsessinstansRepository prosessinstansRepository;

    public SaksflytKontroll(Binge binge, ProsessinstansRepository prosessinstansRepository) {
        this.binge = binge;
        this.prosessinstansRepository = prosessinstansRepository;
    }

    @Scheduled(fixedRateString = "${melosys.saksflyt.kontroll.intervall}",
        initialDelayString = "${melosys.saksflyt.kontroll.intervall}")
    public void sjekkProsessinstansFinnesISaksflyt() {
        log.debug("Kjører kontroll for prosessinstanser");
        Map<UUID, Prosessinstans> prosessinstanser = prosessinstansRepository.findAllByStegIsNotAndStegIsNot(ProsessSteg.FERDIG, ProsessSteg.FEILET_MASKINELT)
            .stream().collect(Collectors.toMap(Prosessinstans::getId, p -> p));

        binge.hentProsessinstanser().forEach(p -> prosessinstanser.remove(p.getId()));

        prosessinstanser.values().forEach(prosessinstans -> {
            log.info("Prosessinstans {} lagt inn i binge etter kontroll", prosessinstans.getId());
            binge.leggTil(prosessinstans);
        });
    }
}
