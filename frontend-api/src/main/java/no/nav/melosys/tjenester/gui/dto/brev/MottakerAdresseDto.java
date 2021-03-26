package no.nav.melosys.tjenester.gui.dto.brev;

import no.nav.melosys.service.brev.BrevAdresse;

import java.util.List;

public class MottakerAdresseDto {
    public String mottakerNavn;
    public String orgnr;
    public List<String> adresselinjer;
    public String postnr;
    public String poststed;
    public String land;

    public MottakerAdresseDto(String mottakerNavn, String orgnr, List<String> adresselinjer, String postnr, String poststed, String land) {
        this.mottakerNavn = mottakerNavn;
        this.orgnr = orgnr;
        this.adresselinjer = adresselinjer;
        this.postnr = postnr;
        this.poststed = poststed;
        this.land = land;
    }

    public static MottakerAdresseDto av(BrevAdresse brevAdresse) {
        return new MottakerAdresseDto(
            brevAdresse.getMottakerNavn(),
            brevAdresse.getOrgnr(),
            brevAdresse.getAdresselinjer(),
            brevAdresse.getPostnr(),
            brevAdresse.getPoststed(),
            brevAdresse.getLand()
        );
    }
}
