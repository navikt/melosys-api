package no.nav.melosys.tjenester.gui.graphql.mapping;


import no.nav.melosys.domain.person.Foedsel;
import no.nav.melosys.tjenester.gui.graphql.dto.FoedselDto;

import static no.nav.melosys.domain.util.IsoLandkodeKonverterer.tilIso2;

public final class FoedselTilDtoKonverter {
    private FoedselTilDtoKonverter() {
    }

    public static FoedselDto tilDto(Foedsel foedsel) {
        return new FoedselDto(
            foedsel.getFødselsår(),
            foedsel.getFødselsdato(),
            tilIso2(foedsel.getFødeland()),
            foedsel.getFødested()
        );
    }
}
