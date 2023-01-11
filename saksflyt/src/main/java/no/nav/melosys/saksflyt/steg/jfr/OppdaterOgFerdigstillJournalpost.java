package no.nav.melosys.saksflyt.steg.jfr;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPDATER_OG_FERDIGSTILL_JOURNALPOST;
import static no.nav.melosys.service.oppgave.OppgaveFactory.utledTema;

@Component
public class OppdaterOgFerdigstillJournalpost implements StegBehandler {

    private static final Logger log = LoggerFactory.getLogger(OppdaterOgFerdigstillJournalpost.class);

    private final JoarkFasade joarkFasade;

    public OppdaterOgFerdigstillJournalpost(JoarkFasade joarkFasade) {
        this.joarkFasade = joarkFasade;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPDATER_OG_FERDIGSTILL_JOURNALPOST;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void utfør(Prosessinstans prosessinstans) {
        String journalpostID = prosessinstans.getData(JOURNALPOST_ID);

        var behandling = prosessinstans.getBehandling();
        String brukerID = prosessinstans.getData(BRUKER_ID);
        String virksomhetOrgnr = prosessinstans.getData(VIRKSOMHET_ORGNR);
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

        JournalpostOppdatering journalpostOppdatering = new JournalpostOppdatering.Builder()
            .medSaksnummer(behandling.getFagsak().getSaksnummer())
            .medBrukerID(brukerID)
            .medVirksomhetOrgnr(virksomhetOrgnr)
            .medHovedDokumentID(hovedDokumentID)
            .medAvsenderID(avsenderID)
            .medAvsenderNavn(avsenderNavn)
            .medAvsenderType(avsenderType).medAvsenderLand(avsenderLand)
            .medTittel(tittel)
            .medMottattDato(mottattDato)
            .medFysiskeVedlegg(fysiskeVedleggMedTitler)
            .medLogiskeVedleggTitler(logiskeVedleggTitler)
            .medTema(utledTema(behandling.getFagsak().getTema()).getKode())
            .build();
        joarkFasade.oppdaterOgFerdigstillJournalpost(journalpostID, journalpostOppdatering);
        log.info("Oppdatert og ferdigstilt journalpost {} for fagsak: {}", journalpostID, behandling.getFagsak().getSaksnummer());
    }
}
