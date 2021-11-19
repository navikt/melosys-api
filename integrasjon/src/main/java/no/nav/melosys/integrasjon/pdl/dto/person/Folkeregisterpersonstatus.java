package no.nav.melosys.integrasjon.pdl.dto.person;

import java.time.LocalDateTime;

import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;

public record Folkeregisterpersonstatus(
    LocalDateTime gyldigFraOgMed,
    LocalDateTime gyldigTilOgMed,
    String status,
    Metadata metadata
) implements HarMetadata {
}
