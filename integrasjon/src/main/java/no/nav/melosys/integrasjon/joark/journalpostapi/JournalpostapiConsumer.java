package no.nav.melosys.integrasjon.joark.journalpostapi;

import no.nav.melosys.integrasjon.joark.journalpostapi.dto.FerdigstillJournalpostRequest;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OppdaterJournalpostRequest;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostRequest;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostResponse;

public interface JournalpostapiConsumer {

    OpprettJournalpostResponse opprettJournalpost(OpprettJournalpostRequest request, boolean forsøkEndeligJfr);

    void oppdaterJournalpost(OppdaterJournalpostRequest request, String journalpostId);

    void leggTilLogiskVedlegg(String dokumentID, String tittel);

    void fjernLogiskeVedlegg(String dokumentInfoId, String logiskVedleggId);

    void ferdigstillJournalpost(FerdigstillJournalpostRequest request, String journalpostId);
}
