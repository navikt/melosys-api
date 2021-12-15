package no.nav.melosys.domain.brev.storbritannia;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk;

public record Utsendelse(
    Lovvalgbestemmelser_trygdeavtale_uk artikkel,
    List<String> oppholdsadresseUK,
    LocalDate startdato,
    LocalDate sluttdato
) {
}
