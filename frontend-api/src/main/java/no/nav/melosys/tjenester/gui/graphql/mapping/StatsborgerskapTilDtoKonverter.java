package no.nav.melosys.tjenester.gui.graphql.mapping;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.person.Statsborgerskap;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.graphql.dto.StatsborgerskapDto;

public final class StatsborgerskapTilDtoKonverter {
    private StatsborgerskapTilDtoKonverter() {
    }

    public static StatsborgerskapDto tilDto(Statsborgerskap statsborgerskap, KodeverkService kodeverkService) {
        return new StatsborgerskapDto(
            kodeverkService.dekod(FellesKodeverk.STATSBORGERSKAP_FREG, statsborgerskap.landkode()),
            statsborgerskap.bekreftelsesdato(),
            statsborgerskap.gyldigFraOgMed(),
            statsborgerskap.gyldigTilOgMed(),
            mapMaster(statsborgerskap.master()),
            statsborgerskap.kilde(),
            statsborgerskap.erHistorisk()
        );
    }

    private static String mapMaster(String master) {
        final String PDL = "PDL";
        final String NAV_PDL = "NAV (PDL)";

        if (PDL.equals(master)) return NAV_PDL;

        return master;
    }
}
