package no.nav.melosys.tjenester.gui.graphql.dto;

import no.nav.melosys.domain.person.Sivilstand;
import no.nav.melosys.domain.person.familie.Familierelasjon;

public record FamiliemedlemDto(
    String navn,
    String ident,
    Familierelasjon relasjonsrolle,
    Integer alder,
    String foreldreansvar,
    String fnrAnnenForelder,
    Sivilstand sivilstand
) {
}
