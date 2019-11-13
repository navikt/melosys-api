package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
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

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Before
    public void setUp() throws IkkeFunnetException {
        vedtakService = new VedtakService(behandlingService, behandlingsresultatService, oppgaveService, prosessinstansService, eessiService, landvelgerService);
        SpringSubjectHandler.set(new TestSubjectHandler());

        behandlingID = 1L;
        behandling.setId(behandlingID);
        behandlingsresultat.setId(behandlingID);

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
        when(eessiService.landErEessiReady(eq("LA_BUC_04"), eq("SE"))).thenReturn(true);
        when(eessiService.erGyldigInstitusjonForLand(eq("LA_BUC_04"), eq("SE"), eq(mottakerInstitusjon)))
            .thenReturn(Boolean.TRUE);

        vedtakService.fattVedtak(behandlingID, resultatType, mottakerInstitusjon);

        verify(behandlingService).hentBehandlingUtenSaksopplysninger(behandlingID);
        verify(behandlingService).lagre(eq(behandling));
        verify(prosessinstansService).opprettProsessinstansIverksettVedtak(any(Behandling.class), eq(resultatType), eq(mottakerInstitusjon));
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
        when(eessiService.landErEessiReady(eq("LA_BUC_04"), eq("SE"))).thenReturn(false);

        vedtakService.fattVedtak(behandlingID, resultatType, null);

        verify(behandlingService).hentBehandlingUtenSaksopplysninger(behandlingID);
        verify(behandlingService).lagre(eq(behandling));
        verify(prosessinstansService).opprettProsessinstansIverksettVedtak(any(Behandling.class), eq(resultatType), isNull());
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
        when(eessiService.landErEessiReady(eq("LA_BUC_04"), eq("SE"))).thenReturn(true);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Kan ikke fatte vedtak: SE er EESSI-ready, men mottaker er ikke satt");

        vedtakService.fattVedtak(behandlingID, resultatType, null);
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
        when(eessiService.landErEessiReady(eq("LA_BUC_04"), eq("SE"))).thenReturn(true);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage(String.format("MottakerID %s er ugyldig for land SE", mottakerInstitusjon));

        vedtakService.fattVedtak(behandlingID, resultatType, mottakerInstitusjon);
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

        vedtakService.fattVedtak(behandlingID, resultatType, null);
    }

    @Test
    public void endreVedtak_fungerer() throws FunksjonellException, TekniskException {
        vedtakService.endreVedtak(behandlingID, Endretperiode.ENDRINGER_ARBEIDSSITUASJON);

        verify(behandlingService).hentBehandlingUtenSaksopplysninger(behandlingID);
        verify(prosessinstansService).opprettProsessinstansForkortPeriode(any(Behandling.class), eq(Endretperiode.ENDRINGER_ARBEIDSSITUASJON));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(any());
    }
}