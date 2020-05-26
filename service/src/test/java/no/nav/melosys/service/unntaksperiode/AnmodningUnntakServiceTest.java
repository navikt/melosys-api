package no.nav.melosys.service.unntaksperiode;

import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.service.unntak.AnmodningUnntakService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.apache.commons.lang3.RandomStringUtils;
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
    @Mock
    private LandvelgerService landvelgerService;
    @Mock
    private EessiService eessiService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private AnmodningUnntakService anmodningUnntakService;

    @Before
    public void setUp() throws IkkeFunnetException {
        anmodningUnntakService = new AnmodningUnntakService(
            behandlingService, oppgaveService, prosessinstansService, anmodningsperiodeService,
            lovvalgsperiodeService, landvelgerService, eessiService, behandlingsresultatService);
    }

    @Test
    public void anmodningOmUnntak_erEessiKlarMedMottakerInstitusjon_prosessOpprettet() throws MelosysException {
        final long behandlingID = 1L;
        final String mottakerInstitusjon = "SE:432";
        final String fritekstSed = "friteksssst";
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-111");
        behandling.setFagsak(fagsak);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(eq(behandlingID))).thenReturn(Collections.singletonList(Landkoder.SE));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(lagBehandlingsresultat());

        anmodningUnntakService.anmodningOmUnntak(behandlingID, mottakerInstitusjon, fritekstSed);

        verify(behandlingsresultatService).hentBehandlingsresultat(behandlingID);
        verify(prosessinstansService).opprettProsessinstansAnmodningOmUnntak(any(Behandling.class), anySet(), eq(fritekstSed));
        verify(oppgaveService).leggTilbakeOppgaveMedSaksnummer(any());
    }

    @Test
    public void anmodningOmUnntakSvar_validert_forventMetodekall() throws FunksjonellException, TekniskException {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);

        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);
        when(anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(anyLong())).thenReturn(Collections.singletonList(new AnmodningsperiodeSvar()));
        when(lovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(Collections.singletonList(new Lovvalgsperiode()));

        anmodningUnntakService.anmodningOmUnntakSvar(1L);

        verify(behandlingService).hentBehandlingUtenSaksopplysninger(eq(1L));
        verify(anmodningsperiodeService).hentAnmodningsperiodeSvarForBehandling(eq(1L));
        verify(lovvalgsperiodeService).hentLovvalgsperioder(eq(1L));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(eq("MEL-111"));
    }

    @Test
    public void anmodningOmUnntakSvar_feilBehandlingstype_forventException() throws FunksjonellException, TekniskException {
        Behandling behandling = lagBehandling();
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Behandling er ikke av tema ANMODNING_OM_UNNTAK_HOVEDREGEL");
        anmodningUnntakService.anmodningOmUnntakSvar(1L);
    }

    @Test
    public void anmodningOmUnntakSvar_behandlingErAvsluttet_forventException() throws FunksjonellException, TekniskException {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Behandlingen er avsluttet");

        anmodningUnntakService.anmodningOmUnntakSvar(1L);
    }

    @Test
    public void anmodningOmUnntakSvar_behandlingHarIngenAnmodningsperiodeSvar_forventException() throws FunksjonellException, TekniskException {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Finner ingen AnmodningsperiodeSvar for behandling 1");
        anmodningUnntakService.anmodningOmUnntakSvar(1L);
    }

    @Test
    public void anmodningOmUnntakSvar_behandlingHarIngenLovvalgsperiode_forventException() throws FunksjonellException, TekniskException {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);

        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);
        when(anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(anyLong())).thenReturn(Collections.singletonList(new AnmodningsperiodeSvar()));

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Finner ingen Lovvalgsperioder for behandling 1");

        anmodningUnntakService.anmodningOmUnntakSvar(1L);
    }

    @Test
    public void anmodningOmUnntakSvar_avslagForLangFritekst_forventException() throws FunksjonellException, TekniskException {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);

        AnmodningsperiodeSvar anmodningsperiodeSvar = new AnmodningsperiodeSvar();
        anmodningsperiodeSvar.setBegrunnelseFritekst(RandomStringUtils.random(256));
        anmodningsperiodeSvar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.AVSLAG);

        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);
        when(anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(anyLong())).thenReturn(Collections.singletonList(anmodningsperiodeSvar));
        when(lovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(Collections.singletonList(new Lovvalgsperiode()));

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Kan ikke ha fritekst lengre enn 255 for avslag på anmodning om unntak");

        anmodningUnntakService.anmodningOmUnntakSvar(1L);
    }

    private static Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-111");
        behandling.setFagsak(fagsak);
        behandling.setId(1L);

        return behandling;
    }

    private static Behandlingsresultat lagBehandlingsresultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setAnmodningsperioder(Set.of(new Anmodningsperiode()));
        return behandlingsresultat;
    }
}