package no.nav.melosys.integrasjon.doksys;

import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.DistribuerJournalpostConsumer;
import no.nav.melosys.integrasjon.doksys.dokumentproduksjon.DokumentproduksjonConsumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class DoksysSystemService extends DoksysService implements DoksysFasade {

    DoksysSystemService(@Qualifier("system") DokumentproduksjonConsumer dokumentproduksjonConsumer, DistribuerJournalpostConsumer distribuerJournalpostConsumer) {
        super(dokumentproduksjonConsumer, distribuerJournalpostConsumer);
    }
}
