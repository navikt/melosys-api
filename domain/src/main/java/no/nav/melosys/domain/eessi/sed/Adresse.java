package no.nav.melosys.domain.eessi.sed;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.domain.person.adresse.Oppholdsadresse;
import org.apache.commons.lang3.StringUtils;

import static no.nav.melosys.domain.eessi.sed.Adressetype.KONTAKTADRESSE;
import static no.nav.melosys.domain.util.IsoLandkodeKonverterer.tilIso3;

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
    private String tilleggsnavn;

    private Adresse() {
    }

    public boolean erGyldigAdresse() {
        return
            (gateadresse != null && (!gateadresse.isBlank() || !gateadresse.equals(IKKE_TILGJENGELIG))) &&
                (poststed != null && (!poststed.isBlank() || !poststed.equals(IKKE_TILGJENGELIG)));
    }

    public static Adresse lagAdresse(Adressetype adressetype, StrukturertAdresse strukturertAdresse) {
        if (strukturertAdresse == null) {
            return null;
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
        adresse.setTilleggsnavn(IKKE_TILGJENGELIG);
        adresse.setLand(landkode);
        return adresse;
    }

    public static Adresse lagIkkeFastAdresse(String landkode) {
        Adresse adresse = new Adresse();
        adresse.setPoststed(INGEN_FAST_ADRESSE);
        adresse.setLand(landkode);
        return adresse;
    }

    public static Adresse lagKontaktadresse(Kontaktadresse kontaktadresse) {
        if (kontaktadresse.strukturertAdresse() != null) {
            return lagAdresse(KONTAKTADRESSE, kontaktadresse.strukturertAdresse());
        }
        if (kontaktadresse.semistrukturertAdresse() != null) {
            return lagAdresse(KONTAKTADRESSE, kontaktadresse.semistrukturertAdresse().tilStrukturertAdresse());
        }
        return null;
    }

    public static Adresse lagOppholdsadresse(Oppholdsadresse oppholdsadresse) {
        // Adressetype POSTADRESSE svarer til opphold i Rina
        return lagAdresse(Adressetype.POSTADRESSE, oppholdsadresse.strukturertAdresse());
    }

    public static Adresse fraStrukturertAdresse(StrukturertAdresse strukturertAdresse) {
        Adresse adresse = new Adresse();
        adresse.setGateadresse(lagGateadresse(strukturertAdresse.getGatenavn(),
            strukturertAdresse.getHusnummerEtasjeLeilighet()));
        adresse.setTilleggsnavn(strukturertAdresse.getTilleggsnavn());
        adresse.setPostnr(strukturertAdresse.getPostnummer());
        adresse.setPoststed(StringUtils.isBlank(
            strukturertAdresse.getPoststed()) ? UKJENT : strukturertAdresse.getPoststed());
        adresse.setRegion(strukturertAdresse.getRegion());
        adresse.setLand(strukturertAdresse.getLandkode());
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
        strukturertAdresse.setTilleggsnavn(tilleggsnavn);
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

    public String getTilleggsnavn() {
        return tilleggsnavn;
    }

    private void setTilleggsnavn(String tilleggsnavn) {
        this.tilleggsnavn = tilleggsnavn;
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

    public void setLand(String land) {
        this.land = land;
    }
}
