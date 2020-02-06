package no.nav.melosys.service.vedtak;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.kontroll.vedtak.VedtakKontrollService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
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
    @Mock
    private FagsakService fagsakService;
    @Mock
    private GsakFasade gsakFasade;
    @Mock
    private VedtakKontrollService vedtakKontrollService;
    @Mock
    private SaksopplysningerService saksopplysningerService;

    private VedtakService vedtakService;

    private long behandlingID;
    private Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
    private Behandling behandling = new Behandling();
    private Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
    private Behandling replikertBehandling = new Behandling();

    @Before
    public void setUp() throws Exception {
        vedtakService = new VedtakService(behandlingService, behandlingsresultatService, oppgaveService, prosessinstansService, eessiService, landvelgerService, fagsakService, gsakFasade, vedtakKontrollService, saksopplysningerService);
        SpringSubjectHandler.set(new TestSubjectHandler());

        behandlingID = 1L;
        behandling.setId(behandlingID);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandlingsresultat.setId(behandlingID);

        replikertBehandling.setId(2L);
        replikertBehandling.setStatus(Behandlingsstatus.OPPRETTET);
        replikertBehandling.setType(Behandlingstyper.NY_VURDERING);

        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        behandlingsresultat.getLovvalgsperioder().add(lovvalgsperiode);

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-111");
        Aktoer aktoer = new Aktoer();
        aktoer.setAktørId("1234567890123");
        aktoer.setRolle(Aktoersroller.BRUKER);
        fagsak.setAktører(Collections.singleton(aktoer));
        behandling.setFagsak(fagsak);

        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)).thenReturn(Collections.singletonList(Landkoder.SE));
        when(behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);
        when(behandlingService.replikerBehandlingOgBehandlingsresultat(any(Behandling.class), any(Behandlingsstatus.class), any(Behandlingstyper.class)))
            .thenReturn(replikertBehandling);
        when(behandlingService.hentBehandling(behandlingID)).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);
    }

    @Test
    public void fattVedtak_landErEessiReadyInstitusjonErSatt_fatterVedtak() throws MelosysException {
        var mottakerinstitusjoner = List.of("AB:CDEF123");
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId("1");
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        behandlingsresultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));
        behandlingsresultat.setType(resultatType);

        when(eessiService.validerOgAvklarMottakerInstitusjonerForBuc(anyList(), anyCollection(), any(BucType.class))).thenCallRealMethod();
        when(eessiService.hentEessiMottakerinstitusjoner(eq(BucType.LA_BUC_04.name()), eq(Landkoder.SE.getKode())))
            .thenReturn(List.of(new Institusjon("AB:CDEF123", "inst", Landkoder.SE.getKode())));

        Vedtakstyper vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK;
        vedtakService.fattVedtak(behandlingID, resultatType, "FRITEKST", mottakerinstitusjoner, vedtakstype, null);

        verify(behandlingService).hentBehandlingUtenSaksopplysninger(behandlingID);
        verify(behandlingService).lagre(eq(behandling));
        verify(prosessinstansService).opprettProsessinstansIverksettVedtak(any(Behandling.class), eq(resultatType), eq("FRITEKST"), eq(mottakerinstitusjoner), eq(vedtakstype), isNull());
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

        Vedtakstyper vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK;
        vedtakService.fattVedtak(behandlingID, resultatType, "FRITEKST", null, vedtakstype, null);

        verify(behandlingService).hentBehandlingUtenSaksopplysninger(behandlingID);
        verify(behandlingService).lagre(eq(behandling));
        verify(prosessinstansService).opprettProsessinstansIverksettVedtak(any(Behandling.class), eq(resultatType), eq("FRITEKST"), anyList(), eq(vedtakstype), isNull());
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(any());
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

        vedtakService.fattVedtak(behandlingID, resultatType, "FRITEKST",null, Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        verify(prosessinstansService).opprettProsessinstansIverksettVedtak(eq(behandling), eq(resultatType), eq("FRITEKST"), anyList(), eq(Vedtakstyper.FØRSTEGANGSVEDTAK), isNull());
    }

    @Test
    public void fattVedtak_erAvslagManglendeOpplysninger_fatterVedtak() throws MelosysException {
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId("1");
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL;
        behandlingsresultat.setType(resultatType);

        vedtakService.fattVedtak(behandlingID, resultatType, null, null, Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        verify(eessiService, never()).landErEessiReady(anyString(), anyString());
        verify(prosessinstansService)
            .opprettProsessinstansIverksettVedtak(eq(behandling), eq(resultatType), isNull(), anyList(), eq(Vedtakstyper.FØRSTEGANGSVEDTAK), isNull());
    }

    @Test
    public void fattVedtak_erAvslagLovvalgsperiodeIkkeInnvilget_fatterVedtak() throws MelosysException {
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId("1");
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.AVSLAATT);
        behandlingsresultat.setType(resultatType);

        vedtakService.fattVedtak(behandlingID, resultatType, null, null, Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        verify(eessiService, never()).landErEessiReady(anyString(), anyString());
        verify(prosessinstansService).opprettProsessinstansIverksettVedtak(eq(behandling), eq(resultatType), isNull(), anyList(), eq(Vedtakstyper.FØRSTEGANGSVEDTAK), isNull());
    }

    @Test
    public void fattVedtak_feilIValidering_kasterExceptionMedFeilkode() throws MelosysException {
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
        behandlingsresultat.setType(resultatType);
        when(vedtakKontrollService.utførKontroller(anyLong(), any(Vedtakstyper.class)))
            .thenReturn(Collections.singletonList(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER));

        ValideringException forventetException = null;
        try {
            vedtakService.fattVedtak(behandlingID, resultatType, null, null, Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        } catch (ValideringException ex) {
            forventetException = ex;
        }

        assertThat(forventetException).isNotNull();
        assertThat(forventetException.getFeilkoder()).containsExactly(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER.getKode());
    }

    @Test
    public void endreVedtak_fungerer() throws FunksjonellException, TekniskException {
        Aktoer myndighet = new Aktoer();
        myndighet.setRolle(Aktoersroller.MYNDIGHET);
        myndighet.setInstitusjonId("SE:SE001");
        behandling.getFagsak().setAktører(Set.of(myndighet));

        vedtakService.endreVedtak(behandlingID, Endretperiode.ENDRINGER_ARBEIDSSITUASJON, Behandlingstyper.ENDRET_PERIODE, "FRITEKST");

        verify(behandlingService).hentBehandlingUtenSaksopplysninger(behandlingID);
        verify(prosessinstansService).opprettProsessinstansForkortPeriode(any(Behandling.class), eq(Endretperiode.ENDRINGER_ARBEIDSSITUASJON), any());
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(any());
    }

    @Test
    public void revurderVedtak_fungerer() throws Exception {
        vedtakService.revurderVedtak(behandlingID);

        ArgumentCaptor<Oppgave> oppgaveArgumentCaptor = ArgumentCaptor.forClass(Oppgave.class);
        verify(behandlingService).hentBehandling(behandlingID);
        verify(behandlingService).replikerBehandlingOgBehandlingsresultat(behandling, Behandlingsstatus.OPPRETTET, Behandlingstyper.NY_VURDERING);
        verify(gsakFasade).opprettOppgave(oppgaveArgumentCaptor.capture());
        verifyNoMoreInteractions(gsakFasade, behandlingService);

        assertThat(oppgaveArgumentCaptor.getValue().getTilordnetRessurs()).isEqualTo("Z990007");
        assertThat(oppgaveArgumentCaptor.getValue().getAktørId()).isEqualTo("1234567890123");
        assertThat(oppgaveArgumentCaptor.getValue().getBehandlingstype()).isEqualTo(Behandlingstyper.NY_VURDERING);
    }

    @Test
    public void revurderVedtak_aktivBehandling_kasterException() throws Exception {
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);

        try {
            vedtakService.revurderVedtak(behandlingID);
            fail();
        } catch (FunksjonellException e) {
            assertThat(e.getMessage()).contains("aktiv");
        }
    }

    @Test
    public void revurderVedtak_forkortetPeriodeVedtak_kasterException() throws Exception {
        behandling.setType(Behandlingstyper.ENDRET_PERIODE);

        try {
            vedtakService.revurderVedtak(behandlingID);
            fail();
        } catch (FunksjonellException e) {
            assertThat(e.getMessage()).contains("forkortet");
        }
    }
}