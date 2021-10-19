package no.nav.melosys.domain.brev;

import java.time.Instant;

public record Person(
    String navn,
    Instant foedselsdato,
    String fnr,
    String dnr
) {
}
