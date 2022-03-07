package no.nav.melosys.service.behandling;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingEndretAvSaksbehandlerEvent;
import no.nav.melosys.domain.BehandlingsfristEndretEvent;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.dokument.DokumentBestiltEvent;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BehandlingEventListenerTest {

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private OppgaveService oppgaveService;

    @Captor
    private ArgumentCaptor<OppgaveOppdatering> oppgaveOppdateringCaptor;

    private BehandlingEventListener behandlingEventListener;

    private final long BEHANDLING_ID = 123321L;
    private final String FAGSAKSNUMMER = "222";
    private final String OPPGAVE_ID = "333";

    private final Behandling behandling = new Behandling();

    @BeforeEach
    public void setup() {
        behandling.setId(BEHANDLING_ID);
        behandlingEventListener = new BehandlingEventListener(behandlingService, oppgaveService);
    }

    @Test
    void dokumentBestilt_dokumentErInnvilgelsesbrev_ingenAksjon() {
        behandlingEventListener.dokumentBestilt(new DokumentBestiltEvent(BEHANDLING_ID, Produserbaredokumenter.INNVILGELSE_YRKESAKTIV));
        verifyNoInteractions(behandlingService);
    }

    @Test
    void dokumentBestilt_dokumentErMangelbrevBehandlingIkkeAktiv_ingenAksjon() {
        behandling.setStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);
        when(behandlingService.hentBehandling(BEHANDLING_ID)).thenReturn(behandling);
        behandlingEventListener.dokumentBestilt(new DokumentBestiltEvent(BEHANDLING_ID, Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER));
        verify(behandlingService).hentBehandling(BEHANDLING_ID);
        verifyNoMoreInteractions(behandlingService);
    }

    @Test
    void dokumentBestilt_dokumentErMangelbrevBehandlingErAktiv_oppdatererStatusOgFrist() {
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        when(behandlingService.hentBehandling(BEHANDLING_ID)).thenReturn(behandling);
        behandlingEventListener.dokumentBestilt(new DokumentBestiltEvent(BEHANDLING_ID, Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER));
        verify(behandlingService).oppdaterStatusOgSvarfrist(eq(behandling), eq(Behandlingsstatus.AVVENT_DOK_PART), any(Instant.class));
    }

    @Test
    void dokumentBestilt_dokumentErNyttMangelbrevTilBrukerBehandlingErAktiv_oppdatererStatusOgFrist() {
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        when(behandlingService.hentBehandling(BEHANDLING_ID)).thenReturn(behandling);
        behandlingEventListener.dokumentBestilt(new DokumentBestiltEvent(BEHANDLING_ID, Produserbaredokumenter.MANGELBREV_BRUKER));
        verify(behandlingService).oppdaterStatusOgSvarfrist(eq(behandling), eq(Behandlingsstatus.AVVENT_DOK_PART), any(Instant.class));
    }

    @Test
    void dokumentBestilt_dokumentErNyttMangelbrevTilArbeidsgiverBehandlingErAktiv_oppdatererStatusOgFrist() {
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        when(behandlingService.hentBehandling(BEHANDLING_ID)).thenReturn(behandling);
        behandlingEventListener.dokumentBestilt(new DokumentBestiltEvent(BEHANDLING_ID, Produserbaredokumenter.MANGELBREV_ARBEIDSGIVER));
        verify(behandlingService).oppdaterStatusOgSvarfrist(eq(behandling), eq(Behandlingsstatus.AVVENT_DOK_PART), any(Instant.class));
    }

    @Test
    void behandlingsfristEndret_enUkeFrem_oppdatererFrist() {
        LocalDate nå = LocalDate.now();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(FAGSAKSNUMMER);
        behandling.setBehandlingsfrist(nå);
        behandling.setFagsak(fagsak);
        Oppgave oppgave = new Oppgave.Builder().setOppgaveId(OPPGAVE_ID).build();

        when(behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID)).thenReturn(behandling);
        when(oppgaveService.finnÅpenOppgaveMedFagsaksnummer(FAGSAKSNUMMER)).thenReturn(Optional.of(oppgave));

        behandlingEventListener.behandlingsfristEndret(new BehandlingsfristEndretEvent(BEHANDLING_ID, nå.plusWeeks(1)));

        verify(behandlingService, only()).hentBehandlingMedSaksopplysninger(BEHANDLING_ID);
        verify(oppgaveService).oppdaterOppgave(eq(OPPGAVE_ID), oppgaveOppdateringCaptor.capture());
        assertThat(oppgaveOppdateringCaptor.getValue().getFristFerdigstillelse()).isEqualTo(nå.plusWeeks(1));
    }

    @Test
    void behandlingEndret_oppdatererOppgave_medRiktigData() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(FAGSAKSNUMMER);
        behandling.setType(Behandlingstyper.NY_VURDERING);
        behandling.setTema(Behandlingstema.IKKE_YRKESAKTIV);
        behandling.setBehandlingsfrist(LocalDate.of(2022, 3, 7));
        behandling.setFagsak(fagsak);
        when(behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID)).thenReturn(behandling);
        BehandlingEndretAvSaksbehandlerEvent behandlingEndretAvSaksbehandlerEvent = new BehandlingEndretAvSaksbehandlerEvent(
            BEHANDLING_ID,
            behandling
        );
        Oppgave oppgave = new Oppgave.Builder().setOppgaveId(OPPGAVE_ID).build();
        when(oppgaveService.finnÅpenOppgaveMedFagsaksnummer(FAGSAKSNUMMER)).thenReturn(Optional.of(oppgave));

        behandlingEventListener.behandlingEndret(behandlingEndretAvSaksbehandlerEvent);

        Oppgave behandlingsOppgaveForType = OppgaveFactory.lagBehandlingsOppgaveForType(behandling.getTema(), behandling.getType()).build();
        verify(oppgaveService).oppdaterOppgave(eq(OPPGAVE_ID), oppgaveOppdateringCaptor.capture());
        OppgaveOppdatering capturedOppgaveOppdatering = oppgaveOppdateringCaptor.getValue();
        assertThat(capturedOppgaveOppdatering.getBehandlingstema()).isEqualTo(behandlingsOppgaveForType.getBehandlingstema());
        assertThat(capturedOppgaveOppdatering.getBehandlingstype()).isEqualTo(behandlingsOppgaveForType.getBehandlingstype());
        assertThat(capturedOppgaveOppdatering.getTema()).isEqualTo(behandlingsOppgaveForType.getTema());
        assertThat(capturedOppgaveOppdatering.getFristFerdigstillelse()).isEqualTo(LocalDate.of(2022, 3, 7));
    }
}
