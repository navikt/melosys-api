package no.nav.melosys.integrasjon.pdl.dto.person;

import no.nav.melosys.integrasjon.pdl.dto.HarMetadata;
import no.nav.melosys.integrasjon.pdl.dto.Metadata;

import java.time.LocalDate;

public record Foedselsdato(LocalDate foedselsdato, Integer foedselsaar, Metadata metadata) implements HarMetadata {}
