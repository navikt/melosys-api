package no.nav.melosys.integrasjon.pdl.dto;

import java.time.LocalDateTime;

public record Folkeregistermetadata(
    LocalDateTime ajourholdstidspunkt,
    LocalDateTime gyldighetstidspunkt,
    LocalDateTime opphoerstidspunkt,
    String kilde,
    String aarsak
) {
}
