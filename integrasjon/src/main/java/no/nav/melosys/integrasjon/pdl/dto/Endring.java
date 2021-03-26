package no.nav.melosys.integrasjon.pdl.dto;

import java.time.LocalDateTime;

public record Endring(LocalDateTime registrert, String type) {
}
