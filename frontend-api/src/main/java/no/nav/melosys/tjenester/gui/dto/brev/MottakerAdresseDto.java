package no.nav.melosys.tjenester.gui.dto.brev;

import no.nav.melosys.service.brev.BrevAdresse;

import java.util.List;

public record MottakerAdresseDto(Tittel tittel,
                                 List<String> adresselinjer,
                                 String postnr,
                                 String poststed,
                                 String land) {

    public static MottakerAdresseDto av(BrevAdresse brevAdresse) {
        return new MottakerAdresseDto(
            new Tittel(brevAdresse.getMottakerNavn(),
                brevAdresse.getOrgnr()),
            brevAdresse.getAdresselinjer(),
            brevAdresse.getPostnr(),
            brevAdresse.getPoststed(),
            brevAdresse.getLand()
        );
    }

    public record Tittel(String mottakerNavn, String orgnr) { }
}
