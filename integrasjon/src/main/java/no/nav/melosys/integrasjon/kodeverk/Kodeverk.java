package no.nav.melosys.integrasjon.kodeverk;

import java.util.List;
import java.util.Map;

/**
 * Ett kodeverk, internt representert som en av der kode gir en liste med en eller flere navn (en for hver gyldighetsperiode)
 */
public class Kodeverk {
    
    private String navn;

    private Map<String, List<Kode>> koder;
    
    public Kodeverk(String navn, Map<String, List<Kode>> koder) {
        this.navn = navn;
        this.koder = koder;
    }

    public String getNavn() {
        return navn;
    }

    public Map<String, List<Kode>> getKoder() {
        return koder;
    }

}
