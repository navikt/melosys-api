package no.nav.melosys.service.kodeverk;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import no.nav.melosys.integrasjon.kodeverk.Kode;
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Klassen tilbyr tjenester for å slå opp kodeverk.
 * Merk: Klassen casher oppslag mot felles-kodevek, og er derfor egentlig ikke stateless (men den er trådsikker).
 */
@Service
public class KodeverkService {

    private static final long MILLIS_MELLOM_VÅKNE_OPP = 3600000;

    private static final long KLOKKESLETT_FOR_CACHE_REFRESH = 6;

    private static final Logger log = LoggerFactory.getLogger(KodeverkService.class);

    private Map<String, no.nav.melosys.integrasjon.kodeverk.Kodeverk> kodeverkCache; // Ikke aksesser denne usynkronisert med mindre du vet hva du gjør

    private KodeverkRegister kodeverkRegister;

    private TømCacheScheduler tømCacheScheduler;


    @Autowired
    public KodeverkService(KodeverkRegister kodeverkRegister) {
        this.kodeverkRegister = kodeverkRegister;
        this.kodeverkCache = new HashMap<>();
        tømCacheScheduler = new TømCacheScheduler();
        tømCacheScheduler.start();
    }

    /**
     * Henter alle gyldige verdier for et kodeverk på et gitt tidspunkt.
     */
    public List<String> gyldigeVerdier(Kodeverk kodeverk, LocalDate dato) {
        Map<String, List<Kode>> koder = hentKodeverk(kodeverk.getNavn()).getKoder();
        List<String> res = new ArrayList<>(koder.size());
        for (List<Kode> kodeperioder : koder.values()) {
            // Kodeperioder er en liste med samme kode men med forskjellige gyldighetsperiode. Det holder at en er gyldig.
            for (Kode kandidat : kodeperioder) {
                if (!kandidat.getGyldigFom().isAfter(dato) && !kandidat.getGyldigTom().isBefore(dato)) {
                    res.add(kandidat.getKode());
                    break; // Inner
                }
            }
        }
        return res;
    }

    /**
     * Henter verdien for en kode i et kodeverk på en gitt dato, eller null hvis koden ikke er omfattet av kodeverket på angitt dato.
     */
    public String dekod(Kodeverk kodeverk, String kode, LocalDate dato) {
        List<Kode> kodeperioder = hentKodeverk(kodeverk.getNavn()).getKoder().get(kode);
        if (kodeperioder == null) {
            throw new RuntimeException("Problemer med å slå opp Kodeverk med navn '" + kodeverk.getNavn() + "'");
        }
        // kodeperioder er en liste med samme kode men med forskjellige gyldighetsperiode. Det holder at en er gyldig.
        for (Kode kandidat : kodeperioder) {
            if (!kandidat.getGyldigFom().isAfter(dato) && !kandidat.getGyldigTom().isBefore(dato)) {
                return kandidat.getNavn();
            }
        }
        return null;
    }

    /*
     * Merk: Vi har en forsvinnende liten mulighet for feil hvis dette ikke synkroniseres (også hvis vi aldri tømmer cache). 
     * Problemer kan oppstå hvis metoden kalles i parallell der det ene kallet medfører put og resize/rehash.
     * NB! Ikke gjør "smarte" ting her (som f.eks. delvis synkronisering) med mindre du vet om alle konsekvensene.
     */
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

    /**
     * Denne methoden tømmer cache og henter kodeverk på nytt
     */
    private synchronized void henteAlleKodeVerkData() {
        log.info("Tømmer cache og henter Kodeverk på nytt");
        kodeverkCache.clear();
        for (Kodeverk kodeverk : Kodeverk.values()) {
            hentKodeverk(kodeverk.getNavn());
        }
    }

    //Obs: Hvis applikasjon starter rett før kl 5 da risikerer man at tømm cache og hente ny Kodeverk blir ikke kallet
    private class TømCacheScheduler extends Thread {
        @Override
        public void run() {
            henteAlleKodeVerkData(); // Hente Kodeverk når applikasjon starter
            for (;;) {
                if (KLOKKESLETT_FOR_CACHE_REFRESH == LocalTime.now().getHour()) { // Tømme cache og hente Kodeverk på nytt hvertdag kl 06:00
                    henteAlleKodeVerkData();
                }
                try {
                    sleep(MILLIS_MELLOM_VÅKNE_OPP);
                } catch (InterruptedException e) {
                    return;
                }

            }
        }
    }

}
