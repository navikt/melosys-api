package no.nav.melosys.tjenester.gui.graphql.mapping;

import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.tjenester.gui.graphql.dto.BostedsadresseDto;

public final class BostedsadresseTilDtoConverter {
    private BostedsadresseTilDtoConverter() {
    }

    public static BostedsadresseDto tilDto(Bostedsadresse bostedsadresse) {
        return new BostedsadresseDto(
            bostedsadresse.coAdressenavn(),
            StrukturertAdresseTilDtoConverter.tilDto(bostedsadresse.strukturertAdresse()),
            bostedsadresse.gyldigFraOgMed(),
            bostedsadresse.gyldigTilOgMed(),
            bostedsadresse.master(),
            bostedsadresse.kilde(),
            bostedsadresse.erHistorisk()
        );
    }
}
