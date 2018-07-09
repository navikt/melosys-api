package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Map;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
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
 * 1) ProsessType.JFR_NY_SAK:
 *     JFR_FERDIGSTILL_JOURNALPOST -> JFR_HENT_PERS_OPPL eller FEILET_MASKINELT hvis feil
 * 2) ProsessType.JFR_KNYTT:
 *     JFR_FERDIGSTILL_JOURNALPOST -> null eller FEILET_MASKINELT hvis feil
 */
@Component
public class FerdigstillJournalpost extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(FerdigstillJournalpost.class);

    private final JoarkFasade joarkFasade;

    @Autowired
    public FerdigstillJournalpost(JoarkFasade joarkFasade) {
        this.joarkFasade = joarkFasade;
        log.info("FerdigstillJournalpost initialisert");
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
    public void utfør(Prosessinstans prosessinstans) throws SikkerhetsbegrensningException {
        log.debug("Starter behandling av {}", prosessinstans.getId());

        ProsessType type = prosessinstans.getType();
        String journalpostID = prosessinstans.getData(JOURNALPOST_ID);
        joarkFasade.ferdigstillJournalføring(journalpostID);

        if (type == ProsessType.JFR_NY_SAK) {
            prosessinstans.setSteg(JFR_HENT_PERS_OPPL);
        } else if (type == ProsessType.JFR_KNYTT) {
            prosessinstans.setSteg(null);
        } else {
            String feilmelding = "Ukjent prosess type: " + type;
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, null);
            return;
        }
    }

}
