package no.nav.melosys.saksflyt.steg.brev;

import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID;
import static no.nav.melosys.domain.saksflyt.ProsessDataKey.DISTRIBUER_OVERSTYR_MOTTAKER;
import static org.springframework.util.StringUtils.isEmpty;

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

        String journalpostId = prosessinstans.getData(DISTRIBUERBAR_JOURNALPOST_ID);
        StrukturertAdresse overstyrtMottaker = prosessinstans.getData(DISTRIBUER_OVERSTYR_MOTTAKER, StrukturertAdresse.class, null);

        if (isEmpty(journalpostId)) {
            throw new FunksjonellException("JournalpostId mangler, kan ikke distribuere");
        }

        String bestillingsId;
        if (overstyrtMottaker != null) {
            //NOTE Implementer når nødvendig
            bestillingsId = doksysFasade.distribuerJournalpost(journalpostId, overstyrtMottaker);
        } else {
            bestillingsId = doksysFasade.distribuerJournalpost(journalpostId);
        }
        log.info("Distribuering av journalpostId {} bestilt med bestillingsId {}", journalpostId, bestillingsId);
    }
}
