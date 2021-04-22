package no.nav.melosys.integrasjon.joark.saf.dto.journalpost;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HentJournalpostResponse(@JsonProperty("query") Journalpost journalpost) {
}
