package no.nav.melosys.domain.dokument.felles;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonValue;

public class Land {
    public static final String BELGIA = "BEL";
    public static final String BULGARIA = "BGR";
    public static final String DANMARK = "DNK";
    public static final String TSJEKKIA = "CZE";
    public static final String ESTLAND = "EST";
    public static final String FINLAND = "FIN";
    public static final String FRANKRIKE = "FRA";
    public static final String FÆRØYENE = "FRO";
    public static final String GRØNLAND = "GRL";
    public static final String HELLAS = "GRC";
    public static final String IRLAND = "IRL";
    public static final String ISLAND = "ISL";
    public static final String ITALIA = "ITA";
    public static final String KROATIA = "HRV";
    public static final String KYPROS = "CYP";
    public static final String LATVIA = "LVA";
    public static final String LIECHTENSTEIN = "LIE";
    public static final String LITAUEN = "LTU";
    public static final String LUXEMBOURG = "LUX";
    public static final String MALTA = "MLT";
    public static final String NEDERLAND = "NLD";
    public static final String NORGE = "NOR";
    public static final String POLEN = "POL";
    public static final String PORTUGAL = "PRT";
    public static final String ROMANIA = "ROU";
    public static final String SLOVAKIA = "SVK";
    public static final String SLOVENIA = "SVN";
    public static final String SPANIA = "ESP";
    public static final String STATSLØS = "XXX";
    public static final String STORBRITANNIA = "GBR";
    public static final String SVALBARD_OG_JAN_MAYEN = "SJM";
    public static final String SVEITS = "CHE";
    public static final String SVERIGE = "SWE";
    public static final String TYSKLAND = "DEU";
    public static final String UNGARN = "HUN";
    public static final String ØSTERRIKE = "AUT";
    public static final String ÅLAND = "ALA";

    // FIXME: Sjekk om/hvordan vi skal håndtere Sveits og Svalbard
    private static final Set<String> EØS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            BELGIA, BULGARIA, DANMARK, ESTLAND, FINLAND, FRANKRIKE, HELLAS, IRLAND, ISLAND, ITALIA, KROATIA, KYPROS,
            LATVIA, LIECHTENSTEIN, LITAUEN, LUXEMBOURG, MALTA, NEDERLAND, NORGE, POLEN, PORTUGAL, ROMANIA, SLOVAKIA,
            SLOVENIA, SPANIA, STORBRITANNIA, SVERIGE, TSJEKKIA, TYSKLAND, UNGARN, ØSTERRIKE)));
    // FIXME: Sjekk om Åland skal være med
    private static final Set<String> NORDEN = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            DANMARK, FINLAND, FÆRØYENE, GRØNLAND, ISLAND, NORGE, SVALBARD_OG_JAN_MAYEN, SVERIGE, ÅLAND)));

    private String kode;

    // Brukes av JAXB
    public Land() {}

    public Land(String landkode) {
        this.kode = landkode;
    }

    @JsonValue
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
        return STATSLØS.equals(kode);
    }

    public boolean erNorden() {
        return NORDEN.contains(kode);
    }
}
