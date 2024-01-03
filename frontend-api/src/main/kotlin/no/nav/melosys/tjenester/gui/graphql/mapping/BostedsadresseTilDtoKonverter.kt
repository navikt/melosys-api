package no.nav.melosys.tjenester.gui.graphql.mapping

import no.nav.melosys.domain.person.adresse.Bostedsadresse
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.tjenester.gui.graphql.dto.BostedsadresseDto


object BostedsadresseTilDtoKonverter {
    fun tilDto(bostedsadresse: Bostedsadresse, kodeverkService: KodeverkService): BostedsadresseDto {
        return BostedsadresseDto(
            bostedsadresse.coAdressenavn,
            StrukturertAdresseTilDtoKonverter.tilDto(bostedsadresse.strukturertAdresse, kodeverkService),
            bostedsadresse.gyldigFraOgMed,
            bostedsadresse.gyldigTilOgMed,
            MasterTilDtoKonverter.tilDto(bostedsadresse.master),
            bostedsadresse.kilde,
            bostedsadresse.erHistorisk
        )
    }
}

