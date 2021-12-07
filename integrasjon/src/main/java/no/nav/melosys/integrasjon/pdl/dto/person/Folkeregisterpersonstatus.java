package no.nav.melosys.integrasjon.pdl.dto.person;

import no.nav.melosys.integrasjon.pdl.dto.Folkeregistermetadata;
import no.nav.melosys.integrasjon.pdl.dto.HarFolkeregistermetadata;
import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;

public record Folkeregisterpersonstatus(
    String status,
    Metadata metadata,
    Folkeregistermetadata folkeregistermetadata
) implements HarMetadata, HarFolkeregistermetadata {
}
