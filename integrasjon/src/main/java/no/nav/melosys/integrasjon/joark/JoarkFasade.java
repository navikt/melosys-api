package no.nav.melosys.integrasjon.joark;

import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;

public interface JoarkFasade {
    /**
     * Henter et dokument i JOARK
     */
    byte[] hentDokument(String journalPostID, String dokumentID) throws IntegrasjonException, SikkerhetsbegrensningException;
}
