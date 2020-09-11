package no.nav.melosys.service.unntaksperiode;

import java.util.Collections;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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
    private BehandlingsresultatService behandlingsresultatService;
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

    private AnmodningUnntakService anmodningUnntakService;

    @Before
    public void setUp() {
        anmodningUnntakService = new AnmodningUnntakService(behandlingService, behandlingsresultatService, oppgaveService, prosessinstansService, anmodningsperiodeService, lovvalgsperiodeService, landvelgerService, eessiService);
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
        behandling.getSaksopplysninger().add(lagPersonSaksopplysning(true));
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingService.hentBehandling(behandlingID)).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(eq(behandlingID))).thenReturn(behandlingsresultat);
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(eq(behandlingID))).thenReturn(Collections.singletonList(Landkoder.SE));

        anmodningUnntakService.anmodningOmUnntak(behandlingID, mottakerInstitusjon, fritekstSed);

        verify(anmodningsperiodeService).validerAnmodningsperiodeForBehandling(behandlingID);
        verify(prosessinstansService).opprettProsessinstansAnmodningOmUnntak(any(Behandling.class), anySet(), eq(fritekstSed));
        verify(oppgaveService).leggTilbakeOppgaveMedSaksnummer(any());

        assertThat(behandlingsresultat.getType()).isEqualTo(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
    }

    @Test
    public void anmodningOmUnntak_ikkeEessiReadyMottakerInstitusjonNull_prosessOpprettet() throws MelosysException {
        final long behandlingID = 1L;
        final String fritekstSed = "friteksssst";
        Behandling behandling = new Behandling();
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-111");
        behandling.setFagsak(fagsak);
        behandling.getSaksopplysninger().add(lagPersonSaksopplysning(true));
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(eq(behandlingID))).thenReturn(behandlingsresultat);
        when(behandlingService.hentBehandling(behandlingID)).thenReturn(behandling);
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(eq(behandlingID))).thenReturn(Collections.singletonList(Landkoder.SE));

        anmodningUnntakService.anmodningOmUnntak(behandlingID, null, fritekstSed);

        verify(anmodningsperiodeService).validerAnmodningsperiodeForBehandling(behandlingID);
        verify(prosessinstansService).opprettProsessinstansAnmodningOmUnntak(any(Behandling.class), anySet(), eq(fritekstSed));
        verify(oppgaveService).leggTilbakeOppgaveMedSaksnummer(any());

        assertThat(behandlingsresultat.getType()).isEqualTo(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
    }

    @Test
    public void anmodningOmUnntak_ikkeBostedsadresse_kasterException() throws MelosysException {
        final long behandlingID = 1L;
        final String fritekstSed = "friteksssst";
        Behandling behandling = new Behandling();
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-111");
        behandling.setFagsak(fagsak);
        behandling.getSaksopplysninger().add(lagPersonSaksopplysning(false));
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(new SoeknadDokument());
        when(behandlingService.hentBehandling(behandlingID)).thenReturn(behandling);
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(eq(behandlingID))).thenReturn(Collections.singletonList(Landkoder.SE));

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("bostedsadresse");

        anmodningUnntakService.anmodningOmUnntak(behandlingID, null, fritekstSed);
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

    private static Saksopplysning lagPersonSaksopplysning(boolean medAdresse) {
        PersonDokument personDokument = new PersonDokument();
        if (medAdresse) {
            personDokument.bostedsadresse.setPostnr("2123");
        }
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        saksopplysning.setDokument(personDokument);
        return saksopplysning;
    }
}