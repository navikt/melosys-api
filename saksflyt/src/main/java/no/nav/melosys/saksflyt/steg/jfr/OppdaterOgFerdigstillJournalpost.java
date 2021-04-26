package no.nav.melosys.saksflyt.steg.jfr;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.sak.FagsakService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPDATER_OG_FERDIGSTILL_JOURNALPOST;

@Component
public class OppdaterOgFerdigstillJournalpost implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterOgFerdigstillJournalpost.class);

    private final JoarkFasade joarkFasade;
    private final FagsakService fagsakService;

    @Autowired
    public OppdaterOgFerdigstillJournalpost(JoarkFasade joarkFasade, FagsakService fagsakService) {
        this.joarkFasade = joarkFasade;
        this.fagsakService = fagsakService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPDATER_OG_FERDIGSTILL_JOURNALPOST;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {
        String journalpostID = prosessinstans.getData(JOURNALPOST_ID);

        Long arkivSakID;
        if (prosessinstans.getBehandling() == null) {
            arkivSakID = fagsakService.hentFagsak(prosessinstans.getData(SAKSNUMMER)).getGsakSaksnummer();
        } else {
            arkivSakID = prosessinstans.getBehandling().getFagsak().getGsakSaksnummer();
        }
        if (arkivSakID == null) {
            throw new TekniskException("Prosessinstansen er ikke knyttet til en nav-sak");
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
            .medMottattDato(mottattDato)
            .medFysiskeVedlegg(fysiskeVedleggMedTitler)
            .medLogiskeVedleggTitler(logiskeVedleggTitler).build();
        joarkFasade.oppdaterJournalpost(journalpostID, journalpostOppdatering, true);
        log.info("Oppdatert og ferdigstilt journalpost {}. ArkivsakID: {}", journalpostID, arkivSakID);
    }
}
