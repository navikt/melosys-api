package no.nav.melosys.tjenester.gui.graphql.dto;

import java.time.LocalDate;

public record StatsborgerskapDto(
    String land,
    LocalDate bekreftelsesdato,
    LocalDate gyldigFraOgMed,
    LocalDate gyldigTilOgMed,
    String master,
    String kilde,
    boolean erHistorisk
) {
}
