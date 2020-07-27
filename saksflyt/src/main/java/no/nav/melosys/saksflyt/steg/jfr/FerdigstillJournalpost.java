package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.JOURNALPOST_ID;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.*;

/**
 * Ferdigstiller en journalpost i Joark.
 *
 * Transisjoner:
 * 1) ProsessType.JFR_NY_SAK:
 *     JFR_FERDIGSTILL_JOURNALPOST -> JFR_HENT_PERS_OPPL eller FEILET_MASKINELT hvis feil
 * 2) ProsessType.JFR_KNYTT:
 *     JFR_FERDIGSTILL_JOURNALPOST -> JFR_SETT_VURDER_DOKUMENT eller FEILET_MASKINELT hvis feil
 * 3) ProsessType.JFR_KNYTT && Behandlingstyper.ENDRET_PERIODE:
 *     JFR_FERDIGSTILL_JOURNALPOST -> REPLIKER_BEHANDLING eller FEILET_MASKINELT hvis feil
 */
@Component
public class FerdigstillJournalpost implements StegBehandler {

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
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        ProsessType type = prosessinstans.getType();

        String journalpostID = prosessinstans.getData(JOURNALPOST_ID);
        Behandlingstyper behandlingstype = prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class);

        joarkFasade.ferdigstillJournalføring(journalpostID);

        if (type == ProsessType.JFR_NY_BEHANDLING && behandlingstype == Behandlingstyper.ENDRET_PERIODE) {
            prosessinstans.setSteg(REPLIKER_BEHANDLING);
        } else if (type == ProsessType.JFR_NY_SAK || type == ProsessType.JFR_NY_BEHANDLING) {
            prosessinstans.setSteg(JFR_HENT_REGISTER_OPPL);
        } else if (type == ProsessType.JFR_KNYTT) {
            prosessinstans.setSteg(JFR_SETT_VURDER_DOKUMENT);
        } else {
            throw new TekniskException("Ukjent prosesstype: " + type);
        }

        log.info("Prosessinstans {} har ferdigstillt journalpost {}", prosessinstans.getId(), journalpostID);
    }

}
