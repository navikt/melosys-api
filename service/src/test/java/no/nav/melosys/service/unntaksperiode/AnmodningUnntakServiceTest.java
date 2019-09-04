package no.nav.melosys.service.unntaksperiode;

import java.util.Collections;
import javax.ws.rs.BadRequestException;

import no.nav.melosys.domain.AnmodningsperiodeSvar;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.service.unntak.AnmodningUnntakService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnmodningUnntakServiceTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private AnmodningsperiodeService anmodningsperiodeService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;

    private AnmodningUnntakService anmodningUnntakService;

    @Before
    public void setUp() {
        anmodningUnntakService = new AnmodningUnntakService(behandlingService, oppgaveService, prosessinstansService, anmodningsperiodeService, lovvalgsperiodeService);
    }

    @Test
    public void anmodningOmUnntak_fungerer() throws FunksjonellException, TekniskException {
        long behandlingID = 1L;
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-111");
        behandling.setFagsak(fagsak);
        when(behandlingService.hentBehandling(behandlingID)).thenReturn(behandling);

        anmodningUnntakService.anmodningOmUnntak(behandlingID);

        verify(behandlingService).oppdaterStatus(eq(behandlingID), eq(Behandlingsstatus.ANMODNING_UNNTAK_SENDT));
        verify(prosessinstansService).opprettProsessinstansAnmodningOmUnntak(any(Behandling.class));
        verify(oppgaveService).leggTilbakeOppgaveMedSaksnummer(any());
    }

    @Test
    public void anmodningOmUnntakSvar_validert_forventMetodekall() throws FunksjonellException, TekniskException {
        Behandling behandling = lagBehandling();
        behandling.setType(Behandlingstyper.ANMODNING_OM_UNNTAK_HOVEDREGEL);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(anyLong())).thenReturn(Collections.singletonList(new AnmodningsperiodeSvar()));
        when(lovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(Collections.singletonList(new Lovvalgsperiode()));

        anmodningUnntakService.anmodningOmUnntakSvar(1L);

        verify(behandlingService).hentBehandling(eq(1L));
        verify(anmodningsperiodeService).hentAnmodningsperiodeSvarForBehandling(eq(1L));
        verify(lovvalgsperiodeService).hentLovvalgsperioder(eq(1L));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(eq("MEL-111"));
    }

    @Test(expected = BadRequestException.class)
    public void anmodningOmUnntakSvar_feilBehandlingstype_forventException() throws FunksjonellException, TekniskException {
        Behandling behandling = lagBehandling();
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        anmodningUnntakService.anmodningOmUnntakSvar(1L);
        verify(behandlingService).hentBehandling(eq(1L));
    }

    @Test(expected = BadRequestException.class)
    public void anmodningOmUnntakSvar_behandlingErAvsluttet_forventException() throws FunksjonellException, TekniskException {
        Behandling behandling = lagBehandling();
        behandling.setType(Behandlingstyper.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        anmodningUnntakService.anmodningOmUnntakSvar(1L);
        verify(behandlingService).hentBehandling(eq(1L));
    }

    @Test(expected = FunksjonellException.class)
    public void anmodningOmUnntakSvar_behandlingHarIngenAnmodningsperiodeSvar_forventException() throws FunksjonellException, TekniskException {
        Behandling behandling = lagBehandling();
        behandling.setType(Behandlingstyper.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        anmodningUnntakService.anmodningOmUnntakSvar(1L);
        verify(behandlingService).hentBehandling(eq(1L));
    }

    @Test(expected = FunksjonellException.class)
    public void anmodningOmUnntakSvar_behandlingHarIngenLovvalgsperiode_forventException() throws FunksjonellException, TekniskException {
        Behandling behandling = lagBehandling();
        behandling.setType(Behandlingstyper.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(anyLong())).thenReturn(Collections.singletonList(new AnmodningsperiodeSvar()));

        anmodningUnntakService.anmodningOmUnntakSvar(1L);
        verify(behandlingService).hentBehandling(eq(1L));
        verify(anmodningsperiodeService).hentAnmodningsperiodeSvarForBehandling(eq(1L));
    }

    private static Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-111");
        behandling.setFagsak(fagsak);
        behandling.setId(1L);

        return behandling;
    }
}