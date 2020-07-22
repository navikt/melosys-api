package no.nav.melosys.domain.eessi.sed;

import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import org.apache.commons.lang3.StringUtils;

import static no.nav.melosys.domain.util.LandkoderUtils.tilIso3;

public class Adresse {
    static final String IKKE_TILGJENGELIG = "N/A";
    public static final String INGEN_FAST_ADRESSE = "No fixed address";

    private Adressetype adressetype;
    private String gateadresse;
    private String postnr;
    private String poststed;
    private String region;
    private String land;

    private Adresse() {
    }

    public static Adresse lagIkkeFastAdresse(String landkode) {
        Adresse adresse = new Adresse();
        adresse.setPoststed(INGEN_FAST_ADRESSE);
        adresse.setLand(landkode);
        return adresse;
    }

    public static Adresse lagMaritimAdresse(String landkode) {
        Adresse adresse = new Adresse();
        adresse.setLand(landkode);
        adresse.setPoststed(IKKE_TILGJENGELIG);
        adresse.setGateadresse(IKKE_TILGJENGELIG);
        return adresse;
    }

    public static Adresse lagTomAdresse() {
        return new Adresse();
    }

    public static Adresse lagAdresse(Adressetype adressetype, StrukturertAdresse bostedsadresse) {
        if (bostedsadresse == null) {
            return Adresse.lagTomAdresse();
        }

        Adresse adresse = new Adresse();
        adresse.setAdressetype(adressetype);
        adresse.setPoststed(bostedsadresse.poststed);
        adresse.setPostnr(bostedsadresse.postnummer);
        adresse.setLand(tilIso3(bostedsadresse.landkode));
        adresse.setGateadresse(lagGateadresse(bostedsadresse.gatenavn, bostedsadresse.husnummer));
        return adresse;
    }

    public static Adresse fraStrukturertAdresse(StrukturertAdresse strukturertAdresse) {
        Adresse adresse = new Adresse();
        adresse.setGateadresse(lagGateadresse(strukturertAdresse.gatenavn, strukturertAdresse.husnummer));
        adresse.setLand(strukturertAdresse.landkode);
        adresse.setPostnr(strukturertAdresse.postnummer);
        adresse.setPoststed(strukturertAdresse.poststed);
        adresse.setRegion(strukturertAdresse.region);
        return adresse;
    }

    private static String lagGateadresse(String gatenavn, String husnummer) {
        if (StringUtils.isBlank(gatenavn)) {
            return IKKE_TILGJENGELIG;
        }
        return gatenavn + (StringUtils.isEmpty(husnummer) ? "" : String.format(" %s", husnummer));
    }

    public StrukturertAdresse tilStrukturertAdresse() {
        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.landkode = land;
        strukturertAdresse.gatenavn = gateadresse;
        strukturertAdresse.region = region;
        strukturertAdresse.postnummer = postnr;
        strukturertAdresse.poststed = poststed;
        return strukturertAdresse;
    }

    public Adressetype getAdressetype() {
        return adressetype;
    }

    public void setAdressetype(Adressetype adressetype) {
        this.adressetype = adressetype;
    }

    public String getGateadresse() {
        return gateadresse;
    }

    public void setGateadresse(String gateadresse) {
        this.gateadresse = gateadresse;
    }

    public String getPostnr() {
        return postnr;
    }

    public void setPostnr(String postnr) {
        this.postnr = postnr;
    }

    public String getPoststed() {
        return poststed;
    }

    public void setPoststed(String poststed) {
        this.poststed = poststed;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
    }
}
