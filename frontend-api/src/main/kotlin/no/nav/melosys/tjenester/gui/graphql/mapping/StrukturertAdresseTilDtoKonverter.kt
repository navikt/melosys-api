package no.nav.melosys.tjenester.gui.graphql.mapping

import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.service.kodeverk.KodeverkService
import no.nav.melosys.tjenester.gui.graphql.dto.StrukturertAdresseformatDto


object StrukturertAdresseTilDtoKonverter {
    fun tilDto(
        strukturertAdresse: StrukturertAdresse?,
        kodeverkService: KodeverkService
    ): StrukturertAdresseformatDto? {
        return if (strukturertAdresse == null) {
            null
        } else StrukturertAdresseformatDto(
            strukturertAdresse.tilleggsnavn,
            strukturertAdresse.gatenavn,
            strukturertAdresse.husnummerEtasjeLeilighet,
            mapPostboks(strukturertAdresse.postboks),
            strukturertAdresse.postnummer,
            strukturertAdresse.poststed,
            strukturertAdresse.region,
            kodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, strukturertAdresse.landkode)
        )
    }

    private fun mapPostboks(postBoks: String?): String? {
        return if (postBoks != null) "Postboks $postBoks" else null
    }
}

