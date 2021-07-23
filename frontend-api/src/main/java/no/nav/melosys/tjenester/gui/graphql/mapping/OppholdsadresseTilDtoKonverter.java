package no.nav.melosys.tjenester.gui.graphql.mapping;

import no.nav.melosys.domain.person.adresse.Oppholdsadresse;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.graphql.dto.OppholdsadresseDto;

public final class OppholdsadresseTilDtoKonverter {
    private OppholdsadresseTilDtoKonverter() {
    }

    public static OppholdsadresseDto tilDto(Oppholdsadresse oppholdsadresse, KodeverkService kodeverkService) {
        return new OppholdsadresseDto(
            oppholdsadresse.coAdressenavn(),
            StrukturertAdresseTilDtoKonverter.tilDto(oppholdsadresse.strukturertAdresse(), kodeverkService),
            oppholdsadresse.gyldigFraOgMed(),
            oppholdsadresse.gyldigTilOgMed(),
            oppholdsadresse.master(),
            oppholdsadresse.kilde(),
            oppholdsadresse.erHistorisk()
        );
    }
}
