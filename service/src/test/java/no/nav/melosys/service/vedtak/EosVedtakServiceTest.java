package no.nav.melosys.service.vedtak;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.eessi.BucType;
import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.kontroll.feature.ferdigbehandling.FerdigbehandlingKontrollFacade;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.ApplicationEventMulticaster;

import static no.nav.melosys.domain.kodeverk.Vedtakstyper.FØRSTEGANGSVEDTAK;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.AVSLAG_SØKNAD;
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
    private FerdigbehandlingKontrollFacade ferdigbehandlingKontrollFacade;
    @Mock
    private SaksbehandlingRegler saksbehandlingRegler;

    private EosVedtakService vedtakService;
    private final long behandlingID = 1L;
    private final Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
    private final Behandling behandling = BehandlingTestBuilder.builderWithDefaults().build();
    private final String behandlingsresultatFritekst = "FRITEKST HEIHEI";

    @BeforeEach
    void setUp() {
        vedtakService = new EosVedtakService(behandlingService, behandlingsresultatService, oppgaveService, prosessinstansService,
            eessiService, landvelgerService, avklartefaktaService, melosysEventMulticaster, ferdigbehandlingKontrollFacade, saksbehandlingRegler);

        SpringSubjectHandler.set(new TestSubjectHandler());

        behandling.setId(behandlingID);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);

        behandlingsresultat.setId(behandlingID);
        behandlingsresultat.setBehandling(behandling);

        behandling.setFagsak(lagFagsak());
    }

    @Test
    void fattVedtak_erInnvilgelse_fatterVedtakOgKontrollererVedtak() {
        var mottakerinstitusjoner = Set.of("AB:CDEF123");
        mockBehandlingsresultat();
        mockEesiReady();
        leggTilLovvalgsperiode(InnvilgelsesResultat.INNVILGET);

        vedtakService.fattVedtak(behandling, lagRequest(FASTSATT_LOVVALGSLAND, FØRSTEGANGSVEDTAK,
            behandlingsresultatFritekst, null, mottakerinstitusjoner));

        assertThat(behandlingsresultat)
            .extracting(Behandlingsresultat::getType, Behandlingsresultat::getNyVurderingBakgrunn, Behandlingsresultat::getBegrunnelseFritekst)
            .containsExactly(FASTSATT_LOVVALGSLAND, null, behandlingsresultatFritekst);

        assertThat(behandlingsresultat.getVedtakMetadata()).isNotNull()
            .extracting(VedtakMetadata::getVedtakstype, VedtakMetadata::getVedtakKlagefrist)
            .containsExactly(FØRSTEGANGSVEDTAK, LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));

        verify(behandlingService).endreStatus(behandling, IVERKSETTER_VEDTAK);
        verify(prosessinstansService).opprettProsessinstansIverksettVedtakEos(
            any(),
            eq(FASTSATT_LOVVALGSLAND),
            eq(behandlingsresultatFritekst),
            isNull(),
            eq(mottakerinstitusjoner),
            eq(true)
        );
        verify(oppgaveService).ferdigstillOppgaveMedBehandlingID(behandlingID);
        verify(ferdigbehandlingKontrollFacade).kontrollerVedtakMedRegisteropplysninger(any(Behandling.class), eq(Sakstyper.EU_EOS), any(Behandlingsresultattyper.class), eq(null));
    }

    @Test
    void fattVedtak_landErEessiReadyInstitusjonErSatt_fatterVedtak() {
        var mottakerinstitusjoner = Set.of("AB:CDEF123");
        mockBehandlingsresultat();
        mockEesiReady();
        leggTilLovvalgsperiode();

        vedtakService.fattVedtak(behandling, lagRequest(FASTSATT_LOVVALGSLAND, FØRSTEGANGSVEDTAK,
            behandlingsresultatFritekst, "FRITEKST_SED", mottakerinstitusjoner));

        assertThat(behandlingsresultat)
            .extracting(Behandlingsresultat::getType, Behandlingsresultat::getNyVurderingBakgrunn, Behandlingsresultat::getBegrunnelseFritekst)
            .containsExactly(FASTSATT_LOVVALGSLAND, null, behandlingsresultatFritekst);

        assertThat(behandlingsresultat.getVedtakMetadata()).isNotNull()
            .extracting(VedtakMetadata::getVedtakstype, VedtakMetadata::getVedtakKlagefrist)
            .containsExactly(FØRSTEGANGSVEDTAK, LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));

        verify(behandlingService).endreStatus(behandling, IVERKSETTER_VEDTAK);
        verify(prosessinstansService).opprettProsessinstansIverksettVedtakEos(
            any(),
            eq(FASTSATT_LOVVALGSLAND),
            eq(behandlingsresultatFritekst),
            eq("FRITEKST_SED"),
            eq(mottakerinstitusjoner),
            eq(true)
        );
        verify(oppgaveService).ferdigstillOppgaveMedBehandlingID(behandlingID);
    }

    @Test
    void fattVedtak_utenMottakerLandErIkkeEessiReady_fatterVedtak() {
        mockBehandlingsresultat();
        leggTilLovvalgsperiode();

        vedtakService.fattVedtak(behandling, lagRequest(FASTSATT_LOVVALGSLAND, FØRSTEGANGSVEDTAK,
            behandlingsresultatFritekst, "FRITEKST_SED", null));

        assertThat(behandlingsresultat)
            .extracting(Behandlingsresultat::getType, Behandlingsresultat::getNyVurderingBakgrunn, Behandlingsresultat::getBegrunnelseFritekst)
            .containsExactly(FASTSATT_LOVVALGSLAND, null, behandlingsresultatFritekst);

        assertThat(behandlingsresultat.getVedtakMetadata()).isNotNull()
            .extracting(VedtakMetadata::getVedtakstype, VedtakMetadata::getVedtakKlagefrist)
            .containsExactly(FØRSTEGANGSVEDTAK, LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));

        verify(behandlingService).endreStatus(behandling, IVERKSETTER_VEDTAK);
        verify(prosessinstansService).opprettProsessinstansIverksettVedtakEos(
            any(Behandling.class),
            eq(FASTSATT_LOVVALGSLAND),
            eq(behandlingsresultatFritekst),
            eq("FRITEKST_SED"),
            anySet(),
            eq(true)
        );
        verify(oppgaveService).ferdigstillOppgaveMedBehandlingID(behandlingID);
    }

    @Test
    void fattVedtak_mottakerErNullOgErAnmodningOmUnntakSvarMottatt_fatterVedtak() {
        mockBehandlingsresultat();

        leggTilLovvalgsperiode();

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setAnmodningsperiodeSvar(new AnmodningsperiodeSvar());
        anmodningsperiode.getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        behandlingsresultat.setAnmodningsperioder(Collections.singleton(anmodningsperiode));

        vedtakService.fattVedtak(behandling, lagRequest(FASTSATT_LOVVALGSLAND, FØRSTEGANGSVEDTAK,
            behandlingsresultatFritekst, "FRITEKST_SED", null));

        assertThat(behandlingsresultat)
            .extracting(Behandlingsresultat::getType, Behandlingsresultat::getNyVurderingBakgrunn, Behandlingsresultat::getBegrunnelseFritekst)
            .containsExactly(FASTSATT_LOVVALGSLAND, null, behandlingsresultatFritekst);

        assertThat(behandlingsresultat.getVedtakMetadata()).isNotNull()
            .extracting(VedtakMetadata::getVedtakstype, VedtakMetadata::getVedtakKlagefrist)
            .containsExactly(FØRSTEGANGSVEDTAK, LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));

        verify(prosessinstansService).opprettProsessinstansIverksettVedtakEos(
            eq(behandling),
            eq(FASTSATT_LOVVALGSLAND),
            eq(behandlingsresultatFritekst),
            eq("FRITEKST_SED"),
            anySet(),
            eq(true)
        );
    }

    @Test
    void fattVedtak_erAvslagManglendeOpplysninger_fatterVedtak() {
        mockBehandlingsresultat();

        final Behandlingsresultattyper resultatType = Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL;
        final Vedtakstyper vedtakstype = FØRSTEGANGSVEDTAK;

        vedtakService.fattVedtak(behandling, lagRequest(resultatType, vedtakstype, null, null, null));

        assertThat(behandlingsresultat)
            .extracting(Behandlingsresultat::getType, Behandlingsresultat::getNyVurderingBakgrunn, Behandlingsresultat::getBegrunnelseFritekst)
            .containsExactly(resultatType, null, null);

        assertThat(behandlingsresultat.getVedtakMetadata()).isNotNull()
            .extracting(VedtakMetadata::getVedtakstype, VedtakMetadata::getVedtakKlagefrist)
            .containsExactly(vedtakstype, LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));

        verify(prosessinstansService).opprettProsessinstansIverksettVedtakEos(
            eq(behandling),
            eq(resultatType),
            isNull(),
            isNull(),
            anySet(),
            eq(true)
        );
    }

    @Test
    void fattVedtak_erAvslag_fatterVedtakUtenKallTilEessi() {
        mockBehandlingsresultat();
        behandlingsresultat.setType(AVSLAG_SØKNAD);
        leggTilLovvalgsperiode(InnvilgelsesResultat.AVSLAATT);
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(behandling.getId())).thenReturn(Set.of(Land_iso2.SE));

        vedtakService.fattVedtak(behandling, lagRequest(AVSLAG_SØKNAD, FØRSTEGANGSVEDTAK, null, null, null));

        verify(prosessinstansService).opprettProsessinstansIverksettVedtakEos(
            eq(behandling),
            eq(AVSLAG_SØKNAD),
            isNull(),
            isNull(),
            anySet(),
            eq(true)
        );
        verifyNoInteractions(eessiService);
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

    private void mockBehandlingsresultat() {
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);
    }

    private void mockEesiReady() {
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)).thenReturn(Collections.singletonList(Land_iso2.SE));
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
        lovvalgsperiode.setLovvalgsland(Land_iso2.NO);
        lovvalgsperiode.setMedlPeriodeID(123L);
        lovvalgsperiode.setFom(LocalDate.now());
        behandlingsresultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));
    }

    private Fagsak lagFagsak() {
        return FagsakTestFactory.builder().medBruker().build();
    }

    private FattVedtakRequest lagRequest(Behandlingsresultattyper behandlingsresultattype, Vedtakstyper vedtakstype,
                                         String behandlingsresultatFritekst, String fritekstSed, Set<String> mottakerinstitusjoner) {
        return new FattVedtakRequest.Builder()
            .medBehandlingsresultatType(behandlingsresultattype)
            .medVedtakstype(vedtakstype)
            .medFritekst(behandlingsresultatFritekst)
            .medFritekstSed(fritekstSed)
            .medMottakerInstitusjoner(mottakerinstitusjoner)
            .build();
    }
}
