package no.nav.melosys.service.kodeverk;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.integrasjon.kodeverk.Kode;
import no.nav.melosys.integrasjon.kodeverk.KodeOppslag;
import no.nav.melosys.integrasjon.kodeverk.KodeOppslagFraKodeverk;
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister;
import no.nav.melosys.integrasjon.kodeverk.impl.KodeverkRegisterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class KodeverkService {
    private static final Logger log = LoggerFactory.getLogger(KodeverkService.class);

    private static final long MILLIS_MELLOM_VÅKNE_OPP = 3600000;
    private static final long KLOKKESLETT_FOR_CACHE_REFRESH = 6;
    public static final String UKJENT = KodeOppslagFraKodeverk.UKJENT;


    private Map<String, no.nav.melosys.integrasjon.kodeverk.Kodeverk> kodeverkCache;
    private KodeverkRegister kodeverkRegister;
    private KodeOppslag kodeOppslag;

    public KodeverkService(KodeverkRegister kodeverkRegister, KodeOppslag kodeOppslag) {
        if(kodeverkRegister instanceof KodeverkRegisterImpl) {
            log.info("KodeOppslagFraKodeverk blir brukt");
        } else {
            log.info("---KodeverkRegister {}", kodeverkRegister.getClass().getSimpleName());
        }
        if(kodeOppslag instanceof KodeOppslagFraKodeverk) {
            log.info("KodeOppslagFraKodeverk blir brukt");
        } else {
            log.info("---KodeverkRegister {}", kodeverkRegister.getClass().getSimpleName());
        }

        this.kodeverkRegister = kodeverkRegister;
        this.kodeOppslag = kodeOppslag;
        this.kodeverkCache = new HashMap<>();
    }

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        new TømCacheScheduler().start();
    }

    public KodeDto getKodeverdi(FellesKodeverk kodeverk, String kode) {
        if (kode == null) {
            return null;
        }
        return new KodeDto(kode, dekod(kodeverk, kode));
    }

    public String dekod(FellesKodeverk kodeverk, String kode) {
        return dekod(kodeverk, kode, LocalDate.now());
    }

    private String dekod(FellesKodeverk kodeverk, String kode, LocalDate dato) {
        if (StringUtils.isEmpty(kode)) {
            log.warn("Metode dekod kalt for kodeverk {} med kode {}", kodeverk, kode);
            return UKJENT;
        }

        List<Kode> kodeperioder = hentKodeverk(kodeverk.getNavn()).getKoder().get(kode);
        return kodeOppslag.getTermFraKodeverk(kodeverk, kode, dato, kodeperioder);
    }

    public List<KodeDto> hentGyldigeKoderForKodeverk(FellesKodeverk kodeverk) {
        if (StringUtils.isEmpty(kodeverk)) {
            log.error("Metode hentGyldigeKoderForKodeverk kalt for kodeverk {}", kodeverk);
            return Collections.emptyList();
        }

        no.nav.melosys.integrasjon.kodeverk.Kodeverk hentetKodeverk = hentKodeverk(kodeverk.getNavn());

        if (hentetKodeverk == null) {
            log.warn("Fant ikke kodeverk {}", kodeverk.getNavn());
            return Collections.emptyList();
        }

        List<KodeDto> gyldigeKoder = new ArrayList<>();
        LocalDate idag = LocalDate.now();

        for (Map.Entry<String, List<Kode>> entry : hentetKodeverk.getKoder().entrySet()) {
            entry.getValue().stream().filter(kode -> !kode.getGyldigFom().isAfter(idag) && !kode.getGyldigTom().isBefore(idag)).findFirst().map(kode -> new KodeDto(kode.getKode(), kode.getNavn())).ifPresent(gyldigeKoder::add);
        }
        return gyldigeKoder;
    }

    private synchronized no.nav.melosys.integrasjon.kodeverk.Kodeverk hentKodeverk(String kodeverkNavn) {
        no.nav.melosys.integrasjon.kodeverk.Kodeverk kodeverk = kodeverkCache.get(kodeverkNavn);
        if (kodeverk != null) {
            return kodeverk;
        }
        kodeverk = kodeverkRegister.hentKodeverk(kodeverkNavn);
        kodeverkCache.put(kodeverkNavn, kodeverk);
        log.debug("Hentet og cachet Kodeverk {}", kodeverkNavn);
        return kodeverk;
    }


    private class TømCacheScheduler extends Thread {

        private synchronized void henteAlleKodeVerkData() {
            log.info("Tømmer cache og henter Kodeverk på nytt");
            kodeverkCache.clear();
            for (FellesKodeverk kodeverk : FellesKodeverk.values()) {
                hentKodeverk(kodeverk.getNavn());
            }
        }

        @Override
        public void run() {
            henteAlleKodeVerkData();
            for (; ; ) {
                if (KLOKKESLETT_FOR_CACHE_REFRESH == LocalTime.now().getHour()) { // Tømme cache og hente Kodeverk på nytt hvertdag kl 06:00
                    henteAlleKodeVerkData();
                }
                try {
                    sleep(MILLIS_MELLOM_VÅKNE_OPP);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

            }
        }
    }

}
