package no.nav.melosys.integrasjon.pdl.dto;

import java.time.LocalDateTime;
import java.util.List;

public record Metadata(String master, boolean historisk, List<Endring> endringer) {
    public LocalDateTime datoSistRegistrert() {
        return endringer.stream()
            .filter(Endring::erOpphør)
            .map(Endring::registrert)
            .max(LocalDateTime::compareTo)
            .orElse(LocalDateTime.MIN);
    }
}
