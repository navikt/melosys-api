package no.nav.melosys.service.kodeverk;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.integrasjon.kodeverk.Kode;
import no.nav.melosys.integrasjon.kodeverk.Kodeverk;
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
public class KodeverkService {
    private static final Logger log = LoggerFactory.getLogger(KodeverkService.class);

    public static final String UKJENT = "UKJENT";

    private final KodeverkRegister kodeverkRegister;

    public KodeverkService(KodeverkRegister kodeverkRegister) {
        this.kodeverkRegister = kodeverkRegister;
    }

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        kodeverkScheduler();
    }

    public String dekod(FellesKodeverk kodeverk, String kode) {
        return dekod(kodeverk, kode, LocalDate.now());
    }

    private String dekod(FellesKodeverk kodeverk, String kode, LocalDate dato) {
        if (ObjectUtils.isEmpty(kode)) {
            log.warn("Metode dekod kalt for kodeverk {} med kode {}", kodeverk, kode);
            return UKJENT;
        }

        List<Kode> kodeperioder = hentKodeverk(kodeverk.getNavn()).getKoder().get(kode);
        return getTermFraKodeverk(kodeverk, kode, dato, kodeperioder);
    }

    public List<Kode> hentGyldigeKoderForKodeverk(FellesKodeverk kodeverk) {
        if (ObjectUtils.isEmpty(kodeverk)) {
            log.error("Metode hentGyldigeKoderForKodeverk kalt for kodeverk {}", kodeverk);
            return Collections.emptyList();
        }

        Kodeverk hentetKodeverk = hentKodeverk(kodeverk.getNavn());

        if (hentetKodeverk == null) {
            log.warn("Fant ikke kodeverk {}", kodeverk.getNavn());
            return Collections.emptyList();
        }

        List<Kode> gyldigeKoder = new ArrayList<>();
        LocalDate idag = LocalDate.now();

        for (Map.Entry<String, List<Kode>> entry : hentetKodeverk.getKoder().entrySet()) {
            entry.getValue().stream().filter(kode -> !kode.getGyldigFom().isAfter(idag) && !kode.getGyldigTom().isBefore(idag)).findFirst().ifPresent(gyldigeKoder::add);
        }
        return gyldigeKoder;
    }

    private Kodeverk hentKodeverk(String kodeverkNavn) {
        log.debug("Hentet og cachet Kodeverk {}", kodeverkNavn);
        return kodeverkRegister.hentKodeverk(kodeverkNavn);
    }

    public String getTermFraKodeverk(FellesKodeverk kodeverk, String kode) {
        return getTermFraKodeverk(kodeverk, kode, LocalDate.now(), kodeverkRegister.hentKodeverk(kodeverk.getNavn()).getKoder().get(kode));
    }

    private String getTermFraKodeverk(FellesKodeverk kodeverk, String kode, LocalDate dato, List<Kode> kodeperioder) {
        if (kodeperioder == null) {
            log.warn("Fant ikke term for kode {} i kodeverk {}", kode, kodeverk.getNavn());
            return UKJENT;
        }
        // kodeperioder er en liste med samme kode men med forskjellige gyldighetsperiode. Det holder at en er gyldig.
        for (Kode kodeperiode : kodeperioder) {
            if (!kodeperiode.getGyldigFom().isAfter(dato) && !kodeperiode.getGyldigTom().isBefore(dato)) {
                return kodeperiode.getNavn();
            }
        }
        log.warn("Fant ingen gyldig term for kode {} i kodeverk {}", kode, kodeverk.getNavn());
        return UKJENT;
    }

    @Scheduled(cron = "0 0 6 * * *")
    @SchedulerLock(name = "KodeverkSchedulerJobb", lockAtLeastFor = "10m")
    public void kodeverkScheduler() {
        log.info("Henter alle kodeverk");
        for (FellesKodeverk kodeverk : FellesKodeverk.values()) {
            hentKodeverk(kodeverk.getNavn());
        }
    }

}
