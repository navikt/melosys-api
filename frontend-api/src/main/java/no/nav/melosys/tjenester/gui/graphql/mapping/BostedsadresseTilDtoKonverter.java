package no.nav.melosys.tjenester.gui.graphql.mapping;

import no.nav.melosys.domain.person.adresse.Bostedsadresse;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.graphql.dto.BostedsadresseDto;

public final class BostedsadresseTilDtoKonverter {
    private BostedsadresseTilDtoKonverter() {
    }

    public static BostedsadresseDto tilDto(Bostedsadresse bostedsadresse, KodeverkService kodeverkService) {
        return new BostedsadresseDto(
            bostedsadresse.coAdressenavn(),
            StrukturertAdresseTilDtoKonverter.tilDto(bostedsadresse.strukturertAdresse(), kodeverkService),
            bostedsadresse.gyldigFraOgMed(),
            bostedsadresse.gyldigTilOgMed(),
            MasterTilDtoKonverter.tilDto(bostedsadresse.master()),
            bostedsadresse.kilde(),
            bostedsadresse.erHistorisk()
        );
    }
}
