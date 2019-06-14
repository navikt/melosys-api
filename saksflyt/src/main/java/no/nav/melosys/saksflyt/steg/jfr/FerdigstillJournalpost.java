package no.nav.melosys.saksflyt.steg.jfr;

import java.util.Map;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.steg.UnntakBehandler;
import no.nav.melosys.saksflyt.steg.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.JOURNALPOST_ID;
import static no.nav.melosys.domain.ProsessSteg.*;

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
    public void utfør(Prosessinstans prosessinstans) throws IntegrasjonException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        ProsessType type = prosessinstans.getType();

        String journalpostID = prosessinstans.getData(JOURNALPOST_ID);
        Behandlingstyper behandlingstype = prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class);

        joarkFasade.ferdigstillJournalføring(journalpostID);

        if (type == ProsessType.JFR_NY_BEHANDLING && behandlingstype == Behandlingstyper.ENDRET_PERIODE) {
            prosessinstans.setSteg(REPLIKER_BEHANDLING);
        } else if (type == ProsessType.JFR_NY_SAK || type == ProsessType.JFR_NY_BEHANDLING) {
            prosessinstans.setSteg(JFR_HENT_PERS_OPPL);
        } else if (type == ProsessType.JFR_KNYTT) {
            prosessinstans.setSteg(JFR_SETT_VURDER_DOKUMENT);
        } else {
            String feilmelding = "Ukjent prosess type: " + type;
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        log.info("Prosessinstans {} har ferdigstillt journalpost {}", prosessinstans.getId(), journalpostID);
    }

}
