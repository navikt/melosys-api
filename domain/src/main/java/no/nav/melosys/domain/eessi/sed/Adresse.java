package no.nav.melosys.domain.eessi.sed;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import org.apache.commons.lang3.StringUtils;

import static no.nav.melosys.domain.util.LandkoderUtils.tilIso3;

public class Adresse {
    public static final String IKKE_TILGJENGELIG = "N/A";
    public static final String UKJENT = "Unknown";
    public static final String INGEN_FAST_ADRESSE = "No fixed address";

    private Adressetype adressetype;
    private String gateadresse;
    private String postnr;
    private String poststed;
    private String region;
    private String land;

    private Adresse() {
    }

    public static Adresse lagAdresse(Adressetype adressetype, StrukturertAdresse strukturertAdresse) {
        if (strukturertAdresse == null) {
            return Adresse.lagTomAdresse();
        }

        Adresse adresse = fraStrukturertAdresse(strukturertAdresse);
        adresse.setAdressetype(adressetype);
        adresse.setLand(tilIso3(strukturertAdresse.getLandkode()));
        return adresse;
    }

    public static Adresse lagAdresseMedBareLandkode(String landkode) {
        Adresse adresse = new Adresse();
        adresse.setGateadresse(IKKE_TILGJENGELIG);
        adresse.setPoststed(IKKE_TILGJENGELIG);
        adresse.setLand(landkode);
        return adresse;
    }

    public static Adresse lagIkkeFastAdresse(String landkode) {
        Adresse adresse = new Adresse();
        adresse.setPoststed(INGEN_FAST_ADRESSE);
        adresse.setLand(landkode);
        return adresse;
    }

    public static Adresse lagTomAdresse() {
        return new Adresse();
    }

    public static Adresse fraStrukturertAdresse(StrukturertAdresse strukturertAdresse) {
        Adresse adresse = new Adresse();
        adresse.setGateadresse(lagGateadresse(strukturertAdresse.getGatenavn(),
            strukturertAdresse.getHusnummerEtasjeLeilighet()));
        adresse.setLand(strukturertAdresse.getLandkode());
        adresse.setPostnr(strukturertAdresse.getPostnummer());
        adresse.setPoststed(StringUtils.isBlank(
            strukturertAdresse.getPoststed()) ? UKJENT : strukturertAdresse.getPoststed());
        adresse.setRegion(strukturertAdresse.getRegion());
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
        strukturertAdresse.setLandkode(land);
        strukturertAdresse.setGatenavn(gateadresse);
        strukturertAdresse.setRegion(region);
        strukturertAdresse.setPostnummer(postnr);
        strukturertAdresse.setPoststed(poststed);
        return strukturertAdresse;
    }

    public Adressetype getAdressetype() {
        return adressetype;
    }

    private void setAdressetype(Adressetype adressetype) {
        this.adressetype = adressetype;
    }

    public String getGateadresse() {
        return gateadresse;
    }

    private void setGateadresse(String gateadresse) {
        this.gateadresse = gateadresse;
    }

    public String getPostnr() {
        return postnr;
    }

    private void setPostnr(String postnr) {
        this.postnr = StringUtils.isBlank(postnr) ? IKKE_TILGJENGELIG : postnr;
    }

    public String getPoststed() {
        return poststed;
    }

    private void setPoststed(String poststed) {
        this.poststed = StringUtils.isBlank(poststed) ? IKKE_TILGJENGELIG : poststed;
    }

    public String getRegion() {
        return region;
    }

    private void setRegion(String region) {
        this.region = region;
    }

    public String getLand() {
        return land;
    }

    private void setLand(String land) {
        this.land = land;
    }
}
