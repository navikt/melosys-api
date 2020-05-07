package no.nav.melosys.saksflyt.steg.jfr;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.JournalfoeringMangel;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
    public class OppdaterJournalpostTest {
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private FagsakService fagsakService;

    private OppdaterJournalpost agent;

    @Captor
    private ArgumentCaptor<JournalpostOppdatering> oppdateringArgumentCaptor;

    @Before
    public void setUp() {
        agent = new OppdaterJournalpost(joarkFasade, fagsakService);
    }

    @Test
    public void utfoerSteg() throws MelosysException {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_NY_SAK);
        p.setData(ProsessDataKey.GSAK_SAK_ID, 123L);
        p.setData(ProsessDataKey.AVSENDER_NAVN, "navn");
        agent.utførSteg(p);

        verify(joarkFasade).utledJournalfoeringsbehov(any());
        verify(joarkFasade).oppdaterJournalpost(any(), oppdateringArgumentCaptor.capture(), eq(false));
        assertThat(oppdateringArgumentCaptor.getValue().isMedDokumentkategori()).isFalse();
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_FERDIGSTILL_JOURNALPOST);
    }

    @Test
    public void utfoerSteg_oppdaterDokumentKategori() throws MelosysException {
        List<JournalfoeringMangel> mangler = new ArrayList<>();
        mangler.add(JournalfoeringMangel.HOVEDDOKUMENT_KATEGORI);
        when(joarkFasade.utledJournalfoeringsbehov(any())).thenReturn(mangler);
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_NY_SAK);
        p.setData(ProsessDataKey.GSAK_SAK_ID, 456L);
        p.setData(ProsessDataKey.AVSENDER_NAVN, "navn");

        agent.utførSteg(p);

        verify(joarkFasade).utledJournalfoeringsbehov(any());
        verify(joarkFasade).oppdaterJournalpost(any(), oppdateringArgumentCaptor.capture(), eq(false));
        assertThat(oppdateringArgumentCaptor.getValue().isMedDokumentkategori()).isTrue();
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_FERDIGSTILL_JOURNALPOST);
    }

    @Test
    public void utfoerSteg_hentGsakFraEksisterendeSakHvisEndretPeriode() throws MelosysException {
        Fagsak fagsak = new Fagsak();
        long gsakSaksnummer = 10L;
        String saksnummer = "saksnummer";
        fagsak.setGsakSaksnummer(gsakSaksnummer);
        when(fagsakService.hentFagsak(eq(saksnummer))).thenReturn(fagsak);
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_NY_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.ENDRET_PERIODE);
        p.setData(ProsessDataKey.SAKSNUMMER, saksnummer);
        p.setData(ProsessDataKey.AVSENDER_NAVN, "navn");
        agent.utførSteg(p);

        verify(joarkFasade).utledJournalfoeringsbehov(any());
        verify(joarkFasade).oppdaterJournalpost(any(), oppdateringArgumentCaptor.capture(), eq(false));
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_FERDIGSTILL_JOURNALPOST);
        assertThat(oppdateringArgumentCaptor.getValue().getArkivSakID()).isEqualTo(gsakSaksnummer);
    }
}