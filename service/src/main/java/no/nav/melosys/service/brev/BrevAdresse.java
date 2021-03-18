package no.nav.melosys.service.brev;

import java.util.List;

public class BrevAdresse {
    public String mottakerNavn;
    public String orgnr;
    public List<String> adresselinjer;
    public String postnr;
    public String poststed;
    public String land;

    public BrevAdresse(String mottakerNavn, String orgnr, List<String> adresselinjer, String postnr, String poststed, String land) {
        this.mottakerNavn = mottakerNavn;
        this.orgnr = orgnr;
        this.adresselinjer = adresselinjer;
        this.postnr = postnr;
        this.poststed = poststed;
        this.land = land;
    }

    public String getMottakerNavn() {
        return mottakerNavn;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public List<String> getAdresselinjer() {
        return adresselinjer;
    }

    public String getPostnr() {
        return postnr;
    }

    public String getPoststed() {
        return poststed;
    }

    public String getLand() {
        return land;
    }
}
