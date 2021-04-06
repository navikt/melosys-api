package no.nav.melosys.integrasjon.joark.saf;

import no.nav.melosys.integrasjon.felles.RestConsumer;

public interface SafConsumer extends RestConsumer {
    byte[] hentDokument(String journalpostID, String dokumentID);
}
