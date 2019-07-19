package no.nav.melosys.integrasjon.joark;

import java.util.List;

import no.nav.melosys.domain.arkiv.JournalfoeringMangel;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;

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
     * Oppdaterer en journalpost i Joark
     */
    void oppdaterJournalpost(String journalpostID, String hovedDokumentID, JournalpostOppdatering journalpostOppdatering)
        throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException;

    /**
     * Returnerer en liste av mangler i journalposten med den oppgitte IDen
     */
    List<JournalfoeringMangel> utledJournalfoeringsbehov(String journalpostID) throws FunksjonellException;
}
