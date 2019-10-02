package no.nav.melosys.integrasjon.joark.journalpostapi;

import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostRequest;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostResponse;

public interface JournalpostapiConsumer {

    OpprettJournalpostResponse opprettJournalpost(OpprettJournalpostRequest request, boolean forsøkEndeligJfr);
}
