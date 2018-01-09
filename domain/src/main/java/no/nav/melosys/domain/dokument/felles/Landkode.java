package no.nav.melosys.domain.dokument.felles;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Landkode {
    private final String BELGIA = "BEL";
    private final String BULGARIA = "BGR";
    private final String DANMARK = "DNK";
    private final String TSJEKKIA = "CZE";
    private final String ESTLAND = "EST";
    private final String FINLAND = "FIN";
    private final String FRANKRIKE = "FRA";
    private final String FÆRØYENE = "FRO";
    private final String GRØNLAND = "GRL";
    private final String HELLAS = "GRC";
    private final String IRLAND = "IRL";
    private final String ISLAND = "ISL";
    private final String ITALIA = "ITA";
    private final String KROATIA = "HRV";
    private final String KYPROS = "CYP";
    private final String LATVIA = "LVA";
    private final String LIECHTENSTEIN = "LIE";
    private final String LITAUEN = "LTU";
    private final String LUXEMBOURG = "LUX";
    private final String MALTA = "MLT";
    private final String NEDERLAND = "NLD";
    private final String NORGE = "NOR";
    private final String POLEN = "POL";
    private final String PORTUGAL = "PRT";
    private final String ROMANIA = "ROU";
    private final String SLOVAKIA = "SVK";
    private final String SLOVENIA = "SVN";
    private final String SPANIA = "ESP";
    private final String STORBRITANNIA = "GBR";
    private final String SVEITS = "SWZ";
    private final String SVERIGE = "SWE";
    private final String TYSKLAND = "DEU";
    private final String UNGARN = "HUN";
    private final String ØSTERRIKE = "AUT";

    // TODO: Sjekk om/hvordan vi skal håndtere Sveits og Svalbard
    private final Set<String> EØS = new HashSet<>(Arrays.asList(ISLAND, LIECHTENSTEIN, NORGE));
    private final Set<String> NORDEN_UTEN_NORGE = new HashSet<>(Arrays.asList(
            DANMARK, FINLAND, FÆRØYENE, GRØNLAND, ISLAND, SVERIGE));
    private final Set<String> EU = new HashSet<>(Arrays.asList(
            BELGIA, BULGARIA, DANMARK, ESTLAND, FINLAND, FRANKRIKE, HELLAS, IRLAND, ITALIA, KROATIA, KYPROS,
            LATVIA, LITAUEN, LUXEMBOURG, MALTA, NEDERLAND, POLEN, PORTUGAL, ROMANIA, SLOVAKIA, SLOVENIA, SPANIA,
            STORBRITANNIA, SVERIGE, TSJEKKIA, TYSKLAND, UNGARN, ØSTERRIKE));

    private String kode;

    // Brukes av JAXB
    public Landkode() {}

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
        return NORDEN_UTEN_NORGE.contains(kode);
    }

    public boolean erNederland() {
        return NEDERLAND.equals(kode);
    }

    public boolean erLuxembourg() {
        return LUXEMBOURG.equals(kode);
    }

    public boolean erTredjeland() {
        return kode != null && !EU.contains(kode) && !EØS.contains(kode);
    }
}
