package no.nav.melosys.service.vedtak;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
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
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.kontroll.vedtak.VedtakKontrollService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
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

import static org.assertj.core.api.Assertions.*;
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
    private TpsFasade tpsFasade;
    @Mock
    private RegisteropplysningerService registeropplysningerService;
    @Mock
    private VedtakKontrollService vedtakKontrollService;

    private VedtakService vedtakService;

    private long behandlingID;
    private final Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
    private final Behandling behandling = lagBehandlingMedBehandlingsgrunnlag();
    private final Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
    private final Behandling replikertBehandling = new Behandling();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        vedtakService = new VedtakService(behandlingService, behandlingsresultatService, oppgaveService, prosessinstansService,
            eessiService, landvelgerService, tpsFasade, registeropplysningerService, vedtakKontrollService);
        SpringSubjectHandler.set(new TestSubjectHandler());

        behandlingID = 1L;
        behandling.setId(behandlingID);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandlingsresultat.setId(behandlingID);

        replikertBehandling.setId(2L);
        replikertBehandling.setStatus(Behandlingsstatus.OPPRETTET);
        replikertBehandling.setType(Behandlingstyper.NY_VURDERING);
        replikertBehandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setMedlPeriodeID(123L);
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
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);

        when(tpsFasade.hentIdentForAktørId(anyString())).thenReturn("123");
    }

    @Test
    public void fattVedtak_landErEessiReadyInstitusjonErSatt_fatterVedtak() throws MelosysException {
        var mottakerinstitusjoner = Set.of("AB:CDEF123");
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId("1");
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        behandlingsresultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));
        behandlingsresultat.setType(resultatType);

        when(eessiService.validerOgAvklarMottakerInstitusjonerForBuc(anySet(), anyCollection(), any(BucType.class))).thenCallRealMethod();
        when(eessiService.hentEessiMottakerinstitusjoner(eq(BucType.LA_BUC_04.name()), eq(Landkoder.SE.getKode())))
            .thenReturn(List.of(new Institusjon("AB:CDEF123", "inst", Landkoder.SE.getKode())));

        Vedtakstyper vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK;
        vedtakService.fattVedtak(behandlingID, resultatType, "FRITEKST", "FRITEKST_SED", mottakerinstitusjoner, vedtakstype, null);

        verify(behandlingService).hentBehandlingUtenSaksopplysninger(behandlingID);
        verify(behandlingService).lagre(eq(behandling));
        verify(prosessinstansService).opprettProsessinstansIverksettVedtak(any(Behandling.class), eq(resultatType), eq("FRITEKST"), eq("FRITEKST_SED"), eq(mottakerinstitusjoner), eq(vedtakstype), isNull());
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
        vedtakService.fattVedtak(behandlingID, resultatType, "FRITEKST", "FRITEKST_SED", null, vedtakstype, null);

        verify(behandlingService).hentBehandlingUtenSaksopplysninger(behandlingID);
        verify(behandlingService).lagre(eq(behandling));
        verify(prosessinstansService).opprettProsessinstansIverksettVedtak(any(Behandling.class), eq(resultatType), eq("FRITEKST"), eq("FRITEKST_SED"), anySet(), eq(vedtakstype), isNull());
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(any());
    }

    @Test
    public void fattVedtak_mottakerErNullOgErAnmodningOmUnntakSvarMottatt_fatterVedtak() throws MelosysException {
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId("1");
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        behandlingsresultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setAnmodningsperiodeSvar(new AnmodningsperiodeSvar());
        anmodningsperiode.getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        behandlingsresultat.setAnmodningsperioder(Collections.singleton(anmodningsperiode));
        behandlingsresultat.setType(resultatType);

        vedtakService.fattVedtak(behandlingID, resultatType, "FRITEKST", "FRITEKST_SED", null, Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        verify(prosessinstansService).opprettProsessinstansIverksettVedtak(eq(behandling), eq(resultatType), eq("FRITEKST"), eq("FRITEKST_SED"), anySet(), eq(Vedtakstyper.FØRSTEGANGSVEDTAK), isNull());
    }

    @Test
    public void fattVedtak_erAvslagManglendeOpplysninger_fatterVedtak() throws MelosysException {
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId("1");
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL;
        behandlingsresultat.setType(resultatType);

        vedtakService.fattVedtak(behandlingID, resultatType, null, null, null, Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        verify(eessiService, never()).landErEessiReady(anyString(), anyString());
        verify(prosessinstansService)
            .opprettProsessinstansIverksettVedtak(eq(behandling), eq(resultatType), isNull(), isNull(), anySet(), eq(Vedtakstyper.FØRSTEGANGSVEDTAK), isNull());
    }

    @Test
    public void fattVedtak_erAvslagLovvalgsperiodeIkkeInnvilget_fatterVedtak() throws MelosysException {
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId("1");
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.AVSLAATT);
        behandlingsresultat.setType(resultatType);

        vedtakService.fattVedtak(behandlingID, resultatType, null, null, null, Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        verify(eessiService, never()).landErEessiReady(anyString(), anyString());
        verify(prosessinstansService).opprettProsessinstansIverksettVedtak(eq(behandling), eq(resultatType), isNull(), isNull(), anySet(), eq(Vedtakstyper.FØRSTEGANGSVEDTAK), isNull());
    }

    @Test
    public void fattVedtak_prosessinstansFinnes_kasterException() {
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.AVSLAATT);
        behandlingsresultat.setType(resultatType);
        when(prosessinstansService.harAktivVedtakInstans(eq(behandlingID))).thenReturn(true);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> vedtakService.fattVedtak(behandlingID, resultatType, null, null, null, Vedtakstyper.FØRSTEGANGSVEDTAK, null));

        verify(prosessinstansService, never())
            .opprettProsessinstansIverksettVedtak(any(), any(), any(), any(), anySet(), eq(Vedtakstyper.FØRSTEGANGSVEDTAK), any());
    }

    @Test
    public void fattVedtak_feilFraKontroller_kasterExceptionMedFeilkode() throws MelosysException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setFom(LocalDate.now());
        behandlingsresultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));

        Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
        behandlingsresultat.setType(resultatType);
        when(vedtakKontrollService.utførKontroller(anyLong(), any(Vedtakstyper.class)))
            .thenReturn(Collections.singletonList(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER));

        Throwable forventetException = catchThrowable(() ->
            vedtakService.fattVedtak(behandlingID, resultatType, null, null, null, Vedtakstyper.FØRSTEGANGSVEDTAK, null)
        );

        Consumer<ValideringException> medFeilkode = v -> assertThat(v.getFeilkoder()).containsExactly(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER.getKode());
        assertThat(forventetException).isInstanceOfSatisfying(ValideringException.class, medFeilkode);
    }

    @Test
    public void fattVedtak_feilBehandlingstype_kasterException() throws MelosysException {

        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Kan ikke fatte vedtak ved behandlingstema ");

        vedtakService.fattVedtak(behandlingID, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, null, null, null, Vedtakstyper.FØRSTEGANGSVEDTAK, null);
    }

    @Test
    public void endreVedtak_fungerer() throws FunksjonellException, TekniskException {
        Aktoer myndighet = new Aktoer();
        myndighet.setRolle(Aktoersroller.MYNDIGHET);
        myndighet.setInstitusjonId("SE:SE001");
        behandling.getFagsak().setAktører(Set.of(myndighet));

        vedtakService.endreVedtak(behandlingID, Endretperiode.ENDRINGER_ARBEIDSSITUASJON, "FRITEKST", "FRITEKST_SED");

        verify(behandlingService).hentBehandlingUtenSaksopplysninger(behandlingID);
        verify(prosessinstansService).opprettProsessinstansForkortPeriode(any(Behandling.class), eq(Endretperiode.ENDRINGER_ARBEIDSSITUASJON), eq("FRITEKST"), eq("FRITEKST_SED"));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(any());
    }

    @Test
    public void fattVedtak_harTomtForetakNavn_forventException() throws MelosysException {
        final long behandlingID = 2L;
        Behandling behandling = lagBehandlingMedBehandlingsgrunnlag();
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().arbeidUtland.add(new ArbeidUtland());
        when(behandlingService.hentBehandlingUtenSaksopplysninger(behandlingID)).thenReturn(behandling);

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Foretaksnavn kan ikke være tomt");

        vedtakService.fattVedtak(behandlingID, null, null, null, null, null, null);
    }

    private static Behandling lagBehandlingMedBehandlingsgrunnlag() {
        BehandlingsgrunnlagData behandlingsgrunnlagData = new BehandlingsgrunnlagData();
        behandlingsgrunnlagData.foretakUtland = new ArrayList<>();

        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(behandlingsgrunnlagData);

        Behandling behandling = new Behandling();
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);
        return behandling;
    }
}