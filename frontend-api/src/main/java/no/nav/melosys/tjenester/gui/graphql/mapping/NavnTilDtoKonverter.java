package no.nav.melosys.tjenester.gui.graphql.mapping;

import no.nav.melosys.domain.person.Navn;
import no.nav.melosys.tjenester.gui.graphql.dto.NavnDto;

public final class NavnTilDtoKonverter {
    private NavnTilDtoKonverter() {
    }

    public static NavnDto tilDto(Navn navn) {
        return new NavnDto(navn.fornavn(), navn.mellomnavn(), navn.etternavn());
    }
}
