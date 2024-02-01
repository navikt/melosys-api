package no.nav.melosys.service.sak;

import java.util.Arrays;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.service.SaksbehandlingDataFactory.lagFagsak;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HenleggFagsakServiceTest {

    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private BehandlingService behandlingService;

    private HenleggFagsakService henleggFagsakService;

    @Captor
    private ArgumentCaptor<Behandlingsresultat> behandlingsresultatCaptor;

    private final Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
    private final Fagsak fagsak = new Fagsak();
    private final Behandling behandling = new Behandling();

    private final String saksnummer = "MEL-0";
    private final long behandlingID = 11;

    @BeforeEach
    public void setup() {
        henleggFagsakService = new HenleggFagsakService(fagsakService, behandlingsresultatService, prosessinstansService, oppgaveService, behandlingService);

        behandling.setId(behandlingID);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        behandling.setFagsak(fagsak);
        fagsak.setSaksnummer(saksnummer);
        fagsak.getBehandlinger().add(behandling);
    }

    @Test
    void henleggFagsak_gyldigHenleggelsesgrunn_behandlingsresultatBlirOppdatert() {
        String fritekst = "Fri tale";
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);
        when(fagsakService.hentFagsak(saksnummer)).thenReturn(fagsak);

        henleggFagsakService.henleggFagsakEllerBehandling(saksnummer, "ANNET", fritekst);

        verify(behandlingsresultatService).lagre(behandlingsresultatCaptor.capture());
        verify(prosessinstansService).opprettProsessinstansFagsakHenlagt(behandling);
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(fagsak.getSaksnummer());

        assertThat(behandlingsresultat)
            .extracting(Behandlingsresultat::getType, Behandlingsresultat::getBegrunnelseFritekst)
            .containsExactly(Behandlingsresultattyper.HENLEGGELSE, fritekst);
    }

    @Test
    void henleggFagsak_avslutterKunBehandling_nårBehandlingTypeErNyVurdering() {
        when(fagsakService.hentFagsak(saksnummer)).thenReturn(fagsak);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);
        behandling.setType(Behandlingstyper.NY_VURDERING);

        henleggFagsakService.henleggFagsakEllerBehandling(saksnummer, "ANNET", "Never say never");

        verify(behandlingService).avsluttAndregangsbehandling(behandlingID, Behandlingsresultattyper.HENLEGGELSE);
        verify(prosessinstansService).opprettProsessinstansFagsakHenlagt(behandling);
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(fagsak.getSaksnummer());
        verifyNoMoreInteractions(fagsakService);
    }

    @Test
    void henleggFagsak_avslutterKunBehandling_nårBehandlingTypeErManglendeInnbetalingTrygdeavgift() {
        when(fagsakService.hentFagsak(saksnummer)).thenReturn(fagsak);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);
        behandling.setType(Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT);


        henleggFagsakService.henleggFagsakEllerBehandling(saksnummer, "ANNET", "Never say never");


        verify(behandlingService).avsluttAndregangsbehandling(behandlingID, Behandlingsresultattyper.HENLEGGELSE);
        verify(prosessinstansService).opprettProsessinstansFagsakHenlagt(behandling);
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(fagsak.getSaksnummer());
        verifyNoMoreInteractions(fagsakService);
    }

    @Test
    void henleggFagsak_avslutterKunBehandling_nårBehandlingTypeIkkeErNyVurdering() {
        when(fagsakService.hentFagsak(saksnummer)).thenReturn(fagsak);
        when(behandlingsresultatService.hentBehandlingsresultat(behandlingID)).thenReturn(behandlingsresultat);
        behandling.setType(Behandlingstyper.FØRSTEGANG);

        henleggFagsakService.henleggFagsakEllerBehandling(saksnummer, "ANNET", "- Justin Bieber");

        verify(fagsakService).avsluttFagsakOgBehandling(fagsak, Saksstatuser.HENLAGT);
        verify(prosessinstansService).opprettProsessinstansFagsakHenlagt(behandling);
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(fagsak.getSaksnummer());
        verifyNoMoreInteractions(behandlingService);
    }

    @Test
    void henleggFagsak_ikkeGyldigHenleggelsesgrunn_kasterException() {
        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> henleggFagsakService.henleggFagsakEllerBehandling(saksnummer, "UGYLDIGKODE", "Fri tale"))
            .withMessageContaining("ingen gyldig henleggelsesgrunn");
    }

    @Test
    void henleggSomBortfalt_fagsakMedFlereBehandlinger_avslutterAktivBehandlingOgStatusBlirHENLAGT_BORTFALT() {
        String saksnummer = "saksnummer";
        Fagsak fagsak = lagFagsak(saksnummer);
        Behandling førsteBehandling = new Behandling();
        førsteBehandling.setId(1L);
        førsteBehandling.setStatus(Behandlingsstatus.AVSLUTTET);
        Behandling andreBehandling = new Behandling();
        andreBehandling.setId(2L);
        andreBehandling.setStatus(Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        fagsak.setBehandlinger(Arrays.asList(førsteBehandling, andreBehandling));
        when(fagsakService.hentFagsak(saksnummer)).thenReturn(fagsak);

        henleggFagsakService.henleggSakEllerBehandlingSomBortfalt(saksnummer);

        verify(fagsakService).lagre(fagsak);
        verify(behandlingsresultatService).oppdaterBehandlingsresultattype(1L, Behandlingsresultattyper.HENLEGGELSE_BORTFALT);
        verify(behandlingsresultatService).oppdaterBehandlingsresultattype(2L, Behandlingsresultattyper.HENLEGGELSE_BORTFALT);
        verify(behandlingService, never()).avsluttBehandling(1L);
        verify(behandlingService).avsluttBehandling(2L);
        assertThat(fagsak.getStatus()).isEqualTo(Saksstatuser.HENLAGT_BORTFALT);
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(saksnummer);
    }

    @Test
    void henleggSomBortfalt_avslutterKunBehandling_dersomBehandlingTypeErNyVurdering() {
        String saksnummer = "saksnummer";
        Fagsak fagsak = lagFagsak(saksnummer);
        Behandling førsteBehandling = new Behandling();
        førsteBehandling.setId(1L);
        førsteBehandling.setStatus(Behandlingsstatus.AVSLUTTET);
        Behandling andreBehandling = new Behandling();
        andreBehandling.setId(2L);
        andreBehandling.setType(Behandlingstyper.NY_VURDERING);
        fagsak.setBehandlinger(Arrays.asList(førsteBehandling, andreBehandling));
        when(fagsakService.hentFagsak(saksnummer)).thenReturn(fagsak);

        henleggFagsakService.henleggSakEllerBehandlingSomBortfalt(saksnummer);

        verify(behandlingService).avsluttAndregangsbehandling(andreBehandling.getId(), Behandlingsresultattyper.HENLEGGELSE_BORTFALT);
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(saksnummer);
        verifyNoMoreInteractions(fagsakService, behandlingsresultatService, oppgaveService);
    }

    @Test
    void henleggSomBortfalt_avslutterKunBehandling_dersomBehandlingTypeErManglendeInnbetalingTrygdeavgift() {
        String saksnummer = "saksnummer";
        Fagsak fagsak = lagFagsak(saksnummer);
        Behandling førsteBehandling = new Behandling();
        førsteBehandling.setId(1L);
        førsteBehandling.setStatus(Behandlingsstatus.AVSLUTTET);
        Behandling andreBehandling = new Behandling();
        andreBehandling.setId(2L);
        andreBehandling.setType(Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT);
        fagsak.setBehandlinger(Arrays.asList(førsteBehandling, andreBehandling));
        when(fagsakService.hentFagsak(saksnummer)).thenReturn(fagsak);


        henleggFagsakService.henleggSakEllerBehandlingSomBortfalt(saksnummer);


        verify(behandlingService).avsluttAndregangsbehandling(andreBehandling.getId(), Behandlingsresultattyper.HENLEGGELSE_BORTFALT);
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(saksnummer);
        verifyNoMoreInteractions(fagsakService, behandlingsresultatService, oppgaveService);
    }
}
