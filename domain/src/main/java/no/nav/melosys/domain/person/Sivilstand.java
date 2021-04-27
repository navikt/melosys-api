package no.nav.melosys.domain.person;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Sivilstand(Sivilstandstype sivilstandstype,
                         LocalDate gyldigFraOgMed,
                         boolean erHistorisk,
                         String master,
                         String kilde,
                         LocalDateTime registreringstidspunkt) {
}
