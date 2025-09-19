package no.nav.melosys.domain.person;

import java.time.LocalDate;

public record Foedsel(LocalDate fødselsdato,
                      Integer fødselsår,
                      String fødeland,
                      String fødested) {
}
