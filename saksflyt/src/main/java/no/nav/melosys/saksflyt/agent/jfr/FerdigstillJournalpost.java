package no.nav.melosys.saksflyt.agent.jfr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.agent.StandardAbstraktAgent;
import no.nav.melosys.saksflyt.api.Binge;

import static no.nav.melosys.domain.ProsessDataKey.JOURNALPOST_ID;
import static no.nav.melosys.domain.ProsessSteg.JFR_HENT_PERS_OPPL;
import static no.nav.melosys.domain.ProsessSteg.JFR_FERDIGSTILL_JOURNALPOST;

/**
 * Ferdigstiller en journalpost i Joark.
 *
 * Transisjoner:
 * JFR_FERDIGSTILL_JOURNALPOST -> JFR_HENT_PERS_OPPL eller FEILET_MASKINELT hvis feil
 */
@Component
public class FerdigstillJournalpost extends StandardAbstraktAgent {

    private static final Logger log = LoggerFactory.getLogger(FerdigstillJournalpost.class);

    JoarkFasade joarkFasade;

    @Autowired
    public FerdigstillJournalpost(Binge binge, ProsessinstansRepository prosessinstansRepo, JoarkFasade joarkFasade) {
        super(binge, prosessinstansRepo);
        this.joarkFasade = joarkFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_FERDIGSTILL_JOURNALPOST;
    }

    @Override
    public void utførSteg(Prosessinstans prosessinstans) {
        String journalpostID = prosessinstans.getData(JOURNALPOST_ID);
        try {
            joarkFasade.ferdigstillJournalføring(journalpostID);
        } catch (SikkerhetsbegrensningException e) {
            log.error("Feil i steg {}", inngangsSteg(), e);
            håndterFeil(prosessinstans, false);
        }

        prosessinstans.setSteg(JFR_HENT_PERS_OPPL);
    }
}
