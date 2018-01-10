package no.nav.melosys.domain.dokument.felles;

import com.fasterxml.jackson.annotation.JsonCreator;
import no.nav.melosys.domain.dokument.KodeverkEnum;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;
import java.util.*;

@XmlType(name = "Land")
@XmlEnum
public enum Land implements KodeverkEnum<Land> {
    // TODO: Bytt om på kode og navn slik at mapping fra XML og JSON fungerer uten videre
    @XmlEnumValue("BEL")
    BELGIA("BEL"),
    @XmlEnumValue("BGR")
    BULGARIA("BGR"),
    @XmlEnumValue("DNK")
    DANMARK("DNK"),
    @XmlEnumValue("CZE")
    TSJEKKIA("CZE"),
    @XmlEnumValue("EST")
    ESTLAND("EST"),
    @XmlEnumValue("FIN")
    FINLAND("FIN"),
    @XmlEnumValue("FRA")
    FRANKRIKE("FRA"),
    @XmlEnumValue("FRO")
    FÆRØYENE("FRO"),
    @XmlEnumValue("GRL")
    GRØNLAND("GRL"),
    @XmlEnumValue("GRC")
    HELLAS("GRC"),
    @XmlEnumValue("IRL")
    IRLAND("IRL"),
    @XmlEnumValue("ISL")
    ISLAND("ISL"),
    @XmlEnumValue("ITA")
    ITALIA("ITA"),
    @XmlEnumValue("HRV")
    KROATIA("HRV"),
    @XmlEnumValue("CYP")
    KYPROS("CYP"),
    @XmlEnumValue("LVA")
    LATVIA("LVA"),
    @XmlEnumValue("LIE")
    LIECHTENSTEIN("LIE"),
    @XmlEnumValue("LTU")
    LITAUEN("LTU"),
    @XmlEnumValue("LUX")
    LUXEMBOURG("LUX"),
    @XmlEnumValue("MLT")
    MALTA("MLT"),
    @XmlEnumValue("NLD")
    NEDERLAND("NLD"),
    @XmlEnumValue("NOR")
    NORGE("NOR"),
    @XmlEnumValue("POL")
    POLEN("POL"),
    @XmlEnumValue("PRT")
    PORTUGAL("PRT"),
    @XmlEnumValue("ROU")
    ROMANIA("ROU"),
    @XmlEnumValue("SVK")
    SLOVAKIA("SVK"),
    @XmlEnumValue("SVN")
    SLOVENIA("SVN"),
    @XmlEnumValue("ESP")
    SPANIA("ESP"),
    @XmlEnumValue("GBR")
    STORBRITANNIA("GBR"),
    @XmlEnumValue("SWZ")
    SVEITS("SWZ"),
    @XmlEnumValue("SWE")
    SVERIGE("SWE"),
    @XmlEnumValue("DEU")
    TYSKLAND("DEU"),
    @XmlEnumValue("HUN")
    UNGARN("HUN"),
    @XmlEnumValue("AUT")
    ØSTERRIKE("AUT"),
    UKJENT(null);

    // TODO: Sjekk om/hvordan vi skal håndtere Sveits og Svalbard
    private static final Set<Land> EØS = new HashSet<>(Arrays.asList(ISLAND, LIECHTENSTEIN, NORGE));
    private static final Set<Land> EU = new HashSet<>(Arrays.asList(
            BELGIA, BULGARIA, DANMARK, ESTLAND, FINLAND, FRANKRIKE, HELLAS, IRLAND, ITALIA, KROATIA, KYPROS,
            LATVIA, LITAUEN, LUXEMBOURG, MALTA, NEDERLAND, POLEN, PORTUGAL, ROMANIA, SLOVAKIA, SLOVENIA, SPANIA,
            STORBRITANNIA, SVERIGE, TSJEKKIA, TYSKLAND, UNGARN, ØSTERRIKE));
    // TODO: Sjekk om Åland skal være med
    private static final Set<Land> NORDEN_UTEN_NORGE = new HashSet<>(Arrays.asList(
            DANMARK, FINLAND, FÆRØYENE, GRØNLAND, ISLAND, SVERIGE));

    private String kode;

    // Brukes av JAXB
    Land() {}

    Land(String kode) {
        this.kode = kode;
    }

    @JsonCreator
    public static Land getLand(String kode) {
        for (Land land : values()) {
            if (kode.equals(land.kode)) {
                return land;
            }
        }
        return UKJENT;
    }

    @Override
    public String getNavn() {
        return kode;
    }

    public boolean erEØS() {
        return EØS.contains(this);
    }

    public boolean erEU() {
        return EU.contains(this);
    }

    public boolean erSveits() {
        return SVEITS.kode.equals(kode);
    }

    public boolean erStatsløs() {
        return kode == null;
    }

    public boolean erNordenUtenNorge() {
        return NORDEN_UTEN_NORGE.contains(this);
    }

    public boolean erNederland() {
        return NEDERLAND.kode.equals(kode);
    }

    public boolean erLuxembourg() {
        return LUXEMBOURG.kode.equals(kode);
    }

    public boolean erTredjeland() {
        return kode != null && !erEU() && !erEØS();
    }
}
