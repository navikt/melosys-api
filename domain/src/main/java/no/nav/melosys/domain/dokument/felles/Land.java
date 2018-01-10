package no.nav.melosys.domain.dokument.felles;

import no.nav.melosys.domain.dokument.KodeverkEnum;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;
import java.util.*;

@XmlType(name = "Land")
@XmlEnum
public enum Land implements KodeverkEnum<Land> {
    BEL("BELGIA"),
    BGR("BULGARIA"),
    DNK("DANMARK"),
    CZE("TSJEKKIA"),
    EST("ESTLAND"),
    FIN("FINLAND"),
    FRA("FRANKRIKE"),
    FRO("FÆRØYENE"),
    GRL("GRØNLAND"),
    GRC("HELLAS"),
    IRL("IRLAND"),
    ISL("ISLAND"),
    ITA("ITALIA"),
    HRV("KROATIA"),
    CYP("KYPROS"),
    LVA("LATVIA"),
    LIE("LIECHTENSTEIN"),
    LTU("LITAUEN"),
    LUX("LUXEMBOURG"),
    MLT("MALTA"),
    NLD("NEDERLAND"),
    NOR("NORGE"),
    POL("POLEN"),
    PRT("PORTUGAL"),
    ROU("ROMANIA"),
    SVK("SLOVAKIA"),
    SVN("SLOVENIA"),
    ESP("SPANIA"),
    GBR("STORBRITANNIA"),
    SWZ("SVEITS"),
    SWE("SVERIGE"),
    DEU("TYSKLAND"),
    HUN("UNGARN"),
    AUT("ØSTERRIKE"),
    UKJ("UOPPGITT/UKJENT"); // Egentlig kodeverdi er '???'

    // TODO: Sjekk om/hvordan vi skal håndtere Sveits og Svalbard
    private static final Set<Land> EØS = new HashSet<>(Arrays.asList(ISL, LIE, NOR));
    private static final Set<Land> EU = new HashSet<>(Arrays.asList(
            BEL, BGR, DNK, EST, FIN, FRA, GRC, IRL, ITA, HRV, CYP,
            LVA, LTU, LUX, MLT, NLD, POL, PRT, ROU, SVK, SVN, ESP,
            GBR, SWE, CZE, DEU, HUN, AUT));
    // TODO: Sjekk om Åland skal være med
    private static final Set<Land> NORDEN_UTEN_NORGE = new HashSet<>(Arrays.asList(DNK, FIN, FRO, GRL, ISL, SWE));

    private String navn;

    // Brukes av JAXB
    Land() {}

    Land(String navn) {
        this.navn = navn;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public boolean erEØS() {
        return EØS.contains(this);
    }

    public boolean erEU() {
        return EU.contains(this);
    }

    public boolean erSveits() {
        return SWZ.navn.equals(navn);
    }

    public boolean erStatsløs() {
        return navn == null;
    }

    public boolean erNordenUtenNorge() {
        return NORDEN_UTEN_NORGE.contains(this);
    }

    public boolean erNederland() {
        return NLD.navn.equals(navn);
    }

    public boolean erLuxembourg() {
        return LUX.navn.equals(navn);
    }

    public boolean erTredjeland() {
        return navn != null && !erEU() && !erEØS();
    }
}
