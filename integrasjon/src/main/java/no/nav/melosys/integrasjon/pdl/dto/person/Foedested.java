package no.nav.melosys.integrasjon.pdl.dto.person;

import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;

public record Foedested(
    String foedeland,
    String foedested,
    Metadata metadata
) implements HarMetadata {}
