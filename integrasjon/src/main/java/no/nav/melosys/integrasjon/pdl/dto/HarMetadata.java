package no.nav.melosys.integrasjon.pdl.dto;

import java.time.LocalDateTime;
import java.util.Comparator;

public interface HarMetadata {
    Metadata metadata();

    default boolean erGyldigFør(LocalDateTime tidspunkt) {
        return metadata().endringer().stream()
            .filter(e -> !e.erOpphør())
            .anyMatch(e -> e.registrert().isBefore(tidspunkt));
    }

    default boolean erGyldig() {
        return metadata().endringer().stream()
            .max(Comparator.comparing(Endring::registrert))
            .filter(Endring::erOpphør)
            .isEmpty();
    }

    default LocalDateTime hentDatoSistRegistrert() {
        return metadata().datoSistRegistrert();
    }

    default String hentKilde() {
        return metadata().endringer().stream().
            max(Comparator.comparing(Endring::registrert))
            .map(Endring::kilde)
            .orElse(null);
    }
}
