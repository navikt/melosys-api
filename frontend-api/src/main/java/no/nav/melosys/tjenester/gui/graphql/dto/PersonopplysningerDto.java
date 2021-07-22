package no.nav.melosys.tjenester.gui.graphql.dto;

import java.util.List;

public record PersonopplysningerDto(
    List<BostedsadresseDto> bostedsadresser,
    List<OppholdsadresseDto> oppholdsadresser,
    List<StatsborgerskapDto> statsborgerskap
) {
}
