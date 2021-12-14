package no.nav.melosys.domain.brev;

import java.time.LocalDate;

public record Person(
    String navn,
    LocalDate foedselsdato,
    String fnr,
    String dnr
) {
}
