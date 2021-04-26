package no.nav.melosys.integrasjon.joark.saf.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Journalpost;

public record HentJournalpostResponse(@JsonProperty("query") Journalpost journalpost) {
}
