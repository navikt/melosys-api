package no.nav.melosys.domain.brev.trygdeavtale;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;

public record Utsendelse(
    LovvalgBestemmelse artikkel,
    List<String> oppholdsadresse,
    LocalDate startdato,
    LocalDate sluttdato
) {
}
