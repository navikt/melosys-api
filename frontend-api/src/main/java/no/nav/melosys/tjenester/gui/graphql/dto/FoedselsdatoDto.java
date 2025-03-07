package no.nav.melosys.tjenester.gui.graphql.dto;

import java.time.LocalDate;

public record FoedselsdatoDto(
    int foedselsaar,
    LocalDate foedselsdato
) {
}
