package no.nav.melosys.domain.brev.storbritannia;

import java.time.Instant;
import java.util.List;

import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;

public record Utsendelse(
    Lovvalgbestemmelser_trygdeavtale_uk artikkel,
    List<String> oppholdsadresseUK,
    Instant startdato,
    Instant sluttdato
) {
}
