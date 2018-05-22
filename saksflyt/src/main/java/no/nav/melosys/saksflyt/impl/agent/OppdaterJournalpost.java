package no.nav.melosys.saksflyt.impl.agent;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessSteg.JFR_FERDIGSTILL_JOURNALPOST;
import static no.nav.melosys.domain.ProsessSteg.JFR_OPPDATER_JOURNALPOST;
import static no.nav.melosys.domain.ProsessDataKey.AVSENDER_ID;
import static no.nav.melosys.domain.ProsessDataKey.AVSENDER_NAVN;
import static no.nav.melosys.domain.ProsessDataKey.BRUKER_ID;
import static no.nav.melosys.domain.ProsessDataKey.GSAK_SAK_ID;
import static no.nav.melosys.domain.ProsessDataKey.HOVEDDOKUMENT_TITTEL;
import static no.nav.melosys.domain.ProsessDataKey.JOURNALPOST_ID;

/**
 * Oppdaterer en journalpost i Joark.
 *
 * Transisjoner:
 * JFR_OPPDATER_JOURNALPOST -> JFR_FERDIGSTILL_JOURNALPOST eller FEILET_MASKINELT hvis feil
 */
@Component
public class OppdaterJournalpost extends StandardAbstraktAgent {

    private static final Logger log = LoggerFactory.getLogger(OppdaterJournalpost.class);

    JoarkFasade joarkFasade;

    @Autowired
    public OppdaterJournalpost(Binge binge, ProsessinstansRepository prosessinstansRepo, JoarkFasade joarkFasade) {
        super(binge, prosessinstansRepo);
        this.joarkFasade = joarkFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_OPPDATER_JOURNALPOST;
    }

    @Override
    public void utførSteg(Prosessinstans prosessinstans) {
        String journalpostID = prosessinstans.getData(JOURNALPOST_ID);
        String gsakSakID = prosessinstans.getData(GSAK_SAK_ID);
        String brukerID = prosessinstans.getData(BRUKER_ID);
        String avsenderID = prosessinstans.getData(AVSENDER_ID);
        String avsenderNavn = prosessinstans.getData(AVSENDER_NAVN);
        String tittel = prosessinstans.getData(HOVEDDOKUMENT_TITTEL);

        try {
            joarkFasade.oppdaterJounalpost(journalpostID, gsakSakID, brukerID, avsenderID, avsenderNavn, tittel);
        } catch (SikkerhetsbegrensningException e) {
            log.error("Feil i steg {}", inngangsSteg(), e);
            håndterFeil(prosessinstans, false);
        }

        prosessinstans.setSteg(JFR_FERDIGSTILL_JOURNALPOST);
    }
}

