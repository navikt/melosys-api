package no.nav.melosys.integrasjon.pdl.dto.person;

import java.time.LocalDate;

import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;

public record Foedselsdato(LocalDate foedselsdato,
                           Integer foedselsaar,
                           Metadata metadata) implements HarMetadata {
}
