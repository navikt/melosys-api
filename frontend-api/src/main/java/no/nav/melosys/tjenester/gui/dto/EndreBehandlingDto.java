package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;

public record EndreBehandlingDto(
    String sakstype,
    String behandlingstype,
    String behandlingstema,
    String behandlingsstatus,
    LocalDate behandlingsfrist
) {
}
