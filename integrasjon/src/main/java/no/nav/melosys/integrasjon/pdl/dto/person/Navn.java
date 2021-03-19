package no.nav.melosys.integrasjon.pdl.dto.person;

import no.nav.melosys.integrasjon.pdl.dto.Metadata;

public record Navn(String fornavn, String mellomnavn, String etternavn, Metadata metadata) {
}
