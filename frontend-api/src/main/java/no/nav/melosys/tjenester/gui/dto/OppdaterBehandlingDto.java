package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;

public record OppdaterBehandlingDto(
    String sakstype,
    String behandlingstype,
    String behandlingstema,
    String behandlingsstatus,
    LocalDate behandlingsfrist
) {
}
