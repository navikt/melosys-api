package no.nav.melosys.saksflyt.agent.jfr;

import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.joark.JournalfoeringMangel;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.agent.UnntakBehandler;
import no.nav.melosys.saksflyt.agent.unntak.FeilStrategi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
public class OppdaterJournalpost extends AbstraktStegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterJournalpost.class);

    private final JoarkFasade joarkFasade;
    private final FagsakRepository fagsakRepo;

    @Autowired
    public OppdaterJournalpost(JoarkFasade joarkFasade, FagsakRepository fagsakRepo) {
        this.joarkFasade = joarkFasade;
        this.fagsakRepo = fagsakRepo;
        log.info("OppdaterJournalpost initialisert");
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return JFR_OPPDATER_JOURNALPOST;
    }

    @Override
    protected Map<Feilkategori, UnntakBehandler> unntaksHåndtering() {
        return FeilStrategi.standardFeilHåndtering();
    }
    
    @Transactional
    @Override
    public void utfør(Prosessinstans prosessinstans) throws IntegrasjonException, IkkeFunnetException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        boolean medDokumentkategori = false;
        String journalpostID = prosessinstans.getData(JOURNALPOST_ID);
        List<JournalfoeringMangel> mangler = joarkFasade.utledJournalfoeringsbehov(journalpostID);
        if (mangler.contains(JournalfoeringMangel.HOVEDDOKUMENT_KATEGORI)) {
            medDokumentkategori = true;
        }

        Long gsakSakID = null;
        if (prosessinstans.getType() == ProsessType.JFR_KNYTT) {
            Fagsak sak = fagsakRepo.findBySaksnummer(prosessinstans.getData(SAKSNUMMER));
            if (sak != null) {
                gsakSakID = sak.getGsakSaksnummer();
            }
        } else {
            gsakSakID = prosessinstans.getData(GSAK_SAK_ID, Long.class);
        }
        if (gsakSakID == null) {
            String feilmelding = "Prosessinstansen er ikke knyttet til en gsak sak";
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        String brukerID = prosessinstans.getData(BRUKER_ID);
        String avsenderID = prosessinstans.getData(AVSENDER_ID);
        String avsenderNavn = prosessinstans.getData(AVSENDER_NAVN);
        if (avsenderID == null) {
            avsenderID = avsenderNavn; //FIXME trenger en avklaring MELOSYS-1353
        }
        if (avsenderNavn == null) {
            avsenderNavn = avsenderID; //FIXME trenger en avklaring MELOSYS-1353
        }
        String tittel = prosessinstans.getData(HOVEDDOKUMENT_TITTEL);
        String dokumentID = prosessinstans.getData(DOKUMENT_ID);

        joarkFasade.oppdaterJounalpost(journalpostID, dokumentID, gsakSakID, brukerID, avsenderID, avsenderNavn, tittel, medDokumentkategori);

        prosessinstans.setSteg(JFR_FERDIGSTILL_JOURNALPOST);
        log.info("Oppdatert journalpost for prosessinstans {}", prosessinstans.getId());
    }
}

