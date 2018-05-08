package no.nav.melosys.integrasjon.joark;

import no.nav.melosys.domain.Journalpost;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;

public interface JoarkFasade {
    /**
     * Henter et dokument i joark
     */
    byte[] hentDokument(String journalPostID, String dokumentID) throws IntegrasjonException, SikkerhetsbegrensningException;

    /**
     * Henter en journalpost fra joark
     */
    Journalpost hentJournalpost(String journalpostID) throws SikkerhetsbegrensningException;
}
