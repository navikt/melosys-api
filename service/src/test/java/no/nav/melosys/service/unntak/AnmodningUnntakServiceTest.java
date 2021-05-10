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
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.kontroll.unntak.AnmodningUnntakKontrollService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
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

    private AnmodningUnntakService anmodningUnntakService;

    @BeforeEach
    public void setUp() {
        anmodningUnntakService = new AnmodningUnntakService(
            behandlingService, behandlingsresultatService, oppgaveService, prosessinstansService, anmodningsperiodeService,
            lovvalgsperiodeService, landvelgerService, eessiService, anmodningUnntakKontrollService);
    }

    @Test
    void anmodningOmUnntak_erEessiKlarMedMottakerInstitusjon_prosessOpprettet() throws Exception {
        final long behandlingID = 1L;
        final String mottakerInstitusjon = "SE:432";
        final DokumentReferanse dokumentReferanse = new DokumentReferanse("jpID", "dokID");
        final String fritekstSed = "friteksssst";
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-111");
        behandling.setFagsak(fagsak);
        behandling.getSaksopplysninger().add(lagPersonSaksopplysning());
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        when(behandlingService.hentBehandling(behandlingID)).thenReturn(behandling);
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)).thenReturn(Collections.singletonList(Landkoder.SE));

        anmodningUnntakService.anmodningOmUnntak(behandlingID, mottakerInstitusjon, Set.of(dokumentReferanse), fritekstSed);

        verify(anmodningUnntakKontrollService).utførKontroller(behandlingID);
        verify(prosessinstansService).opprettProsessinstansAnmodningOmUnntak(any(Behandling.class),
            anySet(), eq(Set.of(dokumentReferanse)), eq(fritekstSed));
        verify(oppgaveService).leggTilbakeOppgaveMedSaksnummer(any());
        verify(behandlingsresultatService).oppdaterBehandlingsresultattype(behandlingID, Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
    }

    @Test
    void anmodningOmUnntak_ikkeEessiReadyMottakerInstitusjonNull_prosessOpprettet() throws Exception {
        final long behandlingID = 1L;
        final String fritekstSed = "friteksssst";
        Behandling behandling = new Behandling();
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-111");
        behandling.setFagsak(fagsak);
        behandling.getSaksopplysninger().add(lagPersonSaksopplysning());
        when(behandlingService.hentBehandling(behandlingID)).thenReturn(behandling);
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(behandlingID)).thenReturn(Collections.singletonList(Landkoder.SE));

        anmodningUnntakService.anmodningOmUnntak(behandlingID, null, Collections.emptySet(), fritekstSed);

        verify(anmodningUnntakKontrollService).utførKontroller(behandlingID);
        verify(prosessinstansService).opprettProsessinstansAnmodningOmUnntak(any(Behandling.class), anySet(),
            anySet(), eq(fritekstSed));
        verify(oppgaveService).leggTilbakeOppgaveMedSaksnummer(any());
        verify(behandlingsresultatService).oppdaterBehandlingsresultattype(behandlingID, Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
    }

    @Test
    void anmodningOmUnntakSvar_validert_forventMetodekall() throws FunksjonellException, TekniskException {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);

        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);
        when(anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(anyLong())).thenReturn(Collections.singletonList(new AnmodningsperiodeSvar()));
        when(lovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(Collections.singletonList(new Lovvalgsperiode()));

        anmodningUnntakService.anmodningOmUnntakSvar(1L);

        verify(behandlingService).hentBehandlingUtenSaksopplysninger(1L);
        verify(anmodningsperiodeService).hentAnmodningsperiodeSvarForBehandling(1L);
        verify(lovvalgsperiodeService).hentLovvalgsperioder(1L);
        verify(prosessinstansService).opprettProsessinstansAnmodningOmUnntakMottakSvar(behandling);
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer("MEL-111");
    }

    @Test
    void anmodningOmUnntakSvar_feilBehandlingstype_forventException() throws FunksjonellException {
        Behandling behandling = lagBehandling();
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> anmodningUnntakService.anmodningOmUnntakSvar(1L))
            .withMessageContaining("Behandling er ikke av tema ANMODNING_OM_UNNTAK_HOVEDREGEL");
    }

    @Test
    void anmodningOmUnntakSvar_behandlingErAvsluttet_forventException() throws FunksjonellException {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> anmodningUnntakService.anmodningOmUnntakSvar(1L))
            .withMessageContaining("Behandlingen er avsluttet");
    }

    @Test
    void anmodningOmUnntakSvar_behandlingHarIngenAnmodningsperiodeSvar_forventException() throws FunksjonellException {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> anmodningUnntakService.anmodningOmUnntakSvar(1L))
            .withMessageContaining("Finner ingen AnmodningsperiodeSvar for behandling 1");
    }

    @Test
    void anmodningOmUnntakSvar_behandlingHarIngenLovvalgsperiode_forventException() throws FunksjonellException {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);

        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);
        when(anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(anyLong())).thenReturn(Collections.singletonList(new AnmodningsperiodeSvar()));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> anmodningUnntakService.anmodningOmUnntakSvar(1L))
            .withMessageContaining("Finner ingen Lovvalgsperioder for behandling 1");
    }

    @Test
    void anmodningOmUnntakSvar_avslagForLangFritekst_forventException() throws FunksjonellException {
        Behandling behandling = lagBehandling();
        behandling.setTema(Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);

        AnmodningsperiodeSvar anmodningsperiodeSvar = new AnmodningsperiodeSvar();
        anmodningsperiodeSvar.setBegrunnelseFritekst(RandomStringUtils.random(256));
        anmodningsperiodeSvar.setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.AVSLAG);

        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);
        when(anmodningsperiodeService.hentAnmodningsperiodeSvarForBehandling(anyLong())).thenReturn(Collections.singletonList(anmodningsperiodeSvar));
        when(lovvalgsperiodeService.hentLovvalgsperioder(anyLong())).thenReturn(Collections.singletonList(new Lovvalgsperiode()));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> anmodningUnntakService.anmodningOmUnntakSvar(1L))
            .withMessageContaining("Kan ikke ha fritekst lengre enn 255 for avslag på anmodning om unntak");
    }

    private static Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-111");
        behandling.setFagsak(fagsak);
        behandling.setId(1L);

        return behandling;
    }

    private static Saksopplysning lagPersonSaksopplysning() {
        PersonDokument personDokument = new PersonDokument();
        personDokument.bostedsadresse.setPostnr("2123");
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        saksopplysning.setDokument(personDokument);
        return saksopplysning;
    }
}
