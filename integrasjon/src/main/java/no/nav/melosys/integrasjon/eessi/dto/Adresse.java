package no.nav.melosys.integrasjon.eessi.dto;

import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;

public class Adresse {

    private String poststed;
    private String postnr;
    private String land;
    private String gateadresse;
    private String region;
    private Adressetype adressetype;

    public StrukturertAdresse tilStrukturertAdresse() {
        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();

        strukturertAdresse.landkode = land;
        strukturertAdresse.gatenavn = gateadresse;
        strukturertAdresse.region = region;
        strukturertAdresse.postnummer = postnr;
        strukturertAdresse.poststed = poststed;

        return strukturertAdresse;
    }

    public SemistrukturertAdresse tilSemistrukturertAdresse() {
        SemistrukturertAdresse semistrukturertAdresse = new SemistrukturertAdresse();

        semistrukturertAdresse.setLandkode(land);
        semistrukturertAdresse.setPostnr(postnr);
        semistrukturertAdresse.setPoststed(poststed);
        semistrukturertAdresse.setAdresselinje1(gateadresse);
        semistrukturertAdresse.setAdresselinje2(region);

        return semistrukturertAdresse;
    }

    public String getPoststed() {
        return poststed;
    }

    public void setPoststed(String poststed) {
        this.poststed = poststed;
    }

    public String getPostnr() {
        return postnr;
    }

    public void setPostnr(String postnr) {
        this.postnr = postnr;
    }

    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
    }

    public String getGateadresse() {
        return gateadresse;
    }

    public void setGateadresse(String gateadresse) {
        this.gateadresse = gateadresse;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Adressetype getAdressetype() {
        return adressetype;
    }

    public void setAdressetype(Adressetype adressetype) {
        this.adressetype = adressetype;
    }
}
