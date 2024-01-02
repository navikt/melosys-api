package no.nav.melosys.tjenester.gui.graphql.mapping

import no.nav.melosys.domain.person.adresse.Oppholdsadresse
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.tjenester.gui.graphql.dto.OppholdsadresseDto
import no.nav.melosys.tjenester.gui.graphql.mapping.StrukturertAdresseTilDtoKonverter.tilDto


object OppholdsadresseTilDtoKonverter {
    fun tilDto(oppholdsadresse: Oppholdsadresse, kodeverkService: KodeverkService?): OppholdsadresseDto {
        return OppholdsadresseDto(
            oppholdsadresse.coAdressenavn,
            tilDto(oppholdsadresse.strukturertAdresse, kodeverkService!!),
            oppholdsadresse.gyldigFraOgMed,
            oppholdsadresse.gyldigTilOgMed,
            MasterTilDtoKonverter.tilDto(oppholdsadresse.master),
            oppholdsadresse.kilde,
            oppholdsadresse.erHistorisk
        )
    }
}
