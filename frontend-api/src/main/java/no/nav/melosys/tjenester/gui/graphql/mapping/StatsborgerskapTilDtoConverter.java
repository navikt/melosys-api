package no.nav.melosys.tjenester.gui.graphql.mapping;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.person.Statsborgerskap;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.graphql.dto.StatsborgerskapDto;

public class StatsborgerskapTilDtoConverter {

    public static StatsborgerskapDto tilDto(Statsborgerskap statsborgerskap, KodeverkService kodeverkService) {
        return new StatsborgerskapDto(
            kodeverkService.dekod(FellesKodeverk.STATSBORGERSKAP_FREG, statsborgerskap.land()),
            statsborgerskap.bekreftelsesdato(),
            statsborgerskap.gyldigFraOgMed(),
            statsborgerskap.gyldigTilOgMed(),
            statsborgerskap.master(),
            statsborgerskap.kilde(),
            statsborgerskap.erHistorisk()
        );
    }
}
