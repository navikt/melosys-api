package no.nav.melosys.saksflyt.agent.jfr;

import java.util.List;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.joark.JournalfoeringMangel;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.agent.StandardAbstraktAgent;
import no.nav.melosys.saksflyt.api.Binge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.ProsessDataKey.*;
import static no.nav.melosys.domain.ProsessSteg.JFR_FERDIGSTILL_JOURNALPOST;
import static no.nav.melosys.domain.ProsessSteg.JFR_OPPDATER_JOURNALPOST;

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
        boolean medDokumentkategori = false;

        String journalpostID = prosessinstans.getData(JOURNALPOST_ID);
        try {
            List<JournalfoeringMangel> mangler = joarkFasade.utledJournalfoeringsbehov(journalpostID);
            if (mangler.contains(JournalfoeringMangel.HOVEDDOKUMENT_KATEGORI)) {
                medDokumentkategori = true;
            }
        } catch (SikkerhetsbegrensningException e) {
            log.error("Feil i steg {}", inngangsSteg(), e);
            håndterFeil(prosessinstans, false);
        }

        String gsakSakID = prosessinstans.getData(GSAK_SAK_ID);
        String brukerID = prosessinstans.getData(BRUKER_ID);
        String avsenderID = prosessinstans.getData(AVSENDER_ID);
        String avsenderNavn = prosessinstans.getData(AVSENDER_NAVN);
        String tittel = prosessinstans.getData(HOVEDDOKUMENT_TITTEL);
        String dokumentID = prosessinstans.getData(DOKUMENT_ID);

        try {
            joarkFasade.oppdaterJounalpost(journalpostID, dokumentID, gsakSakID, brukerID, avsenderID, avsenderNavn, tittel, medDokumentkategori);
        } catch (SikkerhetsbegrensningException e) {
            log.error("Feil i steg {}", inngangsSteg(), e);
            håndterFeil(prosessinstans, false);
        }

        prosessinstans.setSteg(JFR_FERDIGSTILL_JOURNALPOST);
    }
}

