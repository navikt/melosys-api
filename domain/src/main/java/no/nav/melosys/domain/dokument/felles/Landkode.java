package no.nav.melosys.domain.dokument.felles;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Landkode {
    final String ISLAND = "ISL";
    final String LIECHTENSTEIN = "LIE";
    final String LUXEMBOURG = "LUX";
    final String NEDERLAND = "NLD";
    final String NORGE = "NOR";
    final String SVEITS = "SWZ";

    // TODO: Sjekk om vi skal ta hensyn til Sveits og Svalbard
    final Set<String> EØS = new HashSet<>(Arrays.asList(ISLAND, LIECHTENSTEIN, NORGE));

    private String kode;

    // Brukes av JAXB
    public Landkode() {
    }

    public Landkode(String landkode) {
        this.kode = landkode;
    }

    public String getKode() {
        return kode;
    }

    public void setKode(String kode) {
        this.kode = kode;
    }

    public boolean erEØS() {
        return EØS.contains(kode);
    }

    public boolean erSveits() {
        return SVEITS.equals(kode);
    }

    public boolean erStatsløs() {
        return kode == null;
    }

    public boolean erDekketAvNordiskKonvensjonOmTrygd() {
        return true;
    }

    public boolean erNederland() {
        return NEDERLAND.equals(kode);
    }

    public boolean erLuxembourg() {
        return NEDERLAND.equals(kode);
    }
}
