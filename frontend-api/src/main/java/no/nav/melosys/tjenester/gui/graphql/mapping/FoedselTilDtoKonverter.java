package no.nav.melosys.tjenester.gui.graphql.mapping;


import no.nav.melosys.domain.person.Foedsel;
import no.nav.melosys.tjenester.gui.graphql.dto.FoedselDto;

public final class FoedselTilDtoKonverter {
    private FoedselTilDtoKonverter() {
    }

    public static FoedselDto tilDto(Foedsel foedsel) {
        return new FoedselDto(
            foedsel.fødselsår(),
            foedsel.fødselsdato(),
            foedsel.fødeland(),
            foedsel.fødested()
        );
    }
}
