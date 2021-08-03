package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import no.nav.melosys.integrasjon.kodeverk.Kode;
import no.nav.melosys.integrasjon.kodeverk.Kodeverk;
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KodeOppslagFraKodeverk implements KodeOppslag {
    private final KodeverkRegister kodeverkRegister;

    public KodeOppslagFraKodeverk(KodeverkRegister kodeverkRegister) {
        this.kodeverkRegister = kodeverkRegister;
    }

    @Override
    public String getTerm(String kodeverk, String kode) {
        return getKodeverk(kodeverk).getTerm(kode);
    }

    // Diskutert med Andreas og det blir en egen PR på en bedre løsning her
    // Bør bli en felles løsning som også kan brukes av KodeverkService så den ikke gjør cahing selv
    @Cacheable("kodeverk")
    public KodeHolder getKodeverk(String kodeverkName) {
        Kodeverk Kodeverk = kodeverkRegister.hentKodeverk(kodeverkName);
        return new KodeHolder(Kodeverk);
    }

    private static class KodeHolder {
        private final Map<String, List<Kode>> koder;

        private KodeHolder(Kodeverk kodeverk) {
            this.koder = kodeverk.getKoder();
        }

        public String getTerm(String kode) {
            List<Kode> kodes = koder.get(kode);
            // hvordan det være flere navn for kode?
            // Er det greit og returnere første?
            return kodes.get(0).getNavn();
        }
    }
}
