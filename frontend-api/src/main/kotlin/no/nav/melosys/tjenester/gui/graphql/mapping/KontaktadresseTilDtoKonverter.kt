package no.nav.melosys.tjenester.gui.graphql.mapping

import no.nav.melosys.domain.person.adresse.Kontaktadresse
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.tjenester.gui.graphql.dto.KontaktadresseDto
import no.nav.melosys.tjenester.gui.graphql.mapping.StrukturertAdresseTilDtoKonverter.tilDto


object KontaktadresseTilDtoKonverter {
    fun tilDto(kontaktadresse: Kontaktadresse, kodeverkService: KodeverkService?): KontaktadresseDto {
        return KontaktadresseDto(
            kontaktadresse.coAdressenavn,
            SemistrukturertAdresseTilDtoKonverter.tilDto(kontaktadresse.semistrukturertAdresse, kodeverkService),
            tilDto(kontaktadresse.strukturertAdresse, kodeverkService!!),
            kontaktadresse.gyldigFraOgMed,
            kontaktadresse.gyldigTilOgMed,
            MasterTilDtoKonverter.tilDto(kontaktadresse.master),
            kontaktadresse.kilde,
            kontaktadresse.erHistorisk
        )
    }
}

