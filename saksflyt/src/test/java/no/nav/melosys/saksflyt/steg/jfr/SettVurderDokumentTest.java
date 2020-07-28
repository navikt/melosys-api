package no.nav.melosys.saksflyt.steg.jfr;

import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
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
public class SettVurderDokumentTest {

    @Mock
    private FagsakService fagsakService;

    @Mock
    private BehandlingRepository behandlingRepository;

    private SettVurderDokument agent;

    private final static String SAKSNUMMER_UTEN_BEHANDLING = "MELTEST-1";
    private final static String SAKSNUMMER_MED_BEHANDLING = "MELTEST-2";

    @Captor
    private ArgumentCaptor<Behandling> behandlingArgumentCaptor;

    @Before
    public void setUp() throws IkkeFunnetException {
        agent = new SettVurderDokument(fagsakService, behandlingRepository);

        Fagsak fagsak = new Fagsak();

        Fagsak fagsakMedBehandling = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        fagsakMedBehandling.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakService.hentFagsak(SAKSNUMMER_UTEN_BEHANDLING)).thenReturn(fagsak);
        when(fagsakService.hentFagsak(SAKSNUMMER_MED_BEHANDLING)).thenReturn(fagsakMedBehandling);
    }

    @Test
    public void utfør_sakMedBehandling_oppdatererStatus() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_MED_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD);
        p.setData(ProsessDataKey.JFR_INGEN_VURDERING, false);
        p.setData(ProsessDataKey.SKAL_TILORDNES, false);
        agent.utfør(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.FERDIG);
        verify(behandlingRepository).save(behandlingArgumentCaptor.capture());
        assertThat(behandlingArgumentCaptor.getValue().getStatus()).isEqualTo(Behandlingsstatus.VURDER_DOKUMENT);
    }

    @Test
    public void utfør_sakUtenBehandling_ingenStatusEndring() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_UTEN_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD);
        p.setData(ProsessDataKey.JFR_INGEN_VURDERING, false);
        p.setData(ProsessDataKey.SKAL_TILORDNES, false);
        agent.utfør(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.FERDIG);
        verify(behandlingRepository, never()).save(any(Behandling.class));
    }

    @Test
    public void utfør_ingenVurdering_ingenStatusEndring() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_MED_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD);
        p.setData(ProsessDataKey.JFR_INGEN_VURDERING, true);
        p.setData(ProsessDataKey.SKAL_TILORDNES, false);
        agent.utfør(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.FERDIG);
        verify(behandlingRepository, never()).save(any(Behandling.class));
    }

    @Test
    public void utfør_sakMedBehandling_skalTildeleBehandlingsoppgave() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_MED_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstyper.SOEKNAD);
        p.setData(ProsessDataKey.JFR_INGEN_VURDERING, false);
        p.setData(ProsessDataKey.SKAL_TILORDNES, true);
        agent.utfør(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_TILDEL_BEHANDLINGSOPPGAVE);
        verify(behandlingRepository).save(behandlingArgumentCaptor.capture());
        assertThat(behandlingArgumentCaptor.getValue().getStatus()).isEqualTo(Behandlingsstatus.VURDER_DOKUMENT);
    }
}