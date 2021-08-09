package no.nav.melosys.tjenester.gui.graphql.dto;

import java.time.LocalDateTime;

public record KontaktadresseDto(
    String coAdressenavn,
    SemistrukturertAdresseformatDto semistrukturertAdresse,
    StrukturertAdresseformatDto strukturertAdresse,
    LocalDateTime gyldigFraOgMed,
    LocalDateTime gyldigTilOgMed,
    String master,
    String kilde,
    boolean erHistorisk
) {
}
