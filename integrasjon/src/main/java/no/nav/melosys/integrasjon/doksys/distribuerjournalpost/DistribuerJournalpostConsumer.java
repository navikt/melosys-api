package no.nav.melosys.integrasjon.doksys.distribuerjournalpost;

import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostRequest;
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostResponse;

public interface DistribuerJournalpostConsumer {

    DistribuerJournalpostResponse distribuerJournalpost(DistribuerJournalpostRequest request);
}
