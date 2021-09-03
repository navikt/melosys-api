package no.nav.melosys.integrasjon.pdl.dto.person;

import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;

public record Foreldreansvar(String ansvar, Metadata metadata) implements HarMetadata {
}
