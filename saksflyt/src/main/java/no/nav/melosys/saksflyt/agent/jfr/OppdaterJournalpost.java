package no.nav.melosys.saksflyt.agent.jfr;

import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.arkiv.JournalfoeringMangel;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
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

    @Override
    @SuppressWarnings("unchecked")
    public void utfør(Prosessinstans prosessinstans) throws IntegrasjonException, IkkeFunnetException, FunksjonellException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        boolean medDokumentkategori = false;
        String journalpostID = prosessinstans.getData(JOURNALPOST_ID);
        List<JournalfoeringMangel> mangler = joarkFasade.utledJournalfoeringsbehov(journalpostID);
        if (mangler.contains(JournalfoeringMangel.HOVEDDOKUMENT_KATEGORI)) {
            medDokumentkategori = true;
        }

        Behandlingstyper behandlingstype = prosessinstans.getData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.class);
        Long gsakSakID = null;
        if (prosessinstans.getType() == ProsessType.JFR_KNYTT || Behandlingstyper.ENDRET_PERIODE.equals(behandlingstype)) {
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
        if (avsenderNavn == null) {
            if (avsenderID == null) {
                throw new FunksjonellException("Både avsenderID og AvsenderNavn er null. AvsenderNavn er påkrevd for å journalføre.");
            }
            avsenderNavn = avsenderID; //Avsendernavn er påkrevd
        }
        String tittel = prosessinstans.getData(HOVEDDOKUMENT_TITTEL);
        String dokumentID = prosessinstans.getData(DOKUMENT_ID);

        List<String> vedleggTittelListe = prosessinstans.getData(VEDLEGG_TITTEL_LISTE, List.class);

        joarkFasade.oppdaterJournalpost(journalpostID, dokumentID, gsakSakID, brukerID, avsenderID, avsenderNavn, tittel, vedleggTittelListe, medDokumentkategori);

        prosessinstans.setSteg(JFR_FERDIGSTILL_JOURNALPOST);
        log.info("Prosessinstans {} har oppdatert journalpost {}. SakId: {}", prosessinstans.getId(), journalpostID, gsakSakID);
    }
}

