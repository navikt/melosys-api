package no.nav.melosys.tjenester.gui.graphql.mapping;

import no.nav.melosys.domain.person.Statsborgerskap;
import no.nav.melosys.tjenester.gui.graphql.dto.StatsborgerskapDto;

public class StatsborgerskapTilDtoConverter {
    public static StatsborgerskapDto tilDto(Statsborgerskap statsborgerskap) {
        return new StatsborgerskapDto(
            statsborgerskap.land(),
            statsborgerskap.bekreftelsesdato(),
            statsborgerskap.gyldigFraOgMed(),
            statsborgerskap.gyldigTilOgMed(),
            statsborgerskap.master(),
            statsborgerskap.kilde(),
            statsborgerskap.erHistorisk()
        );
    }
}
