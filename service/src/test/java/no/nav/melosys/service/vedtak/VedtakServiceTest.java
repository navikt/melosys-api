package no.nav.melosys.service.vedtak;

import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VedtakServiceTest {
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private EessiService eessiService;
    @Mock
    private LandvelgerService landvelgerService;
    private VedtakService vedtakService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private long behandlingID;
    private Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
    private Behandling behandling = new Behandling();
    private Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();

    @Before
    public void setUp() throws IkkeFunnetException {
        vedtakService = new VedtakService(behandlingService, behandlingsresultatService, oppgaveService, prosessinstansService, eessiService, landvelgerService);
        SpringSubjectHandler.set(new TestSubjectHandler());

        behandlingID = 1L;
        behandling.setId(behandlingID);
        behandlingsresultat.setId(behandlingID);

        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-111");
        behandling.setFagsak(fagsak);

        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)).thenReturn(Collections.singletonList(Landkoder.SE));
        when(behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);
    }

    @Test
    public void fattVedtak_landErEessiReadyInstitusjonErSatt_fatterVedtak() throws MelosysException {
        String mottakerInstitusjon = "AB:CDEF123";
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId("1");
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        behandlingsresultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));
        behandlingsresultat.setType(resultatType);
        when(eessiService.landErEessiReady(eq("LA_BUC_04"), eq("SE"))).thenReturn(true);
        when(eessiService.erGyldigInstitusjonForLand(eq("LA_BUC_04"), eq("SE"), eq(mottakerInstitusjon)))
            .thenReturn(Boolean.TRUE);

        vedtakService.fattVedtak(behandlingID, resultatType, "FRITEKST", mottakerInstitusjon);

        verify(behandlingService).hentBehandlingUtenSaksopplysninger(behandlingID);
        verify(behandlingService).lagre(eq(behandling));
        verify(prosessinstansService).opprettProsessinstansIverksettVedtak(any(Behandling.class), eq(resultatType), eq("FRITEKST"), eq(mottakerInstitusjon));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(any());
    }

    @Test
    public void fattVedtak_utenMottakerLandErIkkeEessiReady_fatterVedtak() throws MelosysException {
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId("1");
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        behandlingsresultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));
        behandlingsresultat.setType(resultatType);
        when(eessiService.landErEessiReady(eq("LA_BUC_04"), eq("SE"))).thenReturn(false);

        vedtakService.fattVedtak(behandlingID, resultatType, "FRITEKST", null);

        verify(behandlingService).hentBehandlingUtenSaksopplysninger(behandlingID);
        verify(behandlingService).lagre(eq(behandling));
        verify(prosessinstansService).opprettProsessinstansIverksettVedtak(any(Behandling.class), eq(resultatType), eq("FRITEKST"), isNull());
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(any());
    }

    @Test
    public void fattVedtak_utenMottakerLandErEessiReady_kasterException() throws MelosysException {
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId("1");
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        behandlingsresultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));
        behandlingsresultat.setType(resultatType);
        when(eessiService.landErEessiReady(eq("LA_BUC_04"), eq("SE"))).thenReturn(true);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Kan ikke fatte vedtak: SE er EESSI-ready, men mottaker er ikke satt");

        vedtakService.fattVedtak(behandlingID, resultatType, null, null);
    }

    @Test
    public void fattVedtak_medMottakerLandErEessiReadyFeilMottaker_kasterException() throws MelosysException {
        String mottakerInstitusjon = "SE:123";
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId("1");
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        behandlingsresultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));
        behandlingsresultat.setType(resultatType);
        when(eessiService.landErEessiReady(eq("LA_BUC_04"), eq("SE"))).thenReturn(true);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage(String.format("MottakerID %s er ugyldig for land SE", mottakerInstitusjon));

        vedtakService.fattVedtak(behandlingID, resultatType, "FRITEKST", mottakerInstitusjon);
    }

    @Test
    public void fattVedtak_mottakerErNullOgErAnmodningOmUnntakSvarMottatt_fatterVedtak() throws MelosysException {
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId("1");
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setAnmodningsperiodeSvar(new AnmodningsperiodeSvar());
        anmodningsperiode.getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        behandlingsresultat.setAnmodningsperioder(Collections.singleton(anmodningsperiode));
        behandlingsresultat.setType(resultatType);

        vedtakService.fattVedtak(behandlingID, resultatType, "FRITEKST",null);
        verify(prosessinstansService).opprettProsessinstansIverksettVedtak(eq(behandling), eq(resultatType), eq("FRITEKST"), isNull());
    }

    @Test
    public void fattVedtak_erAvslagManglendeOpplysninger_fatterVedtak() throws MelosysException {
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId("1");
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL;
        behandlingsresultat.setType(resultatType);

        vedtakService.fattVedtak(behandlingID, resultatType, null, null);
        verify(eessiService, never()).landErEessiReady(anyString(), anyString());
        verify(prosessinstansService).opprettProsessinstansIverksettVedtak(eq(behandling), eq(resultatType), isNull(), isNull());
    }

    @Test
    public void fattVedtak_erAvslagLovvalgsperiodeIkkeInnvilget_fatterVedtak() throws MelosysException {
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId("1");
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.AVSLAATT);
        behandlingsresultat.setType(resultatType);

        vedtakService.fattVedtak(behandlingID, resultatType, null, null);
        verify(eessiService, never()).landErEessiReady(anyString(), anyString());
        verify(prosessinstansService).opprettProsessinstansIverksettVedtak(eq(behandling), eq(resultatType), isNull(), isNull());
    }

    @Test
    public void endreVedtak_fungerer() throws FunksjonellException, TekniskException {
        Aktoer myndighet = new Aktoer();
        myndighet.setRolle(Aktoersroller.MYNDIGHET);
        myndighet.setInstitusjonId("SE:SE001");
        behandling.getFagsak().setAktører(Set.of(myndighet));

        vedtakService.endreVedtak(behandlingID, Endretperiode.ENDRINGER_ARBEIDSSITUASJON, Behandlingstyper.ENDRET_PERIODE, "FRITEKST");

        verify(behandlingService).hentBehandlingUtenSaksopplysninger(behandlingID);
        verify(prosessinstansService).opprettProsessinstansForkortPeriode(any(Behandling.class), eq(Endretperiode.ENDRINGER_ARBEIDSSITUASJON), any(), eq("SE:SE001"));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(any());
    }
}