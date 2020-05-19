package no.nav.melosys.saksflyt.steg.jfr.sed.brev.aou;

import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessDataKey.*;

@Component("JournalførAouBrevFerdigstillJournalpost")
public class FerdigstillJournalpost extends AbstraktStegBehandler {
    private static final Logger log = LoggerFactory.getLogger(FerdigstillJournalpost.class);

    private final JoarkFasade joarkFasade;

    @Autowired
    public FerdigstillJournalpost(JoarkFasade joarkFasade) {
        this.joarkFasade = joarkFasade;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.JFR_AOU_BREV_FERDIGSTILL_JOURNALPOST;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        String journalpostId = prosessinstans.getData(ProsessDataKey.JOURNALPOST_ID);
        Long arkivSakID = prosessinstans.getData(GSAK_SAK_ID, Long.class);
        String hovedDokumentID = prosessinstans.getData(DOKUMENT_ID);
        String tittel = prosessinstans.getData(HOVEDDOKUMENT_TITTEL);
        String brukerID = prosessinstans.getData(BRUKER_ID);
        String avsenderID = prosessinstans.getData(AVSENDER_ID);
        String avsenderNavn = prosessinstans.getData(AVSENDER_NAVN);
        Avsendertyper avsenderType = prosessinstans.getData(AVSENDER_TYPE, Avsendertyper.class);
        List<String> logiskeVedleggTitler = prosessinstans.getData(LOGISKE_VEDLEGG_TITLER, List.class);
        Map<String, String> fysiskeVedleggMedTitler = prosessinstans.getData(FYSISKE_VEDLEGG, Map.class);

        JournalpostOppdatering journalpostOppdatering = new JournalpostOppdatering.Builder()
            .medArkivSakID(arkivSakID).medBrukerID(brukerID).medHovedDokumentID(hovedDokumentID)
            .medAvsenderID(avsenderID).medAvsenderNavn(avsenderNavn).medAvsenderType(avsenderType)
            .medTittel(tittel).medFysiskeVedlegg(fysiskeVedleggMedTitler)
            .medLogiskeVedleggTitler(logiskeVedleggTitler).build();
        joarkFasade.oppdaterJournalpost(journalpostId, journalpostOppdatering, true);

        log.info("Journalpost {} ferdigstilt for gsak-sak {}", journalpostId, prosessinstans.getData(ProsessDataKey.GSAK_SAK_ID));
        prosessinstans.setSteg(ProsessSteg.JFR_AOU_BREV_OPPRETT_SEDDOKUMENT);
    }
}
