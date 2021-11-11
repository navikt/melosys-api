package no.nav.melosys.tjenester.gui.graphql.dto;

import java.time.LocalDate;

public record BostedsadresseDto(
    String coAdressenavn,
    StrukturertAdresseformatDto adresse,
    LocalDate gyldigFraOgMed,
    LocalDate gyldigTilOgMed,
    String master,
    String kilde,
    boolean erHistorisk
) {
}
