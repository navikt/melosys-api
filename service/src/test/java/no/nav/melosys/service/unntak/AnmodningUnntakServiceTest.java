package no.nav.melosys.service.unntak;

import java.util.Collections;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.arkiv.DokumentReferanse;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.service.kontroll.feature.unntak.AnmodningUnntakKontrollService;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private static final String SAKSNUMMER = "MEL-111";
    private static final String MOTTAKER_INSTITUSJON = "SE:432";

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
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        behandling.setFagsak(fagsak);
        behandling.getSaksopplysninger().add(lagPersonSaksopplysning());
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        when(behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID)).thenReturn(behandling);
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID)).thenReturn(Collections.singletonList(Landkoder.SE));

        anmodningUnntakService.anmodningOmUnntak(BEHANDLING_ID, MOTTAKER_INSTITUSJON, Set.of(dokumentReferanse), FRITEKST_SED);

        verify(anmodningUnntakKontrollService).utførKontroller(BEHANDLING_ID);
        verify(anmodningsperiodeService).oppdaterAnmodetAvForBehandling(BEHANDLING_ID, "Z990007");
        verify(prosessinstansService).opprettProsessinstansAnmodningOmUnntak(any(Behandling.class),
            anySet(), eq(Set.of(dokumentReferanse)), eq(FRITEKST_SED));
        verify(oppgaveService).leggTilbakeOppgaveMedSaksnummer(any());
        verify(behandlingsresultatService).oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
    }

    @Test
    void anmodningOmUnntak_ikkeEessiReadyMottakerInstitusjonNull_prosessOpprettet() throws Exception {
        Behandling behandling = new Behandling();
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        behandling.setFagsak(fagsak);
        behandling.getSaksopplysninger().add(lagPersonSaksopplysning());
        when(behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID)).thenReturn(behandling);
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(BEHANDLING_ID)).thenReturn(Collections.singletonList(Landkoder.SE));

        anmodningUnntakService.anmodningOmUnntak(BEHANDLING_ID, null, Collections.emptySet(), FRITEKST_SED);

        verify(anmodningUnntakKontrollService).utførKontroller(BEHANDLING_ID);
        verify(anmodningsperiodeService).oppdaterAnmodetAvForBehandling(BEHANDLING_ID, "Z990007");
        verify(prosessinstansService).opprettProsessinstansAnmodningOmUnntak(any(Behandling.class), anySet(),
            anySet(), eq(FRITEKST_SED));
        verify(oppgaveService).leggTilbakeOppgaveMedSaksnummer(any());
        verify(behandlingsresultatService).oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
    }

    @Test
    void anmodningOmUnntakSvar_validert_forventMetodekall() {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(anyLong())).thenReturn(Collections.singletonList(new AnmodningsperiodeSvar()));
        when(lovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(Collections.singletonList(new Lovvalgsperiode()));

        anmodningUnntakService.anmodningOmUnntakSvar(BEHANDLING_ID, FRITEKST_SED);

        verify(behandlingService).hentBehandling(BEHANDLING_ID);
        verify(anmodningsperiodeService).hentAnmodningsperiodeSvarForBehandling(BEHANDLING_ID);
        verify(lovvalgsperiodeService).hentLovvalgsperioder(BEHANDLING_ID);
        verify(prosessinstansService).opprettProsessinstansAnmodningOmUnntakMottakSvar(behandling, FRITEKST_SED);
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(SAKSNUMMER);
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
    void anmodningOmUnntakSvar_behandlingHarIngenAnmodningsperiodeSvar_forventException() {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> anmodningUnntakService.anmodningOmUnntakSvar(BEHANDLING_ID, null))
            .withMessageContaining("Finner ingen AnmodningsperiodeSvar for behandling 1");
    }

    @Test
    void anmodningOmUnntakSvar_behandlingHarIngenLovvalgsperiode_forventException() {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(anyLong())).thenReturn(Collections.singletonList(new AnmodningsperiodeSvar()));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> anmodningUnntakService.anmodningOmUnntakSvar(BEHANDLING_ID, null))
            .withMessageContaining("Finner ingen Lovvalgsperioder for behandling 1");
    }

    @Test
    void anmodningOmUnntakSvar_avslagForLangFritekst_forventException() {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);

        AnmodningsperiodeSvar anmodningsperiodeSvar = new AnmodningsperiodeSvar();
        anmodningsperiodeSvar.setBegrunnelseFritekst(RandomStringUtils.random(256));
        anmodningsperiodeSvar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.AVSLAG);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(anyLong())).thenReturn(Collections.singletonList(anmodningsperiodeSvar));
        when(lovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(Collections.singletonList(new Lovvalgsperiode()));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> anmodningUnntakService.anmodningOmUnntakSvar(BEHANDLING_ID, null))
            .withMessageContaining("Kan ikke ha fritekst lengre enn 255 for avslag på anmodning om unntak");
    }

    private static Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        behandling.setFagsak(fagsak);
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
}
