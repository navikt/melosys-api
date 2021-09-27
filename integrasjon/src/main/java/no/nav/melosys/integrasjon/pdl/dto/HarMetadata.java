package no.nav.melosys.integrasjon.pdl.dto;

import java.time.LocalDateTime;
import java.util.Comparator;

public interface HarMetadata {
    Metadata metadata();

    default boolean erGyldigFør(LocalDateTime tidspunkt) {
        return metadata().endringer().stream()
            .filter(e -> e.registrert().isBefore(tidspunkt))
            .max(Comparator.comparing(Endring::registrert))
            .filter(Endring::erIkkeOpphør)
            .isPresent();
    }

    default boolean erGyldig() {
        return metadata().endringer().stream()
            .max(Comparator.comparing(Endring::registrert))
            .filter(Endring::erIkkeOpphør)
            .isPresent();
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
