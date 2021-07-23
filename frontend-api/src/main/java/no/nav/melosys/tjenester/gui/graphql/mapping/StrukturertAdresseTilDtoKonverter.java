package no.nav.melosys.tjenester.gui.graphql.mapping;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.graphql.dto.StrukturertAdresseformatDto;

public final class StrukturertAdresseTilDtoKonverter {
    private StrukturertAdresseTilDtoKonverter() {
    }

    public static StrukturertAdresseformatDto tilDto(StrukturertAdresse strukturertAdresse,
                                                     KodeverkService kodeverkService) {
        if (strukturertAdresse == null) {
            return null;
        }
        return new StrukturertAdresseformatDto(
            strukturertAdresse.getTilleggsnavn(),
            strukturertAdresse.getGatenavn(),
            strukturertAdresse.getHusnummerEtasjeLeilighet(),
            strukturertAdresse.getPostboks(),
            strukturertAdresse.getPostnummer(),
            strukturertAdresse.getPoststed(),
            strukturertAdresse.getRegion(),
            kodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, strukturertAdresse.getLandkode())
        );
    }
}
