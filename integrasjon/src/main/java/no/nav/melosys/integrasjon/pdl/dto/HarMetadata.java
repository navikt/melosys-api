package no.nav.melosys.integrasjon.pdl.dto;

import java.util.Comparator;

public interface HarMetadata {
    Metadata metadata();

    default boolean erOpphørt() {
        return metadata().endringer().stream().max(Comparator.comparing(Endring::registrert))
            .filter(Endring::erOpphør).isPresent();
    }
}
