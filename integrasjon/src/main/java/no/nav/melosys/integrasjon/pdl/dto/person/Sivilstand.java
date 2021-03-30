package no.nav.melosys.integrasjon.pdl.dto.person;

import java.time.LocalDate;

public record Sivilstand(Sivilstandstype type,
                         String relatertVedSivilstand,
                         LocalDate gyldigFraOgMed) {
}
