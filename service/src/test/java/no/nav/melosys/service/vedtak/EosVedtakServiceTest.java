package no.nav.melosys.service.vedtak;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.kontroll.ferdigbehandling.FerdigbehandlingKontrollService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.ApplicationEventMulticaster;

import static no.nav.melosys.domain.kodeverk.Vedtakstyper.FØRSTEGANGSVEDTAK;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.IVERKSETTER_VEDTAK;
import static no.nav.melosys.service.vedtak.VedtaksfattingFasade.FRIST_KLAGE_UKER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EosVedtakServiceTest {
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
    private AvklartefaktaService avklartefaktaService;
    @Mock
    private ApplicationEventMulticaster melosysEventMulticaster;
    @Mock
    private FerdigbehandlingKontrollService ferdigbehandlingKontrollService;

    private EosVedtakService vedtakService;

    private final long behandlingID = 1L;
    private final Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
    private final Behandling behandling = new Behandling();
    private final String behandlingsresultatFritekst = "FRITEKST HEIHEI";

    @BeforeEach
    void setUp() {
        vedtakService = new EosVedtakService(behandlingService, behandlingsresultatService, oppgaveService, prosessinstansService,
            eessiService, landvelgerService, avklartefaktaService, melosysEventMulticaster, ferdigbehandlingKontrollService);

        SpringSubjectHandler.set(new TestSubjectHandler());

        behandling.setId(behandlingID);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        behandlingsresultat.setId(behandlingID);
        behandlingsresultat.setBehandling(behandling);

        behandling.setFagsak(lagFagsak());
    }

    @Test
    void fattVedtak_erInnvilgelse_fatterVedtakOgKontrollererVedtak() throws Exception {
        var mottakerinstitusjoner = Set.of("AB:CDEF123");
        mockBehandlingsresultat();
        mockEesiReady();
        leggTilLovvalgsperiode(InnvilgelsesResultat.INNVILGET);

        vedtakService.fattVedtak(behandling, lagRequest(FASTSATT_LOVVALGSLAND, FØRSTEGANGSVEDTAK,
            behandlingsresultatFritekst, null, mottakerinstitusjoner));

        assertThat(behandlingsresultat)
            .extracting(Behandlingsresultat::getType, Behandlingsresultat::getBegrunnelseFritekst)
            .containsExactly(FASTSATT_LOVVALGSLAND, behandlingsresultatFritekst);

        assertThat(behandlingsresultat.getVedtakMetadata()).isNotNull()
            .extracting(VedtakMetadata::getVedtakstype, VedtakMetadata::getNyVurderingBakgrunn, VedtakMetadata::getVedtakKlagefrist)
            .containsExactly(FØRSTEGANGSVEDTAK, null, LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));

        verify(behandlingService).endreStatus(behandling, IVERKSETTER_VEDTAK);
        verify(prosessinstansService).opprettProsessinstansIverksettVedtakEos(
            any(),
            eq(FASTSATT_LOVVALGSLAND),
            eq(behandlingsresultatFritekst),
            isNull(),
            eq(mottakerinstitusjoner)
        );
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(any());
        verify(ferdigbehandlingKontrollService).kontrollerVedtakMedNyeRegisteropplysninger(any(Behandling.class), any(Behandlingsresultat.class), eq(Sakstyper.EU_EOS), any(Behandlingsresultattyper.class));
    }

    @Test
    void fattVedtak_landErEessiReadyInstitusjonErSatt_fatterVedtak() throws Exception {
        var mottakerinstitusjoner = Set.of("AB:CDEF123");
        mockBehandlingsresultat();
        mockEesiReady();
        leggTilLovvalgsperiode();

        vedtakService.fattVedtak(behandling, lagRequest(FASTSATT_LOVVALGSLAND, FØRSTEGANGSVEDTAK,
            behandlingsresultatFritekst, "FRITEKST_SED", mottakerinstitusjoner));

        assertThat(behandlingsresultat)
            .extracting(Behandlingsresultat::getType, Behandlingsresultat::getBegrunnelseFritekst)
            .containsExactly(FASTSATT_LOVVALGSLAND, behandlingsresultatFritekst);

        assertThat(behandlingsresultat.getVedtakMetadata()).isNotNull()
            .extracting(VedtakMetadata::getVedtakstype, VedtakMetadata::getNyVurderingBakgrunn, VedtakMetadata::getVedtakKlagefrist)
            .containsExactly(FØRSTEGANGSVEDTAK, null, LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));

        verify(behandlingService).endreStatus(behandling, IVERKSETTER_VEDTAK);
        verify(prosessinstansService).opprettProsessinstansIverksettVedtakEos(
            any(),
            eq(FASTSATT_LOVVALGSLAND),
            eq(behandlingsresultatFritekst),
            eq("FRITEKST_SED"),
            eq(mottakerinstitusjoner)
        );
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(any());
    }

    @Test
    void fattVedtak_utenMottakerLandErIkkeEessiReady_fatterVedtak() throws Exception {
        mockBehandlingsresultat();
        leggTilLovvalgsperiode();

        vedtakService.fattVedtak(behandling, lagRequest(FASTSATT_LOVVALGSLAND, FØRSTEGANGSVEDTAK,
            behandlingsresultatFritekst, "FRITEKST_SED", null));

        assertThat(behandlingsresultat)
            .extracting(Behandlingsresultat::getType, Behandlingsresultat::getBegrunnelseFritekst)
            .containsExactly(FASTSATT_LOVVALGSLAND, behandlingsresultatFritekst);

        assertThat(behandlingsresultat.getVedtakMetadata()).isNotNull()
            .extracting(VedtakMetadata::getVedtakstype, VedtakMetadata::getNyVurderingBakgrunn, VedtakMetadata::getVedtakKlagefrist)
            .containsExactly(FØRSTEGANGSVEDTAK, null, LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));

        verify(behandlingService).endreStatus(behandling, IVERKSETTER_VEDTAK);
        verify(prosessinstansService).opprettProsessinstansIverksettVedtakEos(
            any(Behandling.class),
            eq(FASTSATT_LOVVALGSLAND),
            eq(behandlingsresultatFritekst),
            eq("FRITEKST_SED"), anySet()
        );
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(any());
    }

    @Test
    void fattVedtak_mottakerErNullOgErAnmodningOmUnntakSvarMottatt_fatterVedtak() throws Exception {
        mockBehandlingsresultat();

        leggTilLovvalgsperiode();

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setAnmodningsperiodeSvar(new AnmodningsperiodeSvar());
        anmodningsperiode.getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        behandlingsresultat.setAnmodningsperioder(Collections.singleton(anmodningsperiode));

        vedtakService.fattVedtak(behandling, lagRequest(FASTSATT_LOVVALGSLAND, FØRSTEGANGSVEDTAK,
            behandlingsresultatFritekst, "FRITEKST_SED", null));

        assertThat(behandlingsresultat)
            .extracting(Behandlingsresultat::getType, Behandlingsresultat::getBegrunnelseFritekst)
            .containsExactly(FASTSATT_LOVVALGSLAND, behandlingsresultatFritekst);

        assertThat(behandlingsresultat.getVedtakMetadata()).isNotNull()
            .extracting(VedtakMetadata::getVedtakstype, VedtakMetadata::getNyVurderingBakgrunn, VedtakMetadata::getVedtakKlagefrist)
            .containsExactly(FØRSTEGANGSVEDTAK, null, LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));

        verify(prosessinstansService).opprettProsessinstansIverksettVedtakEos(
            eq(behandling),
            eq(FASTSATT_LOVVALGSLAND),
            eq(behandlingsresultatFritekst),
            eq("FRITEKST_SED"),
            anySet()
        );
    }

    @Test
    void fattVedtak_erAvslagManglendeOpplysninger_fatterVedtak() throws Exception {
        mockBehandlingsresultat();

        final Behandlingsresultattyper resultatType = Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL;
        final Vedtakstyper vedtakstype = FØRSTEGANGSVEDTAK;

        vedtakService.fattVedtak(behandling, lagRequest(resultatType, vedtakstype, null, null, null));

        assertThat(behandlingsresultat)
            .extracting(Behandlingsresultat::getType, Behandlingsresultat::getBegrunnelseFritekst)
            .containsExactly(resultatType, null);

        assertThat(behandlingsresultat.getVedtakMetadata()).isNotNull()
            .extracting(VedtakMetadata::getVedtakstype, VedtakMetadata::getNyVurderingBakgrunn, VedtakMetadata::getVedtakKlagefrist)
            .containsExactly(vedtakstype, null, LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));

        verify(prosessinstansService).opprettProsessinstansIverksettVedtakEos(
            eq(behandling),
            eq(resultatType),
            isNull(),
            isNull(),
            anySet()
        );
    }

    @Test
    void fattVedtak_erAvslagLovvalgsperiodeIkkeInnvilget_fatterVedtak() throws Exception {
        mockBehandlingsresultat();

        leggTilLovvalgsperiode(InnvilgelsesResultat.AVSLAATT);

        vedtakService.fattVedtak(behandling, lagRequest(FASTSATT_LOVVALGSLAND, FØRSTEGANGSVEDTAK, null, null, null));

        verify(prosessinstansService).opprettProsessinstansIverksettVedtakEos(
            eq(behandling),
            eq(FASTSATT_LOVVALGSLAND),
            isNull(),
            isNull(),
            anySet()
        );
    }

    @Test
    void fattVedtak_prosessinstansFinnes_kasterException() {
        mockBehandlingsresultat();
        when(prosessinstansService.harVedtakInstans(behandlingID)).thenReturn(true);

        leggTilLovvalgsperiode(InnvilgelsesResultat.AVSLAATT);

        final var FattVedtakRequest = lagRequest(FASTSATT_LOVVALGSLAND, FØRSTEGANGSVEDTAK, null, null, null);
        assertThatThrownBy(() -> vedtakService.fattVedtak(behandling, FattVedtakRequest))
            .isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("vedtak-prosess");

        verify(prosessinstansService).harVedtakInstans(behandlingID);
        verifyNoMoreInteractions(prosessinstansService);
    }

    @Test
    void endreVedtak_fungerer() {
        final Endretperiode endretperiodeBegrunnelse = Endretperiode.ENDRINGER_ARBEIDSSITUASJON;
        leggTilMyndighetAktoer();
        leggTilLovvalgsperiode();
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        vedtakService.endreVedtaksperiode(behandling, endretperiodeBegrunnelse, "FRITEKST", "FRITEKST_SED");

        assertThat(behandlingsresultat.getVedtakMetadata()).isNotNull();
        assertThat(behandlingsresultat.getBegrunnelseFritekst()).isEqualTo("FRITEKST");

        verify(avklartefaktaService).leggTilBegrunnelse(
            behandlingID,
            Avklartefaktatyper.AARSAK_ENDRING_PERIODE,
            endretperiodeBegrunnelse.getKode()
        );

        verify(prosessinstansService).opprettProsessinstansForkortPeriode(
            any(Behandling.class),
            eq("FRITEKST"),
            eq("FRITEKST_SED")
        );
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(any());
    }

    @Test
    void endreVedtak_harEksisterendeProsess_kasterException() {
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
        leggTilLovvalgsperiode();
        when(prosessinstansService.harAktivProsessinstans(behandlingID)).thenReturn(true);

        assertThatThrownBy(() -> vedtakService.endreVedtaksperiode(behandling, Endretperiode.ENDRINGER_ARBEIDSSITUASJON, "FRITEKST", "FRITEKST_SED"))
            .isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("Det finnes allerede en aktiv prosess for behandling");

        verify(prosessinstansService).harAktivProsessinstans(behandlingID);
        verifyNoMoreInteractions(avklartefaktaService);
        verifyNoMoreInteractions(prosessinstansService);
        verifyNoInteractions(oppgaveService);
    }

    private void mockBehandlingsresultat() {
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);
    }

    private void mockEesiReady() {
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)).thenReturn(Collections.singletonList(Landkoder.SE));
        when(eessiService.validerOgAvklarMottakerInstitusjonerForBuc(anySet(), anyCollection(), any(BucType.class))).thenCallRealMethod();
        when(eessiService.hentEessiMottakerinstitusjoner(BucType.LA_BUC_04.name(), Set.of(Landkoder.SE.getKode())))
            .thenReturn(List.of(new Institusjon("AB:CDEF123", "inst", Landkoder.SE.getKode())));
    }

    private void leggTilLovvalgsperiode() {
        leggTilLovvalgsperiode(null);
    }

    private void leggTilLovvalgsperiode(InnvilgelsesResultat innvilgelsesResultat) {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setInnvilgelsesresultat(innvilgelsesResultat);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setMedlPeriodeID(123L);
        lovvalgsperiode.setFom(LocalDate.now());
        behandlingsresultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-111");
        Aktoer aktoer = new Aktoer();
        aktoer.setAktørId("1234567890123");
        aktoer.setRolle(Aktoersroller.BRUKER);
        fagsak.setAktører(Collections.singleton(aktoer));
        return fagsak;
    }

    private void leggTilMyndighetAktoer() {
        Aktoer myndighet = new Aktoer();
        myndighet.setRolle(Aktoersroller.TRYGDEMYNDIGHET);
        myndighet.setInstitusjonId("SE:SE001");
        behandling.getFagsak().setAktører(Set.of(myndighet));
    }

    private FattVedtakRequest lagRequest(Behandlingsresultattyper behandlingsresultattype, Vedtakstyper vedtakstype,
                                            String behandlingsresultatFritekst, String fritekstSed, Set<String> mottakerinstitusjoner) {
        return new FattVedtakRequest.Builder()
            .medBehandlingsresultat(behandlingsresultattype)
            .medVedtakstype(vedtakstype)
            .medFritekst(behandlingsresultatFritekst)
            .medFritekstSed(fritekstSed)
            .medMottakerInstitusjoner(mottakerinstitusjoner)
            .build();
    }
}
