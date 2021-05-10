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
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.exception.ValideringException;
import no.nav.melosys.exception.validering.KontrollfeilDto;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.kontroll.vedtak.VedtakKontrollService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.service.validering.Kontrollfeil;
import no.nav.melosys.service.vedtak.dto.FattEosVedtakRequest;
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
import static no.nav.melosys.service.vedtak.EosVedtakService.FRIST_KLAGE_UKER;
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
    private PersondataFasade persondataFasade;
    @Mock
    private RegisteropplysningerService registeropplysningerService;
    @Mock
    private VedtakKontrollService vedtakKontrollService;
    @Mock
    private AvklartefaktaService avklartefaktaService;
    @Mock
    private ApplicationEventMulticaster melosysEventMulticaster;

    private EosVedtakService vedtakService;

    private final long behandlingID = 1L;
    private final Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
    private final Behandling behandling = new Behandling();
    private final String behandlingsresultatFritekst = "FRITEKST HEIHEI";

    @BeforeEach
    void setUp() {
        vedtakService = new EosVedtakService(behandlingService, behandlingsresultatService, oppgaveService, prosessinstansService,
            eessiService, landvelgerService, persondataFasade, registeropplysningerService, vedtakKontrollService, avklartefaktaService, melosysEventMulticaster);

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
            .extracting(VedtakMetadata::getVedtakstype, VedtakMetadata::getRevurderBegrunnelse, VedtakMetadata::getVedtakKlagefrist)
            .containsExactly(FØRSTEGANGSVEDTAK, null, LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));

        verify(behandlingService).lagre(behandling);
        verify(prosessinstansService).opprettProsessinstansIverksettVedtakEos(
            any(),
            eq(FASTSATT_LOVVALGSLAND),
            eq(behandlingsresultatFritekst),
            eq("FRITEKST_SED"),
            eq(mottakerinstitusjoner),
            isNull()
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
            .extracting(VedtakMetadata::getVedtakstype, VedtakMetadata::getRevurderBegrunnelse, VedtakMetadata::getVedtakKlagefrist)
            .containsExactly(FØRSTEGANGSVEDTAK, null, LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));

        verify(behandlingService).lagre(behandling);
        verify(prosessinstansService).opprettProsessinstansIverksettVedtakEos(
            any(Behandling.class),
            eq(FASTSATT_LOVVALGSLAND),
            eq(behandlingsresultatFritekst),
            eq("FRITEKST_SED"), anySet(),
            isNull()
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
            .extracting(VedtakMetadata::getVedtakstype, VedtakMetadata::getRevurderBegrunnelse, VedtakMetadata::getVedtakKlagefrist)
            .containsExactly(FØRSTEGANGSVEDTAK, null, LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));

        verify(prosessinstansService).opprettProsessinstansIverksettVedtakEos(
            eq(behandling),
            eq(FASTSATT_LOVVALGSLAND),
            eq(behandlingsresultatFritekst),
            eq("FRITEKST_SED"),
            anySet(),
            isNull()
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
            .extracting(VedtakMetadata::getVedtakstype, VedtakMetadata::getRevurderBegrunnelse, VedtakMetadata::getVedtakKlagefrist)
            .containsExactly(vedtakstype, null, LocalDate.now().plusWeeks(FRIST_KLAGE_UKER));

        verify(prosessinstansService).opprettProsessinstansIverksettVedtakEos(
            eq(behandling),
            eq(resultatType),
            isNull(),
            isNull(),
            anySet(),
            isNull()
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
            anySet(),
            isNull()
        );
    }

    @Test
    void fattVedtak_prosessinstansFinnes_kasterException() throws Exception {
        mockBehandlingsresultat();
        when(prosessinstansService.harVedtakInstans(behandlingID)).thenReturn(true);

        leggTilLovvalgsperiode(InnvilgelsesResultat.AVSLAATT);

        assertThatThrownBy(() -> vedtakService.fattVedtak(behandling, lagRequest(FASTSATT_LOVVALGSLAND, FØRSTEGANGSVEDTAK, null, null,
            null)))
            .isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("vedtak-prosess");

        verify(prosessinstansService).harVedtakInstans(behandlingID);
        verifyNoMoreInteractions(prosessinstansService);
    }

    @Test
    void fattVedtak_feilFraKontroller_kasterExceptionMedFeilkode() throws Exception {
        mockBehandlingsresultat();
        mockFeilendeValidering();
        leggTilLovvalgsperiode(InnvilgelsesResultat.INNVILGET);

        Consumer<ValideringException> medFeilkode = v -> assertThat(v.getFeilkoder())
            .extracting(KontrollfeilDto::getKode).containsExactly(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER.getKode());

        assertThatThrownBy(() -> vedtakService.fattVedtak(behandling, lagRequest(FASTSATT_LOVVALGSLAND,
            FØRSTEGANGSVEDTAK, null, null, null)))
            .isInstanceOfSatisfying(ValideringException.class, medFeilkode)
            .hasMessage("Feil i validering. Kan ikke fatte vedtak.");
    }

    @Test
    void endreVedtak_fungerer() throws FunksjonellException, TekniskException {
        final Endretperiode endretperiodeBegrunnelse = Endretperiode.ENDRINGER_ARBEIDSSITUASJON;
        leggTilMyndighetAktoer();

        vedtakService.endreVedtak(behandling, endretperiodeBegrunnelse, "FRITEKST", "FRITEKST_SED");

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
    void endreVedtak_harEksisterendeProsess_kasterException() throws FunksjonellException {
        when(prosessinstansService.harAktivProsessinstans(behandlingID)).thenReturn(true);

        assertThatThrownBy(() -> vedtakService.endreVedtak(behandling, Endretperiode.ENDRINGER_ARBEIDSSITUASJON, "FRITEKST", "FRITEKST_SED"))
            .isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("Det finnes allerede en aktiv prosess for behandling");

        verify(avklartefaktaService).leggTilBegrunnelse(
            eq(behandlingID),
            eq(Avklartefaktatyper.AARSAK_ENDRING_PERIODE),
            any()
        );
        verify(prosessinstansService).harAktivProsessinstans(behandlingID);
        verifyNoMoreInteractions(prosessinstansService);
        verifyNoInteractions(oppgaveService);
    }

    private void mockBehandlingsresultat() throws IkkeFunnetException {
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);
    }

    private void mockEesiReady() throws Exception {
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)).thenReturn(Collections.singletonList(Landkoder.SE));
        when(eessiService.validerOgAvklarMottakerInstitusjonerForBuc(anySet(), anyCollection(), any(BucType.class))).thenCallRealMethod();
        when(eessiService.hentEessiMottakerinstitusjoner(BucType.LA_BUC_04.name(), Set.of(Landkoder.SE.getKode())))
            .thenReturn(List.of(new Institusjon("AB:CDEF123", "inst", Landkoder.SE.getKode())));
    }

    private void mockFeilendeValidering() throws FunksjonellException, TekniskException {
        when(persondataFasade.hentFolkeregisterIdent(anyString())).thenReturn("123");
        when(vedtakKontrollService.utførKontroller(anyLong(), any(Vedtakstyper.class)))
            .thenReturn(Collections.singletonList(new Kontrollfeil(Kontroll_begrunnelser.OVERLAPPENDE_MEDL_PERIODER)));
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
        myndighet.setRolle(Aktoersroller.MYNDIGHET);
        myndighet.setInstitusjonId("SE:SE001");
        behandling.getFagsak().setAktører(Set.of(myndighet));
    }

    private FattEosVedtakRequest lagRequest(Behandlingsresultattyper behandlingsresultattype, Vedtakstyper vedtakstype,
                                            String behandlingsresultatFritekst, String fritekstSed, Set<String> mottakerinstitusjoner) {
        return new FattEosVedtakRequest.Builder()
            .medBehandlingsresultat(behandlingsresultattype)
            .medVedtakstype(vedtakstype)
            .medFritekst(behandlingsresultatFritekst)
            .medFritekstSed(fritekstSed)
            .medMottakerInstitusjoner(mottakerinstitusjoner)
            .build();
    }
}
