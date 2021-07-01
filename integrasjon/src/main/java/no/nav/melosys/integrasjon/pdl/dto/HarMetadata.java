package no.nav.melosys.integrasjon.pdl.dto;

import java.time.LocalDateTime;
import java.util.Comparator;

public interface HarMetadata {
    Metadata metadata();

    default boolean erOpphørt() {
        return metadata().endringer().stream().max(Comparator.comparing(Endring::registrert))
            .filter(Endring::erOpphør).isPresent();
    }

    default LocalDateTime hentDatoSistRegistrert() {
        return metadata().datoSistRegistrert();
    }

    default String hentKilde() {
        return metadata().endringer().stream().max(Comparator.comparing(Endring::registrert)).map(Endring::kilde)
            .orElseThrow(() -> new IllegalArgumentException("Kilde er obligatorisk i PDL-endringer."));
    }
}
