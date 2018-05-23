package no.nav.melosys.integrasjon.joark;

import java.util.List;

import no.nav.melosys.domain.Journalpost;
import no.nav.melosys.domain.joark.JournalfoeringMangel;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;

public interface JoarkFasade {
    /**
     * Ferdigstiller journalføring
     */
    void ferdigstillJournalføring(String journalpostId) throws SikkerhetsbegrensningException;

    /**
     * Henter et dokument fra Joark
     */
    byte[] hentDokument(String journalPostID, String dokumentID) throws IntegrasjonException, SikkerhetsbegrensningException;

    /**
     * Henter en journalpost fra Joark
     */
    Journalpost hentJournalpost(String journalpostID) throws SikkerhetsbegrensningException;

    /**
     * Oppdaterer en journalpost i Joark
     * @param medDokumentkategori Om dokumentkategori skal oppdatteres med standardverdi "IS", Ikke tolkbart skjema
     */
    void oppdaterJounalpost(String journalpostId, String gsakSaksnummer, String brukerID, String avsenderID, String avsenderNavn, String tittel, boolean medDokumentkategori)
        throws SikkerhetsbegrensningException;

    /**
     * Returnerer en liste av mangler i journalposten med den oppgitte IDen
     */
    List<JournalfoeringMangel> utledJournalfoeringsbehov(String journalpostID) throws SikkerhetsbegrensningException;
}
