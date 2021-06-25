package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import no.nav.melosys.integrasjon.kodeverk.Kode;
import no.nav.melosys.integrasjon.kodeverk.Kodeverk;
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KodeOppslagFraKodeVerk implements KodeOppslag {
    private final KodeverkRegister kodeverkRegister;

    ConcurrentHashMap<String, KodeHolder> map = new ConcurrentHashMap<>();

    public KodeOppslagFraKodeVerk(KodeverkRegister kodeverkRegister) {
        this.kodeverkRegister = kodeverkRegister;
    }

    @Override
    public String getTerm(String kodeverk, String kode) {
        return getKodeverk(kodeverk).getTerm(kode);
}

    private KodeHolder getKodeverk(String kodeverkName) {
        if (map.contains(kodeverkName)) {
            return map.get(kodeverkName);
        }
        Kodeverk Kodeverk = kodeverkRegister.hentKodeverk(kodeverkName);
        KodeHolder kodeHolder = new KodeHolder(Kodeverk);
        map.put(kodeverkName, kodeHolder);
        return kodeHolder;
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
