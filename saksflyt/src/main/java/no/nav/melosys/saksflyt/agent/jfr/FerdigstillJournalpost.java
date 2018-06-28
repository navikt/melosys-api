package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Map;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.JOURNALPOST_ID;
import static no.nav.melosys.domain.ProsessSteg.JFR_FERDIGSTILL_JOURNALPOST;
import static no.nav.melosys.domain.ProsessSteg.JFR_HENT_PERS_OPPL;

/**
 * Ferdigstiller en journalpost i Joark.
 *
 * Transisjoner:
 * JFR_FERDIGSTILL_JOURNALPOST -> JFR_HENT_PERS_OPPL eller FEILET_MASKINELT hvis feil
 */
@Component
public class FerdigstillJournalpost extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(FerdigstillJournalpost.class);

    JoarkFasade joarkFasade;

    @Autowired
    public FerdigstillJournalpost(JoarkFasade joarkFasade) {
        this.joarkFasade = joarkFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_FERDIGSTILL_JOURNALPOST;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Override
    public void utførSteg(Prosessinstans prosessinstans) {
        String journalpostID = prosessinstans.getData(JOURNALPOST_ID);
        try {
            joarkFasade.ferdigstillJournalføring(journalpostID);
        } catch (SikkerhetsbegrensningException e) {
            log.error("Feil i steg", e);
            // FIXME: MELOSYS-1316
        }

        prosessinstans.setSteg(JFR_HENT_PERS_OPPL);
    }
}
