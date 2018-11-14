package no.nav.melosys.integrasjon.joark;

import java.util.List;

import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.JournalfoeringMangel;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;

public interface JoarkFasade {

    /**
     * Ferdigstiller journalføring
     */
    void ferdigstillJournalføring(String journalpostId) throws SikkerhetsbegrensningException, IkkeFunnetException, FunksjonellException;

    /**
     * Henter et dokument fra Joark
     */
    byte[] hentDokument(String journalPostID, String dokumentID) throws SikkerhetsbegrensningException, IkkeFunnetException;

    /**
     * Henter en journalpost fra Joark
     */
    Journalpost hentJournalpost(String journalpostID) throws SikkerhetsbegrensningException, IkkeFunnetException, FunksjonellException, IntegrasjonException;

    /**
     * Henter en liste med journalposter knyttet til en sak.
     */
    List<Journalpost> hentKjerneJournalpostListe(Long gsakSakID) throws SikkerhetsbegrensningException, IntegrasjonException;

    /**
     * Oppdaterer en journalpost i Joark
     * @param medDokumentkategori Om dokumentkategori skal oppdatteres med standardverdi "IS", Ikke tolkbart skjema
     */
    void oppdaterJounalpost(String journalpostId, String dokumentID, Long gsakSaksnummer, String brukerID, String avsenderID, String avsenderNavn, String tittel, boolean medDokumentkategori)
        throws SikkerhetsbegrensningException, IkkeFunnetException, FunksjonellException;

    /**
     * Returnerer en liste av mangler i journalposten med den oppgitte IDen
     */
    List<JournalfoeringMangel> utledJournalfoeringsbehov(String journalpostID) throws SikkerhetsbegrensningException, IkkeFunnetException, FunksjonellException;
}
