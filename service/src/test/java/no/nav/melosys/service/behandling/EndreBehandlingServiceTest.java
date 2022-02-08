package no.nav.melosys.service.behandling;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static no.nav.melosys.domain.Behandling.BEHANDLINGSTEMA_SED_FORESPØRSEL;
import static no.nav.melosys.domain.Behandling.BEHANDLINGSTEMA_SØKNAD;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EndreBehandlingServiceTest {

    private static final long BEHANDLING_ID = 11L;
    private static final Behandlingstyper BEHANDLING_TYPE = Behandlingstyper.SOEKNAD;
    private static final Behandlingstema BEHANDLING_TEMA = Behandlingstema.ARBEID_I_UTLANDET;
    private static final Behandlingsstatus BEHANDLING_STATUS = UNDER_BEHANDLING;
    private static final LocalDate BEHANDLING_FRIST = LocalDate.now().plusMonths(1);
    private Behandlingsresultat BEHANDLINGSRESULTAT = new Behandlingsresultat();

    private Behandling behandling;

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private BehandlingsgrunnlagService behandlingsgrunnlagService;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Captor
    private ArgumentCaptor<Behandling> behandlingCaptor;
    @Captor
    private ArgumentCaptor<OppgaveOppdatering> oppgaveOppdateringCaptor;
    @Captor
    private ArgumentCaptor<Behandlingsgrunnlag> behandlingsgrunnlagCaptor;
    @Captor
    private ArgumentCaptor<BehandlingEndretEvent> behandlingEndretEventCaptor;

    private EndreBehandlingService endreBehandlingService;

    @BeforeEach
    void setUp() {
        endreBehandlingService = new EndreBehandlingService(
            behandlingService,
            behandlingsresultatService,
            oppgaveService,
            behandlingsgrunnlagService,
            behandlingRepository,
            applicationEventPublisher
        );

        behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
    }

    @Test
    void endreBehandling() {
        behandling.setTema(Behandlingstema.IKKE_YRKESAKTIV);
        behandling.setFagsak(new Fagsak());
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);

        endreBehandlingService.endreBehandling(BEHANDLING_ID, Sakstyper.EU_EOS, BEHANDLING_TYPE, BEHANDLING_TEMA, BEHANDLING_STATUS, BEHANDLING_FRIST);

        verify(behandlingService).oppdaterStatus(behandling, BEHANDLING_STATUS);
        verify(behandlingRepository).save(behandlingCaptor.capture());

        var lagretBehandling = behandlingCaptor.getValue();
        assertThat(lagretBehandling.getId()).isEqualTo(BEHANDLING_ID);
        assertThat(lagretBehandling.getType()).isEqualTo(BEHANDLING_TYPE);
        assertThat(lagretBehandling.getTema()).isEqualTo(BEHANDLING_TEMA);
        assertThat(lagretBehandling.getBehandlingsfrist()).isEqualTo(BEHANDLING_FRIST);

        verify(applicationEventPublisher).publishEvent(behandlingEndretEventCaptor.capture());
        var behandlingEndretEvent = behandlingEndretEventCaptor.getValue();

        assertThat(behandlingEndretEvent.getBehandlingID()).isEqualTo(BEHANDLING_ID);
        assertThat(behandlingEndretEvent.getBehandlingstype()).isEqualTo(BEHANDLING_TYPE);
        assertThat(behandlingEndretEvent.getBehandlingstema()).isEqualTo(BEHANDLING_TEMA);
        assertThat(behandlingEndretEvent.getBehandlingsfrist()).isEqualTo(BEHANDLING_FRIST);
    }

    @Test
    void brukerOppdaterStatus_nyStatusErIkkeGyldig() {
        behandling.setTema(Behandlingstema.ARBEID_I_UTLANDET);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> endreBehandlingService.endreStatus(BEHANDLING_ID, AVSLUTTET))
            .withMessageContaining("Behandlingen kan ikke endres til status AVSLUTTET. Gyldige statuser for ");
    }

    @Test
    void hentMuligeStatuser_temaOvrigeSedMed_avsluttetErMulig() {
        behandling.setTema(Behandlingstema.ØVRIGE_SED_MED);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);

        Collection<Behandlingsstatus> muligeStatuser = endreBehandlingService.hentMuligeStatuser(BEHANDLING_ID);
        assertThat(muligeStatuser).containsExactlyInAnyOrder(AVVENT_DOK_PART, AVVENT_DOK_UTL, UNDER_BEHANDLING, AVVENT_FAGLIG_AVKLARING, AVSLUTTET);
    }

    @Test
    void hentMuligeStatuser_temaArbeidUtland_avsluttetErIkkeMulig() {
        behandling.setTema(Behandlingstema.ARBEID_I_UTLANDET);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);

        Collection<Behandlingsstatus> muligeStatuser = endreBehandlingService.hentMuligeStatuser(BEHANDLING_ID);
        assertThat(muligeStatuser).containsExactlyInAnyOrder(AVVENT_DOK_PART, AVVENT_DOK_UTL, UNDER_BEHANDLING, AVVENT_FAGLIG_AVKLARING);
    }

    @Test
    void hentMuligeBehandlingstema_gyldigSøknadBehandlingstema_returnererSøknadBehandlingstema() {
        behandling.setTema(ARBEID_FLERE_LAND);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(eq(BEHANDLING_ID))).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(BEHANDLINGSRESULTAT);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingService.hentMuligeBehandlingstema(BEHANDLING_ID);
        assertThat(BEHANDLINGSTEMA_SØKNAD).isEqualTo(muligeBehandlingstema);
    }

    @Test
    void hentMuligeBehandlingstema_gyldigSEDForespørselBehandlingstema_returnererSEDForespørselBehandlingstema() {
        behandling.setTema(ØVRIGE_SED_MED);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(eq(BEHANDLING_ID))).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(BEHANDLINGSRESULTAT);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingService.hentMuligeBehandlingstema(BEHANDLING_ID);
        assertThat(BEHANDLINGSTEMA_SED_FORESPØRSEL).isEqualTo(muligeBehandlingstema);
    }

    @Test
    void hentMuligeBehandlingstema_ugyldigBehandlingstema_returnererTomListe() {
        behandling.setTema(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(BEHANDLINGSRESULTAT);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingService.hentMuligeBehandlingstema(BEHANDLING_ID);
        assertThat(muligeBehandlingstema).isEmpty();
    }

    @Test
    void hentMuligeBehandlingstema_inaktivBehandling_returnererTomListe() {
        behandling.setTema(ARBEID_FLERE_LAND);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingService.hentMuligeBehandlingstema(BEHANDLING_ID);
        assertThat(muligeBehandlingstema).isEmpty();
    }

    @Test
    void hentMuligeBehandlingstema_erArtikkel16MedSendtAnmodningOmUnntak_returnererTomListe() {
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setSendtUtland(true);
        BEHANDLINGSRESULTAT.setAnmodningsperioder(Set.of(anmodningsperiode));
        behandling.setTema(ARBEID_FLERE_LAND);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(eq(BEHANDLING_ID))).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(BEHANDLINGSRESULTAT);

        List<Behandlingstema> muligeBehandlingstema = endreBehandlingService.hentMuligeBehandlingstema(BEHANDLING_ID);
        assertThat(muligeBehandlingstema).isEmpty();
    }

    @Test
    void endreBehandlingstema_gyldigEndringForSøknad_behandlingLagresBehandlingsresultatTømmesOgOppgaveOppdateres() {
        behandling.setTema(ARBEID_FLERE_LAND);
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(new BehandlingsgrunnlagData());
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(anyLong())).thenReturn(behandling);

        setup_endreBehandlingstemaTester();
        Oppgave behandlingsOppgaveForType = OppgaveFactory.lagBehandlingsOppgaveForType(UTSENDT_ARBEIDSTAKER, behandling.getType()).build();

        endreBehandlingService.endreBehandlingstemaTilBehandling(BEHANDLING_ID, UTSENDT_ARBEIDSTAKER);
        verify(behandlingService).lagre(behandlingCaptor.capture());
        verify(behandlingsresultatService).tømBehandlingsresultat(BEHANDLING_ID);
        verify(oppgaveService).oppdaterOppgave(any(String.class), oppgaveOppdateringCaptor.capture());
        assertThat(behandlingCaptor.getValue().getTema()).isEqualTo(UTSENDT_ARBEIDSTAKER);
        assertThat(behandlingCaptor.getValue().getId()).isEqualTo(BEHANDLING_ID);
        assertThat(oppgaveOppdateringCaptor.getValue().getBehandlingstema()).isEqualTo(behandlingsOppgaveForType.getBehandlingstema());
    }

    @Test
    void endreBehandlingstema_gyldigEndringForSED_behandlingLagresBehandlingsresultatTømmesOgOppgaveOppdateres() {
        behandling.setTema(TRYGDETID);
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(new BehandlingsgrunnlagData());
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(eq(BEHANDLING_ID))).thenReturn(behandling);
        setup_endreBehandlingstemaTester();
        Oppgave behandlingsOppgaveForType = OppgaveFactory.lagBehandlingsOppgaveForType(ØVRIGE_SED_MED, behandling.getType()).build();

        endreBehandlingService.endreBehandlingstemaTilBehandling(BEHANDLING_ID, ØVRIGE_SED_MED);
        verify(behandlingService).lagre(behandlingCaptor.capture());
        verify(behandlingsresultatService).tømBehandlingsresultat(BEHANDLING_ID);
        verify(oppgaveService).oppdaterOppgave(any(String.class), oppgaveOppdateringCaptor.capture());
        assertThat(behandlingCaptor.getValue().getTema()).isEqualTo(ØVRIGE_SED_MED);
        assertThat(behandlingCaptor.getValue().getId()).isEqualTo(BEHANDLING_ID);
        assertThat(oppgaveOppdateringCaptor.getValue().getBehandlingstema()).isEqualTo(behandlingsOppgaveForType.getBehandlingstema());
    }

    @Test
    void endreBehandlingstema_ugyldigNyttTemaForSøknad_exceptionKastes() {
        behandling.setTema(ARBEID_FLERE_LAND);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(eq(BEHANDLING_ID))).thenReturn(behandling);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(BEHANDLINGSRESULTAT);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> endreBehandlingService.endreBehandlingstemaTilBehandling(BEHANDLING_ID, ØVRIGE_SED_MED))
            .withMessage("Ikke mulig å endre behandlingstema");
        verify(behandlingService, never()).lagre(any(Behandling.class));
        verify(behandlingsresultatService, never()).tømBehandlingsresultat(BEHANDLING_ID);
        verify(oppgaveService, never()).oppdaterOppgave(any(), any());
    }

    @Test
    void endreBehandlingstema_nyttTemaErIkkeArbeidIFlereLand_erUkjenteEllerAlleEosLandSettesTilFalse() {
        behandling.setTema(ARBEID_FLERE_LAND);
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(new BehandlingsgrunnlagData());
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);
        when(behandlingService.hentBehandlingUtenSaksopplysninger(eq(BEHANDLING_ID))).thenReturn(behandling);
        setup_endreBehandlingstemaTester();

        endreBehandlingService.endreBehandlingstemaTilBehandling(BEHANDLING_ID, UTSENDT_ARBEIDSTAKER);

        verify(behandlingsgrunnlagService).oppdaterBehandlingsgrunnlag(behandlingsgrunnlagCaptor.capture());
        assertThat(behandlingsgrunnlagCaptor.getValue().getBehandlingsgrunnlagdata().soeknadsland.erUkjenteEllerAlleEosLand).isFalse();
    }

    void setup_endreBehandlingstemaTester() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("saksnummer");
        behandling.setFagsak(fagsak);
        Oppgave oppgave = new Oppgave.Builder()
            .setOppgaveId("oppgaveID")
            .setSaksnummer(behandling.getFagsak().getSaksnummer())
            .build();
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(BEHANDLINGSRESULTAT);
        when(oppgaveService.finnÅpenOppgaveMedFagsaksnummer(fagsak.getSaksnummer())).thenReturn(Optional.of(oppgave));
    }
}
