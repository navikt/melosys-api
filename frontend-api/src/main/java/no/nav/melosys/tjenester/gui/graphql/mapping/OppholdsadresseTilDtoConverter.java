package no.nav.melosys.tjenester.gui.graphql.mapping;

import no.nav.melosys.domain.person.adresse.Oppholdsadresse;
import no.nav.melosys.tjenester.gui.graphql.dto.OppholdsadresseDto;

public final class OppholdsadresseTilDtoConverter {
    private OppholdsadresseTilDtoConverter() {
    }

    public static OppholdsadresseDto tilDto(Oppholdsadresse oppholdsadresse) {
        return new OppholdsadresseDto(
            oppholdsadresse.coAdressenavn(),
            StrukturertAdresseTilDtoConverter.tilDto(oppholdsadresse.strukturertAdresse()),
            oppholdsadresse.gyldigFraOgMed(),
            oppholdsadresse.gyldigTilOgMed(),
            oppholdsadresse.master(),
            oppholdsadresse.kilde(),
            oppholdsadresse.erHistorisk()
        );
    }
}
