package no.nav.melosys.domain.person;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Statsborgerskap(String land,
                              LocalDate gyldigFraOgMed,
                              LocalDate gyldigTilOgMed,
                              LocalDate bekreftelsesdato,
                              boolean erHistorisk,
                              String master,
                              String kilde,
                              LocalDateTime registreringstidspunkt) {
}
