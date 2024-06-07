package no.nav.melosys.service.unntak;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.eessi.SedType;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.kontroll.feature.anmodningomunntak.AnmodningUnntakKontrollService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.SaksopplysningType.SEDOPPL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnmodningUnntakServiceTest {
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
    @Mock
    private AnmodningUnntakKontrollService anmodningUnntakKontrollService;
    @Mock
    private JoarkFasade joarkFasade;
    private AnmodningUnntakService anmodningUnntakService;

    private static final long BEHANDLING_ID = 1L;
    private static final String FRITEKST_SED = "Ytterligere info som fritekst";
    private static final String MOTTAKER_INSTITUSJON = "SE:432";

    @Captor
    private ArgumentCaptor<Collection<Lovvalgsperiode>> lovvalgsperioder;

    @BeforeEach
    public void setUp() {
        anmodningUnntakService = new AnmodningUnntakService(
            behandlingService, behandlingsresultatService, oppgaveService, prosessinstansService, anmodningsperiodeService,
            lovvalgsperiodeService, landvelgerService, eessiService, anmodningUnntakKontrollService, joarkFasade);

        TestSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    void anmodningOmUnntak_erEessiKlarMedMottakerInstitusjon_prosessOpprettet() throws Exception {
        final DokumentReferanse dokumentReferanse = new DokumentReferanse("jpID", "dokID");
        Behandling behandling = new Behandling();
        Fagsak fagsak = FagsakTestFactory.lagFagsak();
        behandling.setFagsak(fagsak);
        behandling.getSaksopplysninger().add(lagPersonSaksopplysning());
        behandling.setMottatteOpplysninger(new MottatteOpplysninger());
        when(behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID)).thenReturn(behandling);
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID)).thenReturn(Collections.singletonList(Land_iso2.SE));

        anmodningUnntakService.anmodningOmUnntak(BEHANDLING_ID, MOTTAKER_INSTITUSJON, Set.of(dokumentReferanse), FRITEKST_SED);

        verify(anmodningUnntakKontrollService).utførKontroller(BEHANDLING_ID);
        verify(anmodningsperiodeService).oppdaterAnmodetAvForBehandling(BEHANDLING_ID, "Z990007");
        verify(prosessinstansService).opprettProsessinstansAnmodningOmUnntak(any(Behandling.class),
            anySet(), eq(Set.of(dokumentReferanse)), eq(FRITEKST_SED));
        verify(oppgaveService).leggTilbakeBehandlingsoppgaveMedSaksnummer(any());
        verify(behandlingsresultatService).oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
    }

    @Test
    void anmodningOmUnntak_ikkeEessiReadyMottakerInstitusjonNull_prosessOpprettet() throws Exception {
        Behandling behandling = new Behandling();
        behandling.setMottatteOpplysninger(new MottatteOpplysninger());
        Fagsak fagsak = FagsakTestFactory.lagFagsak();
        behandling.setFagsak(fagsak);
        behandling.getSaksopplysninger().add(lagPersonSaksopplysning());
        when(behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID)).thenReturn(behandling);
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID)).thenReturn(Collections.singletonList(Land_iso2.SE));

        anmodningUnntakService.anmodningOmUnntak(BEHANDLING_ID, null, Collections.emptySet(), FRITEKST_SED);

        verify(anmodningUnntakKontrollService).utførKontroller(BEHANDLING_ID);
        verify(anmodningsperiodeService).oppdaterAnmodetAvForBehandling(BEHANDLING_ID, "Z990007");
        verify(prosessinstansService).opprettProsessinstansAnmodningOmUnntak(any(Behandling.class), anySet(),
            anySet(), eq(FRITEKST_SED));
        verify(oppgaveService).leggTilbakeBehandlingsoppgaveMedSaksnummer(any());
        verify(behandlingsresultatService).oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
    }

    @Test
    void anmodningOmUnntakSvar_validert_forventMetodekall() {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SEDOPPL);

        SedDokument sedDokument = new SedDokument();
        sedDokument.setRinaSaksnummer("55667788");
        saksopplysning.setDokument(sedDokument);

        behandling.setSaksopplysninger(Set.of(saksopplysning));

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(anyLong())).thenReturn(lagAnmodningsperiodeSvar());
        when(eessiService.kanOppretteSedTyperPåBuc(anyString(), any(SedType.class))).thenReturn(true);

        anmodningUnntakService.anmodningOmUnntakSvar(BEHANDLING_ID, FRITEKST_SED);

        verify(behandlingService).hentBehandling(BEHANDLING_ID);
        verify(anmodningsperiodeService).hentAnmodningsperiodeSvarForBehandling(BEHANDLING_ID);
        verify(lovvalgsperiodeService).lagreLovvalgsperioder(eq(BEHANDLING_ID), lovvalgsperioder.capture());
        assertThat(lovvalgsperioder.getValue()).containsExactly(
            Lovvalgsperiode.av(lagAnmodningsperiodeSvar(), Medlemskapstyper.PLIKTIG));
        verify(prosessinstansService).opprettProsessinstansAnmodningOmUnntakMottakSvar(behandling, FRITEKST_SED);
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(FagsakTestFactory.SAKSNUMMER);
        verify(behandlingsresultatService).oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.REGISTRERT_UNNTAK);
    }

    @Test
    void anmodningOmUnntakSvar_ikkeGodkjent_forventBehandlingsResultatFerdigbehandlet() {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SEDOPPL);

        SedDokument sedDokument = new SedDokument();
        sedDokument.setRinaSaksnummer("55667788");
        saksopplysning.setDokument(sedDokument);

        behandling.setSaksopplysninger(Set.of(saksopplysning));

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(anyLong())).thenReturn(lagAnmodningsperiodeSvarNegativt());
        when(eessiService.kanOppretteSedTyperPåBuc(anyString(), any(SedType.class))).thenReturn(true);

        anmodningUnntakService.anmodningOmUnntakSvar(BEHANDLING_ID, FRITEKST_SED);

        verify(behandlingsresultatService).oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.FERDIGBEHANDLET);
    }

    @Test
    void anmodningOmUnntakSvar_feilBehandlingstype_forventException() {
        Behandling behandling = lagBehandling();
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> anmodningUnntakService.anmodningOmUnntakSvar(BEHANDLING_ID, null))
            .withMessageContaining("Behandling er ikke av tema ANMODNING_OM_UNNTAK_HOVEDREGEL");
    }

    @Test
    void anmodningOmUnntakSvar_behandlingErAvsluttet_forventException() {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> anmodningUnntakService.anmodningOmUnntakSvar(BEHANDLING_ID, null))
            .withMessageContaining("Behandlingen er avsluttet");
    }

    @Test
    void anmodningOmUnntakSvar_avslagForLangFritekst_forventException() {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);

        AnmodningsperiodeSvar anmodningsperiodeSvar = lagAnmodningsperiodeSvar();
        anmodningsperiodeSvar.setBegrunnelseFritekst(RandomStringUtils.random(256));
        anmodningsperiodeSvar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.AVSLAG);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(anyLong())).thenReturn(anmodningsperiodeSvar);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> anmodningUnntakService.anmodningOmUnntakSvar(BEHANDLING_ID, null))
            .withMessageContaining("Kan ikke ha fritekst lengre enn 255 for avslag på anmodning om unntak");
    }

    @Test
    void anmodningOmUnntakSvar_kanIkkeOppretteSedPåBuc_forventException() {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SEDOPPL);

        SedDokument sedDokument = new SedDokument();
        sedDokument.setRinaSaksnummer("55667788");
        saksopplysning.setDokument(sedDokument);

        behandling.setSaksopplysninger(Set.of(saksopplysning));

        when(behandlingService.hentBehandling(BEHANDLING_ID)).thenReturn(behandling);
        when(anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(BEHANDLING_ID)).thenReturn(lagAnmodningsperiodeSvar());
        when(eessiService.kanOppretteSedTyperPåBuc("55667788", SedType.A011)).thenReturn(false);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> anmodningUnntakService.anmodningOmUnntakSvar(BEHANDLING_ID, null))
            .withMessageContaining("Kan ikke opprette SedType A011 på rinaSaknummer: 55667788");
    }

    private static Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setFagsak(FagsakTestFactory.lagFagsak());
        behandling.setId(BEHANDLING_ID);

        return behandling;
    }

    private static Saksopplysning lagPersonSaksopplysning() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.getBostedsadresse().setPostnr("2123");
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        saksopplysning.setDokument(personDokument);
        return saksopplysning;
    }

    private static AnmodningsperiodeSvar lagAnmodningsperiodeSvar() {
        AnmodningsperiodeSvar anmodningsperiodeSvar = new AnmodningsperiodeSvar();
        anmodningsperiodeSvar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        anmodningsperiodeSvar.setAnmodningsperiode(lagAnmodningsperiode());
        return anmodningsperiodeSvar;
    }

    private static AnmodningsperiodeSvar lagAnmodningsperiodeSvarNegativt() {
        AnmodningsperiodeSvar anmodningsperiodeSvar = new AnmodningsperiodeSvar();
        anmodningsperiodeSvar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.AVSLAG);
        anmodningsperiodeSvar.setAnmodningsperiode(lagAnmodningsperiode());
        return anmodningsperiodeSvar;
    }

    private static Anmodningsperiode lagAnmodningsperiode() {
        return new Anmodningsperiode(
            LocalDate.EPOCH,
            LocalDate.MAX,
            Land_iso2.NO, Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1, null,
            Land_iso2.SE, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, Trygdedekninger.FULL_DEKNING_EOSFO);
    }
}
