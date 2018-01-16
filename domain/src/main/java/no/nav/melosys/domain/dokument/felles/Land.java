package no.nav.melosys.domain.dokument.felles;

import java.util.*;

public class Land {
    private static final String BELGIA = "BEL";
    private static final String BULGARIA = "BGR";
    private static final String DANMARK = "DNK";
    private static final String TSJEKKIA = "CZE";
    private static final String ESTLAND = "EST";
    private static final String FINLAND = "FIN";
    private static final String FRANKRIKE = "FRA";
    private static final String FÆRØYENE = "FRO";
    private static final String GRØNLAND = "GRL";
    private static final String HELLAS = "GRC";
    private static final String IRLAND = "IRL";
    private static final String ISLAND = "ISL";
    private static final String ITALIA = "ITA";
    private static final String KROATIA = "HRV";
    private static final String KYPROS = "CYP";
    private static final String LATVIA = "LVA";
    private static final String LIECHTENSTEIN = "LIE";
    private static final String LITAUEN = "LTU";
    private static final String LUXEMBOURG = "LUX";
    private static final String MALTA = "MLT";
    private static final String NEDERLAND = "NLD";
    private static final String NORGE = "NOR";
    private static final String POLEN = "POL";
    private static final String PORTUGAL = "PRT";
    private static final String ROMANIA = "ROU";
    private static final String SLOVAKIA = "SVK";
    private static final String SLOVENIA = "SVN";
    private static final String SPANIA = "ESP";
    private static final String STATSLØS = "XXX";
    private static final String STORBRITANNIA = "GBR";
    private static final String SVALBARD_OG_JAN_MAYEN = "SJM";
    private static final String SVEITS = "SWZ";
    private static final String SVERIGE = "SWE";
    private static final String TYSKLAND = "DEU";
    private static final String UNGARN = "HUN";
    private static final String ØSTERRIKE = "AUT";
    private static final String ÅLAND = "ALA";

    // TODO: Sjekk om/hvordan vi skal håndtere Sveits og Svalbard
    private static final Set<String> EØS = new HashSet<>(Arrays.asList(ISLAND, LIECHTENSTEIN, NORGE));
    private static final Set<String> EU = new HashSet<>(Arrays.asList(
            BELGIA, BULGARIA, DANMARK, ESTLAND, FINLAND, FRANKRIKE, HELLAS, IRLAND, ITALIA, KROATIA, KYPROS,
            LATVIA, LITAUEN, LUXEMBOURG, MALTA, NEDERLAND, POLEN, PORTUGAL, ROMANIA, SLOVAKIA, SLOVENIA, SPANIA,
            STORBRITANNIA, SVERIGE, TSJEKKIA, TYSKLAND, UNGARN, ØSTERRIKE));
    // TODO: Sjekk om Åland skal være med
    private static final Set<String> NORDEN = new HashSet<>(Arrays.asList(
            DANMARK, FINLAND, FÆRØYENE, GRØNLAND, ISLAND, NORGE, SVALBARD_OG_JAN_MAYEN, SVERIGE, ÅLAND));

    private String kode;

    // Brukes av JAXB
    public Land() {}

    public Land(String landkode) {
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

    public boolean erEU() {
        return EU.contains(kode);
    }

    public boolean erSveits() {
        return SVEITS.equals(kode);
    }

    public boolean erStatsløs() {
        return STATSLØS.equals(kode);
    }

    public boolean erNorden() {
        return NORDEN.contains(kode);
    }

    public boolean erNederland() {
        return NEDERLAND.equals(kode);
    }

    public boolean erLuxembourg() {
        return LUXEMBOURG.equals(kode);
    }
}
