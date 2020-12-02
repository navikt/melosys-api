package no.nav.melosys.integrasjon.joark;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.exception.*;

public interface JoarkFasade {

    /**
     * Ferdigstiller journalføring
     */
    void ferdigstillJournalføring(String journalpostId) throws FunksjonellException, IntegrasjonException;

    /**
     * Henter et dokument fra Joark
     */
    byte[] hentDokument(String journalPostID, String dokumentID) throws SikkerhetsbegrensningException, IkkeFunnetException;

    /**
     * Henter en journalpost fra Joark
     */
    Journalpost hentJournalpost(String journalpostID) throws IntegrasjonException, SikkerhetsbegrensningException;

    /**
     * Henter en liste med journalposter knyttet til en sak.
     */
    List<Journalpost> hentKjerneJournalpostListe(Long gsakSakID) throws IntegrasjonException, SikkerhetsbegrensningException;

    /**
     * Oppretter en journalpost  i Joark
     */
    String opprettJournalpost(OpprettJournalpost opprettJournalpost, boolean forsøkEndeligJfr) throws FunksjonellException;

    /**
     * Oppdaterer en journalpost og forsøker å ferdigstille hvis forsøkFerdigstill er satt
     */
    void oppdaterJournalpost(String journalpostID, JournalpostOppdatering journalpostOppdatering, boolean forsøkFerdigstill)
        throws SikkerhetsbegrensningException, TekniskException;

    LocalDate hentMottaksDatoForJournalpost(String journalpostID) throws SikkerhetsbegrensningException, IntegrasjonException;
}
