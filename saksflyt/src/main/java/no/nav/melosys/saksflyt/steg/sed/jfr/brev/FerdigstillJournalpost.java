package no.nav.melosys.saksflyt.steg.sed.jfr.brev;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("JournalførAouBrevFerdigstillJournalpost")
public class FerdigstillJournalpost extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(FerdigstillJournalpost.class);

    private final JoarkFasade joarkFasade;

    @Autowired
    public FerdigstillJournalpost(JoarkFasade joarkFasade) {
        this.joarkFasade = joarkFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_AOU_BREV_FERDIGSTILL_JOURNALPOST;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        String journalpostId = prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID);
        joarkFasade.oppdaterJournalpostMedSaksnummerOgBruker(
            journalpostId,
            prosessinstans.getData(ProsessDataKey.BRUKER_ID),
            prosessinstans.getData(ProsessDataKey.GSAK_SAK_ID, Long.class),
            true
        );

        log.info("Journalpost {} ferdigstilt for gsak-sak {}", journalpostId, prosessinstans.getData(ProsessDataKey.GSAK_SAK_ID));
        prosessinstans.setSteg(ProsessSteg.JFR_AOU_BREV_OPPRETT_SEDDOKUMENT);
    }
}
