package no.nav.melosys.service.kodeverk;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.integrasjon.kodeverk.Kode;
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        return getTermFraKodeverk(kodeverk, kode, dato, kodeverkRegister.hentKodeverk(kodeverk.getNavn()).getKoder().get(kode));
    }

    @Override
    public String getTermFraKodeverk(FellesKodeverk kodeverk, String kode, LocalDate dato, List<Kode> kodeperioder) {
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
}
