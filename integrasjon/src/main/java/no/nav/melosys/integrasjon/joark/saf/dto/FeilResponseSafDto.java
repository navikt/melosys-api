package no.nav.melosys.integrasjon.joark.saf.dto;

import java.time.ZonedDateTime;

public record FeilResponseSafDto(
    ZonedDateTime timestamp,
    int status,
    String error,
    String message,
    String path
) {
}
