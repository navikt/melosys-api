package no.nav.melosys.tjenester.gui.graphql.dto;

import java.time.LocalDate;

public record FolkeregisterpersonstatusDto(
    String kode,
    String tekst,
    LocalDate gyldigFraOgMed,
    LocalDate gyldigTilOgMed,
    String master,
    String kilde,
    boolean erHistorisk
) {
}
