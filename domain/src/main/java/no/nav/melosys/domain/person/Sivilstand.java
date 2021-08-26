package no.nav.melosys.domain.person;

import java.time.LocalDate;

public record Sivilstand(Sivilstandstype type,
                         String relatertVedSivilstand,
                         LocalDate gyldigFraOgMed,
                         LocalDate bekreftelsesdato,
                         String master,
                         String kilde,
                         boolean erHistorisk) {
}
