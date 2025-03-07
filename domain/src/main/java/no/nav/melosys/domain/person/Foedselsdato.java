package no.nav.melosys.domain.person;

import java.time.LocalDate;

public record Foedselsdato(LocalDate fødselsdato,
                           Integer fødselsår) {
}
