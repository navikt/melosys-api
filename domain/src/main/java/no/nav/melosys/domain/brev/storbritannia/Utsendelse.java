package no.nav.melosys.domain.brev.storbritannia;

import java.time.Instant;
import java.util.List;

public record Utsendelse(
    UKArtikkel artikkel,
    List<String> oppholdsadresseUK,
    Instant startdato,
    Instant sluttdato
) {
}
