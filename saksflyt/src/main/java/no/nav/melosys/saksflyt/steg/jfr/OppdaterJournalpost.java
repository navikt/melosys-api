package no.nav.melosys.saksflyt.steg.jfr;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.JournalfoeringMangel;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.feil.Feilkategori;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.JFR_FERDIGSTILL_JOURNALPOST;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.JFR_OPPDATER_JOURNALPOST;

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
    @SuppressWarnings("unchecked")
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        log.debug("Starter behandling av prosessinstans {}", prosessinstans.getId());

        boolean medDokumentkategori = false;
        String journalpostID = prosessinstans.getData(JOURNALPOST_ID);
        List<JournalfoeringMangel> mangler = joarkFasade.utledJournalfoeringsbehov(journalpostID);
        if (mangler.contains(JournalfoeringMangel.HOVEDDOKUMENT_KATEGORI)) {
            medDokumentkategori = true;
        }

        Behandlingstyper behandlingstype = prosessinstans.getData(BEHANDLINGSTYPE, Behandlingstyper.class);
        Long arkivSakID = null;
        if (prosessinstans.getType() == ProsessType.JFR_KNYTT || Behandlingstyper.ENDRET_PERIODE.equals(behandlingstype)) {
            Fagsak sak = fagsakRepo.findBySaksnummer(prosessinstans.getData(SAKSNUMMER));
            if (sak != null) {
                arkivSakID = sak.getGsakSaksnummer();
            }
        } else {
            arkivSakID = prosessinstans.getData(GSAK_SAK_ID, Long.class);
        }
        if (arkivSakID == null) {
            String feilmelding = "Prosessinstansen er ikke knyttet til en gsak sak";
            log.error("{}: {}", prosessinstans.getId(), feilmelding);
            håndterUnntak(Feilkategori.TEKNISK_FEIL, prosessinstans, feilmelding, null);
            return;
        }

        String brukerID = prosessinstans.getData(BRUKER_ID);
        String avsenderID = prosessinstans.getData(AVSENDER_ID);
        String avsenderNavn = prosessinstans.getData(AVSENDER_NAVN);
        String avsenderLand = prosessinstans.getData(AVSENDER_LAND);
        Avsendertyper avsenderType = prosessinstans.getData(AVSENDER_TYPE, Avsendertyper.class);
        if (avsenderNavn == null) {
            if (avsenderID == null) {
                throw new FunksjonellException("Både avsenderID og AvsenderNavn er null. AvsenderNavn er påkrevd for å journalføre.");
            }
            avsenderNavn = avsenderID; //Avsendernavn er påkrevd
        }
        String tittel = prosessinstans.getData(HOVEDDOKUMENT_TITTEL);
        String hovedDokumentID = prosessinstans.getData(DOKUMENT_ID);
        LocalDate mottattDato = prosessinstans.getData(MOTTATT_DATO, LocalDate.class);

        List<String> logiskeVedleggTitler = prosessinstans.getData(LOGISKE_VEDLEGG_TITLER, List.class);
        Map<String, String> fysiskeVedleggMedTitler = prosessinstans.getData(FYSISKE_VEDLEGG, Map.class);

        JournalpostOppdatering journalpostOppdatering = new JournalpostOppdatering.Builder().medArkivSakID(arkivSakID)
            .medBrukerID(brukerID).medHovedDokumentID(hovedDokumentID)
            .medAvsenderID(avsenderID).medAvsenderNavn(avsenderNavn).medAvsenderType(avsenderType).medAvsenderLand(avsenderLand)
            .medTittel(tittel)
            .medMottatDato(mottattDato)
            .medFysiskeVedlegg(fysiskeVedleggMedTitler)
            .medLogiskeVedleggTitler(logiskeVedleggTitler).medDokumentkategori(medDokumentkategori).build();
        joarkFasade.oppdaterJournalpost(journalpostID, journalpostOppdatering, false);

        prosessinstans.setSteg(JFR_FERDIGSTILL_JOURNALPOST);
        log.info("Prosessinstans {} har oppdatert journalpost {}. SakId: {}", prosessinstans.getId(), journalpostID, arkivSakID);
    }
}