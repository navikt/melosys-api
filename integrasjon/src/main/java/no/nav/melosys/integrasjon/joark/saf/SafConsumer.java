package no.nav.melosys.integrasjon.joark.saf;

import java.util.Collection;

import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.Journalpost;

public interface SafConsumer  {
    byte[] hentDokument(String journalpostID, String dokumentID);
    Journalpost hentJournalpost(String journalpostID);
    Collection<Journalpost> hentDokumentoversikt(String saksnummer);
}
