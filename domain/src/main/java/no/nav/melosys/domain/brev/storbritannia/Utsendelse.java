package no.nav.melosys.domain.brev.storbritannia;

import java.time.Instant;
import java.util.List;

public record Utsendelse(
    boolean artikkel6_1,
    boolean artikkel7_3,
    boolean artikkel6_5,
    List<String> oppholdsadresseUK,
    Instant startdato,
    Instant sluttdato
) {
}
