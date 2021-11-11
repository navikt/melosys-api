package no.nav.melosys.tjenester.gui.graphql.dto;

import java.time.LocalDate;

public record KontaktadresseDto(
    String coAdressenavn,
    SemistrukturertAdresseformatDto semistrukturertAdresse,
    StrukturertAdresseformatDto strukturertAdresse,
    LocalDate gyldigFraOgMed,
    LocalDate gyldigTilOgMed,
    String master,
    String kilde,
    boolean erHistorisk
) {
}
