package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static org.springframework.util.StringUtils.hasText;

@Component
public class DistribuerJournalpost implements StegBehandler {
    private static final Logger log = LoggerFactory.getLogger(DistribuerJournalpost.class);

    private final DoksysFasade doksysFasade;

    public DistribuerJournalpost(DoksysFasade doksysFasade) {
        this.doksysFasade = doksysFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.DISTRIBUER_JOURNALPOST;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {

        String journalpostId = prosessinstans.getData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID);

        //TODO Mangler flagg for å overstyre mottaker
        if (hasText(journalpostId)) {
            String bestillingsId = doksysFasade.distribuerJournalpost(journalpostId);
            log.info("Distribuering av journalpostId {} bestilt med bestillingsId {}", journalpostId, bestillingsId);
        }
    }
}
