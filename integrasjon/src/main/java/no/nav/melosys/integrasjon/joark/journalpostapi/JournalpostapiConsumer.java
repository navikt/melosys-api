package no.nav.melosys.integrasjon.joark.journalpostapi;

import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.FerdigstillJournalpostRequest;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OppdaterJournalpostRequest;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostRequest;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostResponse;

public interface JournalpostapiConsumer {

    OpprettJournalpostResponse opprettJournalpost(OpprettJournalpostRequest request, boolean forsøkEndeligJfr);

    void oppdaterJournalpost(OppdaterJournalpostRequest request, String journalpostId) throws SikkerhetsbegrensningException, IntegrasjonException;

    void leggTilLogiskVedlegg(String dokumentID, String tittel) throws SikkerhetsbegrensningException, IntegrasjonException;

    void ferdigstillJournalpost(FerdigstillJournalpostRequest request, String journalpostId) throws SikkerhetsbegrensningException, IntegrasjonException;
}
