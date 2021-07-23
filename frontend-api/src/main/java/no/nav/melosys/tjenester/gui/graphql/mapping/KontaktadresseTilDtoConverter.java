package no.nav.melosys.tjenester.gui.graphql.mapping;

import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.tjenester.gui.graphql.dto.KontaktadresseDto;

public final class KontaktadresseTilDtoConverter {
    private KontaktadresseTilDtoConverter() {
    }

    public static KontaktadresseDto tilDto(Kontaktadresse kontaktadresse) {
        return new KontaktadresseDto(
            kontaktadresse.coAdressenavn(),
            SemistrukturertAdresseTilDtoConverter.tilDto(kontaktadresse.semistrukturertAdresse()),
            StrukturertAdresseTilDtoConverter.tilDto(kontaktadresse.strukturertAdresse()),
            kontaktadresse.gyldigFraOgMed(),
            kontaktadresse.gyldigTilOgMed(),
            kontaktadresse.master(),
            kontaktadresse.kilde(),
            kontaktadresse.erHistorisk()
        );
    }
}
