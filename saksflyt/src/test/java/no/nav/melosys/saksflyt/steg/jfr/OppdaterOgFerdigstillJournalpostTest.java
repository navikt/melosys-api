package no.nav.melosys.saksflyt.steg.jfr;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessType;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OppdaterOgFerdigstillJournalpostTest {
    @Mock
    private JoarkFasade joarkFasade;

    private OppdaterOgFerdigstillJournalpost oppdaterOgFerdigstillJournalpost;

    @Captor
    private ArgumentCaptor<JournalpostOppdatering> oppdateringArgumentCaptor;

    private final OppgaveFactory oppgaveFactory = new OppgaveFactory();

    @BeforeEach
    public void setUp() {
        oppdaterOgFerdigstillJournalpost = new OppdaterOgFerdigstillJournalpost(joarkFasade, oppgaveFactory);
    }

    @Test
    void utfør_avsenderNavnErNull_setterAvsenderNavnTilAvsenderId() {
        var prosessinstans = prosessinstans(false);
        oppdaterOgFerdigstillJournalpost.utfør(prosessinstans);

        verify(joarkFasade).oppdaterOgFerdigstillJournalpost(any(), oppdateringArgumentCaptor.capture());

        assertOppdatering(oppdateringArgumentCaptor.getValue(), false);
    }

    @Test
    void utfør_avsenderNavnErSatt_brukerAvsenderNavn() {
        var prosessinstans = prosessinstans(true);
        oppdaterOgFerdigstillJournalpost.utfør(prosessinstans);

        verify(joarkFasade).oppdaterOgFerdigstillJournalpost(any(), oppdateringArgumentCaptor.capture());

        assertOppdatering(oppdateringArgumentCaptor.getValue(), true);
    }

    @Test
    void utfør_mottakerKanalErEessi_setterIkkeAvsender() {
        var prosessinstans = new Prosessinstans();
        leggTilBehandling(prosessinstans);
        prosessinstans.setData(ProsessDataKey.AVSENDER_ID, null);
        prosessinstans.setData(ProsessDataKey.AVSENDER_ID, null);
        prosessinstans.setData(ProsessDataKey.AVSENDER_LAND, null);
        prosessinstans.setData(ProsessDataKey.AVSENDER_TYPE, null);
        prosessinstans.setData(ProsessDataKey.MOTTAKSKANAL_ER_EESSI, true);


        oppdaterOgFerdigstillJournalpost.utfør(prosessinstans);


        verify(joarkFasade).oppdaterOgFerdigstillJournalpost(any(), oppdateringArgumentCaptor.capture());
        var oppdatering = oppdateringArgumentCaptor.getValue();
        assertThat(oppdatering.getAvsenderID()).isNull();
        assertThat(oppdatering.getAvsenderNavn()).isNull();
        assertThat(oppdatering.getAvsenderLand()).isNull();
        assertThat(oppdatering.getAvsenderType()).isNull();
    }


    private Prosessinstans prosessinstans(boolean medAvsenderNavn) {
        var prosessinstans = new Prosessinstans();
        prosessinstans.setType(ProsessType.JFR_NY_SAK_BRUKER);
        leggTilBehandling(prosessinstans);

        if (medAvsenderNavn) {
            prosessinstans.setData(ProsessDataKey.AVSENDER_NAVN, avsenderNavn);

        }
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, brukerID);
        prosessinstans.setData(ProsessDataKey.AVSENDER_ID, avsenderID);
        prosessinstans.setData(ProsessDataKey.AVSENDER_LAND, avsenderLand);
        prosessinstans.setData(ProsessDataKey.AVSENDER_TYPE, avsendertype);
        prosessinstans.setData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, tittel);
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, hovedDokId);
        prosessinstans.setData(ProsessDataKey.MOTTATT_DATO, mottattDato);
        prosessinstans.setData(ProsessDataKey.LOGISKE_VEDLEGG_TITLER, logiskVedleggTitler);
        prosessinstans.setData(ProsessDataKey.FYSISKE_VEDLEGG, fysiskVedleggTitler);

        return prosessinstans;
    }

    private void assertOppdatering(JournalpostOppdatering oppdatering, boolean medAvsenderNavn) {
        if (medAvsenderNavn) {
            assertThat(oppdatering.getAvsenderNavn()).isEqualTo(avsenderNavn);
        } else {
            assertThat(oppdatering.getAvsenderNavn()).isEqualTo(avsenderID);
        }

        assertThat(oppdatering)
            .extracting(
                JournalpostOppdatering::getAvsenderLand,
                JournalpostOppdatering::getBrukerID,
                JournalpostOppdatering::getAvsenderID,
                JournalpostOppdatering::getAvsenderType,
                JournalpostOppdatering::getTittel,
                JournalpostOppdatering::getHovedDokumentID,
                JournalpostOppdatering::getMottattDato,
                JournalpostOppdatering::getLogiskeVedleggTitler,
                JournalpostOppdatering::getFysiskeVedlegg
            ).containsExactly(
                avsenderLand,
                brukerID,
                avsenderID,
                avsendertype,
                tittel,
                hovedDokId,
                mottattDato,
                logiskVedleggTitler,
                fysiskVedleggTitler
            );
    }

    private void leggTilBehandling(Prosessinstans prosessinstans) {
        prosessinstans.setBehandling(new Behandling());
        prosessinstans.getBehandling().setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        prosessinstans.getBehandling().setFagsak(new Fagsak());
        prosessinstans.getBehandling().getFagsak().setType(Sakstyper.EU_EOS);
        prosessinstans.getBehandling().getFagsak().setSaksnummer("MEL-123");
        prosessinstans.getBehandling().getFagsak().setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
    }

    private final String brukerID = "231";
    private final String avsenderNavn = "avsendernavn";
    private final String avsenderID = "avsenderID";
    private final Avsendertyper avsendertype = Avsendertyper.ORGANISASJON;
    private final String avsenderLand = "SE";
    private final String tittel = "Tittelei";
    private final String hovedDokId = "4424224";
    private final LocalDate mottattDato = LocalDate.now().minusYears(1);
    private final List<String> logiskVedleggTitler = List.of("tittelen", "tittelto");
    private final Map<String, String> fysiskVedleggTitler = Map.of("id", "doktittel");
}
