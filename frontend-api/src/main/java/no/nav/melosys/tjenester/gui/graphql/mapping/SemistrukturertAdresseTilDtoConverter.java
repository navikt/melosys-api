package no.nav.melosys.tjenester.gui.graphql.mapping;

import no.nav.melosys.domain.adresse.SemistrukturertAdresse;
import no.nav.melosys.tjenester.gui.graphql.dto.SemistrukturertAdresseformatDto;

public final class SemistrukturertAdresseTilDtoConverter {
    private SemistrukturertAdresseTilDtoConverter() {
    }

    public static SemistrukturertAdresseformatDto tilDto(SemistrukturertAdresse semistrukturertAdresse) {
        if (semistrukturertAdresse == null) {
            return null;
        }
        return new SemistrukturertAdresseformatDto(
            semistrukturertAdresse.adresselinje1(),
            semistrukturertAdresse.adresselinje2(),
            semistrukturertAdresse.adresselinje3(),
            semistrukturertAdresse.adresselinje4(),
            semistrukturertAdresse.postnr(),
            semistrukturertAdresse.poststed(),
            semistrukturertAdresse.landkode()
        );
    }
}
