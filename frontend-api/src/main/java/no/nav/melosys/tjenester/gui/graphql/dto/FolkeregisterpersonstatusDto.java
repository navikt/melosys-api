package no.nav.melosys.tjenester.gui.graphql.dto;

import java.time.LocalDate;

public record FolkeregisterpersonstatusDto(
    String kode,
    String tekst,
    String master,
    String kilde,
    LocalDate fregGyldighetstidspunkt,
    boolean erHistorisk
) {
}
