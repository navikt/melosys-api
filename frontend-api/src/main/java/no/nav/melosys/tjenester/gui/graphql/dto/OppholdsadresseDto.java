package no.nav.melosys.tjenester.gui.graphql.dto;

import java.time.LocalDateTime;

public record OppholdsadresseDto(
    String coAdressenavn,
    StrukturertAdresseformatDto adresse,
    LocalDateTime gyldigFraOgMed,
    LocalDateTime gyldigTilOgMed,
    String master,
    String kilde,
    boolean erHistorisk
) {
}
