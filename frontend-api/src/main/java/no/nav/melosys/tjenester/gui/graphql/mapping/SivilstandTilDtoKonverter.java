package no.nav.melosys.tjenester.gui.graphql.mapping;

import no.nav.melosys.domain.person.Sivilstand;
import no.nav.melosys.tjenester.gui.graphql.dto.SivilstandDto;

public final class SivilstandTilDtoKonverter {
    private SivilstandTilDtoKonverter() {
    }

    public static SivilstandDto tilDto(Sivilstand sivilstand) {
        return new SivilstandDto(
            sivilstand.type().toString(),
            sivilstand.relatertVedSivilstand(),
            sivilstand.gyldigFraOgMed(),
            sivilstand.bekreftelsesdato(),
            MasterTilDtoKonverter.tilDto(sivilstand.master()),
            sivilstand.kilde(),
            sivilstand.erHistorisk()
        );
    }
}
