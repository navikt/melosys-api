package no.nav.melosys.tjenester.gui.graphql.mapping;

import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.tjenester.gui.graphql.dto.StrukturertAdresseformatDto;

public final class StrukturertAdresseTilDtoConverter {
    private StrukturertAdresseTilDtoConverter() {
    }

    public static StrukturertAdresseformatDto tilDto(StrukturertAdresse strukturertAdresse) {
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
            strukturertAdresse.getLandkode()
        );
    }
}
