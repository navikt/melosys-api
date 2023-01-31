package no.nav.melosys.tjenester.gui.dto.brev;

import java.util.List;

import no.nav.melosys.service.brev.brevmalliste.BrevAdresse;

public record MottakerAdresseDto(Tittel tittel,
                                 List<String> adresselinjer,
                                 String postnr,
                                 String poststed,
                                 String region,
                                 String land) {

    public static MottakerAdresseDto av(BrevAdresse brevAdresse) {
        return new MottakerAdresseDto(
            new Tittel(brevAdresse.getMottakerNavn(),
                brevAdresse.getOrgnr()),
            brevAdresse.getAdresselinjer(),
            brevAdresse.getPostnr(),
            brevAdresse.getPoststed(),
            brevAdresse.getRegion(),
            brevAdresse.getLand()
        );
    }

    public record Tittel(String mottakerNavn, String orgnr) {
    }
}
