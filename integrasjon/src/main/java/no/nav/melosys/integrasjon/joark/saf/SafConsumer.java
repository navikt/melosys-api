package no.nav.melosys.integrasjon.joark.saf;

import no.nav.melosys.integrasjon.felles.RestConsumer;
import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Journalpost;

public interface SafConsumer extends RestConsumer {
    byte[] hentDokument(String journalpostID, String dokumentID);
    Journalpost hentJournalpost(String journalpostID);
}
