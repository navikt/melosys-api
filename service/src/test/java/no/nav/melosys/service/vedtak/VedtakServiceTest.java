package no.nav.melosys.service.vedtak;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.exception.validering.KontrollfeilDto;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.kontroll.vedtak.VedtakKontrollService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.service.validering.Kontrollfeil;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.event.ApplicationEventMulticaster;

import static no.nav.melosys.service.vedtak.VedtakService.FRIST_KLAGE_UKER;
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
    @Mock
    private AvklartefaktaService avklartefaktaService;
    @Mock
    private ApplicationEventMulticaster melosysEventMulticaster;

    private VedtakService vedtakService;

    private long behandlingID;
    private final Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
    private final Behandling behandling = new Behandling();
    private final Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        vedtakService = new VedtakService(behandlingService, behandlingsresultatService, oppgaveService, prosessinstansService,
            eessiService, landvelgerService, tpsFasade, registeropplysningerService, vedtakKontrollService, avklartefaktaService, melosysEventMulticaster);
        SpringSubjectHandler.set(new TestSubjectHandler());

        behandlingID = 1L;
        behandling.setId(behandlingID);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandlingsresultat.setId(behandlingID);
        behandlingsresultat.setBehandling(behandling);

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
        final Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
        final String behandlingsresultatFritekst = "FRITEKST HEIHEI";

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        behandlingsresultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));

        when(eessiService.validerOgAvklarMottakerInstitusjonerForBuc(anySet(), anyCollection(), any(BucType.class))).thenCallRealMethod();
        when(eessiService.hentEessiMottakerinstitusjoner(eq(BucType.LA_BUC_04.name()), eq(Set.of(Landkoder.SE.getKode()))))
            .thenReturn(List.of(new Institusjon("AB:CDEF123", "inst", Landkoder.SE.getKode())));

        Vedtakstyper vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK;
        vedtakService.fattVedtak(behandlingID, resultatType, behandlingsresultatFritekst, "FRITEKST_SED", mottakerinstitusjoner, vedtakstype, null);

        verify(behandlingService).hentBehandlingUtenSaksopplysninger(behandlingID);
        verify(behandlingService).lagre(eq(behandling));
        verify(prosessinstansService).opprettProsessinstansIverksettVedtak(any(Behandling.class), eq(resultatType), eq(behandlingsresultatFritekst), eq("FRITEKST_SED"), eq(mottakerinstitusjoner), isNull());
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(any());

        assertThat(behandlingsresultat)
            .extracting(Behandlingsresultat::getType, Behandlingsresultat::getBegrunnelseFritekst)
            .containsExactly(resultatType, behandlingsresultatFritekst);
        assertThat(behandlingsresultat.getVedtakMetadata()).isNotNull()
            .extracting(VedtakMetadata::getVedtakstype, VedtakMetadata::getRevurderBegrunnelse, VedtakMetadata::getVedtakKlagefrist)
            .containsExactly(vedtakstype, null, LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));
    }

    @Test
    public void fattVedtak_utenMottakerLandErIkkeEessiReady_fatterVedtak() throws MelosysException {
        final Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
        final String behandlingsresultatFritekst = "FRITEKST HEIHEI";

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        behandlingsresultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));

        Vedtakstyper vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK;
        vedtakService.fattVedtak(behandlingID, resultatType, behandlingsresultatFritekst, "FRITEKST_SED", null, vedtakstype, null);

        verify(behandlingService).hentBehandlingUtenSaksopplysninger(behandlingID);
        verify(behandlingService).lagre(eq(behandling));
        verify(prosessinstansService).opprettProsessinstansIverksettVedtak(any(Behandling.class), eq(resultatType), eq(behandlingsresultatFritekst), eq("FRITEKST_SED"), anySet(), isNull());
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(any());

        assertThat(behandlingsresultat)
            .extracting(Behandlingsresultat::getType, Behandlingsresultat::getBegrunnelseFritekst)
            .containsExactly(resultatType, behandlingsresultatFritekst);
        assertThat(behandlingsresultat.getVedtakMetadata()).isNotNull()
            .extracting(VedtakMetadata::getVedtakstype, VedtakMetadata::getRevurderBegrunnelse, VedtakMetadata::getVedtakKlagefrist)
            .containsExactly(vedtakstype, null, LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));
    }

    @Test
    public void fattVedtak_mottakerErNullOgErAnmodningOmUnntakSvarMottatt_fatterVedtak() throws MelosysException {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        behandlingsresultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));
        final Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
        final String behandlingsresultatFritekst = "FRITEKST HEIHEI";
        final Vedtakstyper vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK;

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setAnmodningsperiodeSvar(new AnmodningsperiodeSvar());
        anmodningsperiode.getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        behandlingsresultat.setAnmodningsperioder(Collections.singleton(anmodningsperiode));

        vedtakService.fattVedtak(behandlingID, resultatType, behandlingsresultatFritekst, "FRITEKST_SED", null, vedtakstype, null);
        verify(prosessinstansService).opprettProsessinstansIverksettVedtak(eq(behandling), eq(resultatType), eq(behandlingsresultatFritekst), eq("FRITEKST_SED"), anySet(), isNull());

        assertThat(behandlingsresultat)
            .extracting(Behandlingsresultat::getType, Behandlingsresultat::getBegrunnelseFritekst)
            .containsExactly(resultatType, behandlingsresultatFritekst);
        assertThat(behandlingsresultat.getVedtakMetadata()).isNotNull()
            .extracting(VedtakMetadata::getVedtakstype, VedtakMetadata::getRevurderBegrunnelse, VedtakMetadata::getVedtakKlagefrist)
            .containsExactly(vedtakstype, null, LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));
    }

    @Test
    public void fattVedtak_erAvslagManglendeOpplysninger_fatterVedtak() throws MelosysException {
        final Behandlingsresultattyper resultatType = Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL;
        final Vedtakstyper vedtakstype = Vedtakstyper.FØRSTEGANGSVEDTAK;

        vedtakService.fattVedtak(behandlingID, resultatType, null, null, null, vedtakstype, null);
        verify(prosessinstansService)
            .opprettProsessinstansIverksettVedtak(eq(behandling), eq(resultatType), isNull(), isNull(), anySet(), isNull());

        assertThat(behandlingsresultat)
            .extracting(Behandlingsresultat::getType, Behandlingsresultat::getBegrunnelseFritekst)
            .containsExactly(resultatType, null);
        assertThat(behandlingsresultat.getVedtakMetadata()).isNotNull()
            .extracting(VedtakMetadata::getVedtakstype, VedtakMetadata::getRevurderBegrunnelse, VedtakMetadata::getVedtakKlagefrist)
            .containsExactly(vedtakstype, null, LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));

    }

    @Test
    public void fattVedtak_erAvslagLovvalgsperiodeIkkeInnvilget_fatterVedtak() throws MelosysException {
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.AVSLAATT);

        vedtakService.fattVedtak(behandlingID, resultatType, null, null, null, Vedtakstyper.FØRSTEGANGSVEDTAK, null);
        verify(prosessinstansService).opprettProsessinstansIverksettVedtak(eq(behandling), eq(resultatType), isNull(), isNull(), anySet(), isNull());
    }

    @Test
    public void fattVedtak_prosessinstansFinnes_kasterException() {
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.AVSLAATT);
        when(prosessinstansService.harVedtakInstans(eq(behandlingID))).thenReturn(true);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> vedtakService.fattVedtak(behandlingID, resultatType, null, null, null, Vedtakstyper.FØRSTEGANGSVEDTAK, null))
            .withMessageContaining("vedtak-prosess");

        verify(prosessinstansService, never())
            .opprettProsessinstansIverksettVedtak(any(), any(), any(), any(), anySet(), any());
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
        when(vedtakKontrollService.utførKontroller(anyLong(), any(Vedtakstyper.class)))
            .thenReturn(Collections.singletonList(new Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER)));

        Throwable forventetException = catchThrowable(() ->
            vedtakService.fattVedtak(behandlingID, resultatType, null, null, null, Vedtakstyper.FØRSTEGANGSVEDTAK, null)
        );

        Consumer<ValideringException> medFeilkode = v -> assertThat(v.getFeilkoder()).extracting(KontrollfeilDto::getKode).containsExactly(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER.getKode());
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
        final Endretperiode endretperiodeBegrunnelse = Endretperiode.ENDRINGER_ARBEIDSSITUASJON;
        Aktoer myndighet = new Aktoer();
        myndighet.setRolle(Aktoersroller.MYNDIGHET);
        myndighet.setInstitusjonId("SE:SE001");
        behandling.getFagsak().setAktører(Set.of(myndighet));

        vedtakService.endreVedtak(behandlingID, endretperiodeBegrunnelse, "FRITEKST", "FRITEKST_SED");

        verify(avklartefaktaService).leggTilBegrunnelse(eq(behandlingID), eq(Avklartefaktatyper.AARSAK_ENDRING_PERIODE), eq(endretperiodeBegrunnelse.getKode()));
        verify(behandlingService).hentBehandlingUtenSaksopplysninger(behandlingID);
        verify(prosessinstansService).opprettProsessinstansForkortPeriode(
            any(Behandling.class),
            eq("FRITEKST"),
            eq("FRITEKST_SED")
        );
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(any());
    }
}