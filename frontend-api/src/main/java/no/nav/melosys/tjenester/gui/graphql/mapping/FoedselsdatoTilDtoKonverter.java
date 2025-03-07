package no.nav.melosys.tjenester.gui.graphql.mapping;


import no.nav.melosys.domain.person.Foedselsdato;
import no.nav.melosys.tjenester.gui.graphql.dto.FoedselsdatoDto;

public final class FoedselsdatoTilDtoKonverter {
    private FoedselsdatoTilDtoKonverter() {
    }

    public static FoedselsdatoDto tilDto(Foedselsdato foedselsdato) {
        return new FoedselsdatoDto(
            foedselsdato.fødselsår(),
            foedselsdato.fødselsdato()
        );
    }
}
