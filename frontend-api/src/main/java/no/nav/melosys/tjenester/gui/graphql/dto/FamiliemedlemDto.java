package no.nav.melosys.tjenester.gui.graphql.dto;

import java.time.LocalDate;

import no.nav.melosys.domain.person.Sivilstandstype;
import no.nav.melosys.domain.person.familie.Familierelasjon;

public record FamiliemedlemDto(
    String navn,
    String ident,
    Familierelasjon relasjonsrolle,
    Integer alder,
    String foreldreansvar,
    String fnrAnnenForelder,
    Sivilstandstype sivilstand,
    Boolean erHistorisk,
    LocalDate sivilstandGyldighetsperiodeFom
) {
}
