package no.nav.melosys.tjenester.gui.graphql.dto;

import java.time.LocalDate;

public record FoedselDto(
    int foedselsaar,
    LocalDate foedselsdato,
    String foedeland,
    String foedested
) {
}
