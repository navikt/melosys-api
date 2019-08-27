package no.nav.melosys.saksflyt.steg.jfr;

import java.util.Collections;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
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
public class SettVurderDokumentTest {

    @Mock
    private FagsakRepository fagsakRepository;

    @Mock
    private BehandlingRepository behandlingRepository;

    private SettVurderDokument agent;

    private final static String SAKSNUMMER_FINNES_IKKE = "MELTEST-0";
    private final static String SAKSNUMMER_UTEN_BEHANDLING = "MELTEST-1";
    private final static String SAKSNUMMER_MED_BEHANDLING = "MELTEST-2";

    @Captor
    private ArgumentCaptor<Behandling> behandlingArgumentCaptor;

    @Before
    public void setUp() {
        agent = new SettVurderDokument(fagsakRepository, behandlingRepository);

        Fagsak fagsak = new Fagsak();

        Fagsak fagsakMedBehandling = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        fagsakMedBehandling.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakRepository.findBySaksnummer(SAKSNUMMER_UTEN_BEHANDLING)).thenReturn(fagsak);
        when(fagsakRepository.findBySaksnummer(SAKSNUMMER_MED_BEHANDLING)).thenReturn(fagsakMedBehandling);
    }

    @Test
    public void utførSteg_sakMedBehandling_oppdatererStatus() {
        Prosessinstans p = new Prosessinstans();
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_MED_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD);
        p.setData(ProsessDataKey.JFR_INGEN_VURDERING, false);
        p.setData(ProsessDataKey.SKAL_TILORDNES, false);
        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.FERDIG);
        verify(behandlingRepository).save(behandlingArgumentCaptor.capture());
        assertThat(behandlingArgumentCaptor.getValue().getStatus()).isEqualTo(Behandlingsstatus.VURDER_DOKUMENT);
    }

    @Test
    public void utførSteg_sakUtenBehandling_ingenStatusEndring() {
        Prosessinstans p = new Prosessinstans();
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_UTEN_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD);
        p.setData(ProsessDataKey.JFR_INGEN_VURDERING, false);
        p.setData(ProsessDataKey.SKAL_TILORDNES, false);
        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.FERDIG);
        verify(behandlingRepository, never()).save(any(Behandling.class));
    }

    @Test
    public void utførSteg_ingenVurdering_ingenStatusEndring() {
        Prosessinstans p = new Prosessinstans();
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_MED_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD);
        p.setData(ProsessDataKey.JFR_INGEN_VURDERING, true);
        p.setData(ProsessDataKey.SKAL_TILORDNES, false);
        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.FERDIG);
        verify(behandlingRepository, never()).save(any(Behandling.class));
    }

    @Test
    public void utførSteg_sakMedBehandling_skalTildeleBehandlingsoppgave() {
        Prosessinstans p = new Prosessinstans();
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_MED_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD);
        p.setData(ProsessDataKey.JFR_INGEN_VURDERING, false);
        p.setData(ProsessDataKey.SKAL_TILORDNES, true);
        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_TILDEL_BEHANDLINGSOPPGAVE);
        verify(behandlingRepository).save(behandlingArgumentCaptor.capture());
        assertThat(behandlingArgumentCaptor.getValue().getStatus()).isEqualTo(Behandlingsstatus.VURDER_DOKUMENT);
    }

    @Test
    public void utførSteg_ukjentSak_feiler() {
        Prosessinstans p = new Prosessinstans();
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_FINNES_IKKE);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD);
        p.setData(ProsessDataKey.JFR_INGEN_VURDERING, false);
        p.setData(ProsessDataKey.SKAL_TILORDNES, false);
        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
        assertThat(p.getHendelser()).isNotEmpty();
        assertThat(p.getHendelser().get(0).getMelding()).isEqualTo("Det finnes ingen fagsak med saksnummer " + SAKSNUMMER_FINNES_IKKE);
    }
}