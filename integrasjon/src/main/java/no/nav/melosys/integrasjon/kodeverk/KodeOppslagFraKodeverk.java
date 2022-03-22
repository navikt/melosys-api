package no.nav.melosys.integrasjon.kodeverk;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.FellesKodeverk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class KodeOppslagFraKodeverk implements KodeOppslag {
    private static final Logger log = LoggerFactory.getLogger(KodeOppslagFraKodeverk.class);

    private final KodeverkRegister kodeverkRegister;

    public static final String UKJENT = "UKJENT";

    public KodeOppslagFraKodeverk(KodeverkRegister kodeverkRegister) {
        this.kodeverkRegister = kodeverkRegister;
    }

    @Override
    public String getTermFraKodeverk(FellesKodeverk kodeverk, String kode) {
        return getTermFraKodeverk(kodeverk, kode, LocalDate.now());
    }

    @Override
    public String getTermFraKodeverk(FellesKodeverk kodeverk, String kode, LocalDate dato) {
        return getTermFraKodeverk(kodeverk, kode, dato, getKodeverk(kodeverk.getNavn()).getKoder().get(kode));
    }

    @Override
    public String getTermFraKodeverk(FellesKodeverk kodeverk, String kode, LocalDate dato, List<Kode> kodeperioder) {
        if (kodeperioder == null) {
            log.warn("Fant ikke term for kode {} i kodeverk {}", kode, kodeverk.getNavn());
            return UKJENT;
        }
        // kodeperioder er en liste med samme kode men med forskjellige gyldighetsperiode. Det holder at en er gyldig.
        for (Kode kandidat : kodeperioder) {
            if (!kandidat.getGyldigFom().isAfter(dato) && !kandidat.getGyldigTom().isBefore(dato)) {
                return kandidat.getNavn();
            }
        }
        log.warn("Fant ingen gyldig term for kode {} i kodeverk {}", kode, kodeverk.getNavn());
        return UKJENT;
    }

    // Diskutert med Andreas og det blir en egen PR på en bedre løsning her
    // Bør bli en felles løsning som også kan brukes av KodeverkService så den ikke gjør cahing selv
    @Cacheable("kodeverk")
    public Kodeverk getKodeverk(String kodeverkName) {
        return kodeverkRegister.hentKodeverk(kodeverkName);
    }
}
