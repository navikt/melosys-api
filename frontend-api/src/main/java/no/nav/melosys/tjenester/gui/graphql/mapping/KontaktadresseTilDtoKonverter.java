package no.nav.melosys.tjenester.gui.graphql.mapping;

import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.graphql.dto.KontaktadresseDto;

public final class KontaktadresseTilDtoKonverter {
    private KontaktadresseTilDtoKonverter() {
    }

    public static KontaktadresseDto tilDto(Kontaktadresse kontaktadresse, KodeverkService kodeverkService) {
        return new KontaktadresseDto(
            kontaktadresse.coAdressenavn(),
            SemistrukturertAdresseTilDtoKonverter.tilDto(kontaktadresse.semistrukturertAdresse(), kodeverkService),
            StrukturertAdresseTilDtoKonverter.tilDto(kontaktadresse.strukturertAdresse(), kodeverkService),
            kontaktadresse.gyldigFraOgMed(),
            kontaktadresse.gyldigTilOgMed(),
            MasterTilDtoKonverter.tilDto(kontaktadresse.master()),
            kontaktadresse.kilde(),
            kontaktadresse.erHistorisk()
        );
    }
}
