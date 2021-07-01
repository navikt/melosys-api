package no.nav.melosys.integrasjon.pdl.dto.person;

import java.time.LocalDate;

import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;

public record Doedsfall(LocalDate doedsdato, Metadata metadata) implements HarMetadata {
}
