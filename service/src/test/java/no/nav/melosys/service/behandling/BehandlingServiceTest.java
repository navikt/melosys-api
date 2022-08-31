package no.nav.melosys.service.behandling;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsgrunnlagRepository;
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.sak.LovligeKombinasjoner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BehandlingServiceTest {

    private static final long BEHANDLING_ID = 11L;
    private static final Behandlingstyper BEHANDLING_TYPE = Behandlingstyper.NY_VURDERING;
    private static final Behandlingstema BEHANDLING_TEMA = Behandlingstema.ARBEID_FLERE_LAND;
    private static final Behandlingsstatus BEHANDLING_STATUS = UNDER_BEHANDLING;
    private static final LocalDate BEHANDLING_FRIST = LocalDate.now().plusMonths(1);
    private final Behandlingsresultat BEHANDLINGSRESULTAT = new Behandlingsresultat();
    private static final String SAKSNUMMER = "12";
    private static final List<Long> PERIODE_IDS = Arrays.asList(2L, 3L);

    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepo;
    @Mock
    private BehandlingsgrunnlagRepository behandlingsgrunnlagRepo;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    private final FakeUnleash fakeUnleash = new FakeUnleash();
    private final LovligeKombinasjoner lovligeKombinasjoner = new LovligeKombinasjoner();
    private BehandlingService behandlingService;
    @Captor
    private ArgumentCaptor<Behandling> behandlingCaptor;
    @Captor
    private ArgumentCaptor<BehandlingEvent> behandlingEventCaptor;
    @Captor
    private ArgumentCaptor<BehandlingEndretAvSaksbehandlerEvent> behandlingEndretAvSaksbehandlerEventArgumentCaptor;
    @Captor
    private ArgumentCaptor<BehandlingEndretStatusEvent> behandlingEndretStatusEventCaptor;

    private Behandling behandling;

    @BeforeEach
    public void setUp() {
        behandlingService = new BehandlingService(behandlingRepository, tidligereMedlemsperiodeRepo, behandlingsgrunnlagRepo, behandlingsresultatService, oppgaveService, applicationEventPublisher, fakeUnleash);

        behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
    }

    @Test
    void hentBehandling() {
        when(behandlingRepository.findWithSaksopplysningerById(BEHANDLING_ID)).thenReturn(null);

        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLING_ID))
            .withMessage("Finner ikke behandling med id " + BEHANDLING_ID);
    }

    @Test
    void endreBehandling() {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);

        behandling.setTema(ARBEID_TJENESTEPERSON_ELLER_FLY);
        behandling.setType(HENVENDELSE);
        behandling.setFagsak(fagsak);
        behandling.setBehandlingsgrunnlag(opprettBehandlingsgrunnlag());

        when(behandlingRepository.findById(BEHANDLING_ID)).thenReturn(Optional.of(behandling));
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(BEHANDLINGSRESULTAT);

        behandlingService.endreBehandling(BEHANDLING_ID, BEHANDLING_TYPE, BEHANDLING_TEMA, BEHANDLING_STATUS, BEHANDLING_FRIST);

        verify(behandlingRepository, times(4)).save(behandlingCaptor.capture());
        verify(applicationEventPublisher, times(4)).publishEvent(behandlingEventCaptor.capture());

        var lagredeBehandlinger = behandlingCaptor.getAllValues();
        assertThat(lagredeBehandlinger.get(0).getId()).isEqualTo(BEHANDLING_ID);
        assertThat(lagredeBehandlinger.get(0).getStatus()).isEqualTo(BEHANDLING_STATUS);
        assertThat(lagredeBehandlinger.get(1).getId()).isEqualTo(BEHANDLING_ID);
        assertThat(lagredeBehandlinger.get(1).getType()).isEqualTo(BEHANDLING_TYPE);
        assertThat(lagredeBehandlinger.get(2).getId()).isEqualTo(BEHANDLING_ID);
        assertThat(lagredeBehandlinger.get(2).getBehandlingsfrist()).isEqualTo(BEHANDLING_FRIST);
        assertThat(lagredeBehandlinger.get(3).getId()).isEqualTo(BEHANDLING_ID);
        assertThat(lagredeBehandlinger.get(3).getTema()).isEqualTo(BEHANDLING_TEMA);

        var behandlingEndretEvents = behandlingEventCaptor.getAllValues();

        assertThat(behandlingEndretEvents.get(0).getBehandlingID()).isEqualTo(BEHANDLING_ID);
        assertThat(((BehandlingEndretStatusEvent) behandlingEndretEvents.get(0)).getBehandlingsstatus()).isEqualTo(BEHANDLING_STATUS);
        assertThat(behandlingEndretEvents.get(1).getBehandlingID()).isEqualTo(BEHANDLING_ID);
        assertThat(((BehandlingEndretAvSaksbehandlerEvent) behandlingEndretEvents.get(1)).getBehandlingstema()).isEqualTo(BEHANDLING_TEMA);
        assertThat(behandlingEndretEvents.get(2).getBehandlingID()).isEqualTo(BEHANDLING_ID);
        assertThat(((BehandlingEndretAvSaksbehandlerEvent) behandlingEndretEvents.get(2)).getBehandlingstype()).isEqualTo(BEHANDLING_TYPE);
        assertThat(behandlingEndretEvents.get(3).getBehandlingID()).isEqualTo(BEHANDLING_ID);
        assertThat(((BehandlingEndretAvSaksbehandlerEvent) behandlingEndretEvents.get(3)).getBehandlingsfrist()).isEqualTo(BEHANDLING_FRIST);
    }

    @Test
    void endreBehandling_med_ikke_godtatt_behandlingstema() {
        Fagsak fagsak = new Fagsak();
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);

        Behandlingstema initiellBehandlingsstema = REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING;
        Behandlingstyper initiellBehandlingstype = SOEKNAD;

        behandling.setTema(initiellBehandlingsstema);
        behandling.setType(initiellBehandlingstype);
        behandling.setFagsak(fagsak);
        behandling.setBehandlingsgrunnlag(opprettBehandlingsgrunnlag());

        when(behandlingRepository.findById(BEHANDLING_ID)).thenReturn(Optional.of(behandling));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> behandlingService.endreBehandling(BEHANDLING_ID, BEHANDLING_TYPE, BEHANDLING_TEMA, BEHANDLING_STATUS, BEHANDLING_FRIST));
    }

    @Test
    void endreBehandling_nullEllerSammeVerdi_ingenEndring() {
        behandling.setTema(BEHANDLING_TEMA);
        behandling.setType(BEHANDLING_TYPE);
        behandling.setStatus(BEHANDLING_STATUS);
        behandling.setBehandlingsfrist(BEHANDLING_FRIST);
        behandling.setBehandlingsgrunnlag(opprettBehandlingsgrunnlag());
        when(behandlingRepository.findById(BEHANDLING_ID)).thenReturn(Optional.of(behandling));

        behandlingService.endreBehandling(BEHANDLING_ID, BEHANDLING_TYPE, null, null, BEHANDLING_FRIST);

        verify(behandlingRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void endreBehandlingstema_gyldigEndringForSøknad_behandlingLagresBehandlingsresultatTømmesOgOppgaveOppdateres() {
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(new BehandlingsgrunnlagData());
        behandling.setTema(ARBEID_FLERE_LAND);
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);

        behandlingService.endreTema(behandling, UTSENDT_ARBEIDSTAKER);

        verify(behandlingRepository).save(behandlingCaptor.capture());
        verify(behandlingsresultatService).tømBehandlingsresultat(BEHANDLING_ID);
        verify(applicationEventPublisher).publishEvent(behandlingEndretAvSaksbehandlerEventArgumentCaptor.capture());
        assertThat(behandlingCaptor.getValue().getTema()).isEqualTo(UTSENDT_ARBEIDSTAKER);
        assertThat(behandlingCaptor.getValue().getId()).isEqualTo(BEHANDLING_ID);
        assertThat(behandlingEndretAvSaksbehandlerEventArgumentCaptor.getValue().getBehandlingstema()).isEqualTo(UTSENDT_ARBEIDSTAKER);
    }

    @Test
    void endreBehandlingstema_gyldigEndringForSED_behandlingLagresBehandlingsresultatTømmesOgOppgaveOppdateres() {
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(new BehandlingsgrunnlagData());
        behandling.setTema(TRYGDETID);
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);

        behandlingService.endreTema(behandling, ØVRIGE_SED_MED);

        verify(behandlingRepository).save(behandlingCaptor.capture());
        verify(behandlingsresultatService).tømBehandlingsresultat(BEHANDLING_ID);
        verify(applicationEventPublisher).publishEvent(behandlingEndretAvSaksbehandlerEventArgumentCaptor.capture());
        assertThat(behandlingCaptor.getValue().getTema()).isEqualTo(ØVRIGE_SED_MED);
        assertThat(behandlingCaptor.getValue().getId()).isEqualTo(BEHANDLING_ID);
        assertThat(behandlingEndretAvSaksbehandlerEventArgumentCaptor.getValue().getBehandlingstema()).isEqualTo(ØVRIGE_SED_MED);
    }

    @Test
    void endreBehandlingstema_ugyldigNyttTemaForSøknad_exceptionKastes() {
        behandling.setTema(ARBEID_FLERE_LAND);
        when(behandlingRepository.findById(BEHANDLING_ID)).thenReturn(Optional.of(behandling));
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(BEHANDLINGSRESULTAT);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> behandlingService.endreBehandlingstemaTilBehandling(BEHANDLING_ID, ØVRIGE_SED_MED))
            .withMessage("Ikke mulig å endre behandlingstema");
        verify(behandlingRepository, never()).save(any(Behandling.class));
        verify(behandlingsresultatService, never()).tømBehandlingsresultat(BEHANDLING_ID);
        verify(oppgaveService, never()).oppdaterOppgave(any(), any());
    }

    @Test
    void oppdaterStatus_statusAvventDok_dokumentasjonSvarfristOppdatert() {
        var fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        var behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        behandling.setFagsak(fagsak);
        behandling.setId(BEHANDLING_ID);
        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));

        behandlingService.endreStatus(BEHANDLING_ID, Behandlingsstatus.AVVENT_DOK_PART);

        verify(applicationEventPublisher).publishEvent(behandlingEndretStatusEventCaptor.capture());
        assertThat(behandling.getDokumentasjonSvarfristDato()).isNotNull();
        verify(oppgaveService).oppdaterOppgaveMedSaksnummer(eq(SAKSNUMMER), any());
        BehandlingEndretStatusEvent behandlingEndretStatusEvent = behandlingEndretStatusEventCaptor.getValue();
        assertThat(behandlingEndretStatusEvent.getBehandlingID()).isEqualTo(BEHANDLING_ID);
        assertThat(behandlingEndretStatusEvent.getBehandlingsstatus()).isEqualTo(AVVENT_DOK_PART);

    }

    @Test
    void oppdaterStatus_statusAnmodningUnntakSendt_behandlingLagret() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("23132");
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        behandling.setFagsak(fagsak);
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));
        behandlingService.endreStatus(BEHANDLING_ID, Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        verify(behandlingRepository).save(behandling);
        verify(applicationEventPublisher).publishEvent(behandlingEndretStatusEventCaptor.capture());

        BehandlingEndretStatusEvent behandlingEndretStatusEvent = behandlingEndretStatusEventCaptor.getValue();
        assertThat(behandlingEndretStatusEvent.getBehandlingID()).isEqualTo(BEHANDLING_ID);
        assertThat(behandlingEndretStatusEvent.getBehandlingsstatus()).isEqualTo(ANMODNING_UNNTAK_SENDT);
    }

    @Test
    void oppdaterStatus_behIkkeFunnet() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> behandlingService.endreStatus(BEHANDLING_ID, Behandlingsstatus.AVVENT_DOK_PART));
    }

    @Test
    void oppdaterStatus_ugyldig() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> behandlingService.endreStatus(BEHANDLING_ID, Behandlingsstatus.AVSLUTTET))
            .withMessage("Finner ikke behandling med id " + BEHANDLING_ID);
    }

    @Test
    void oppdaterStatus_statusAvsluttet_ferdigstillOppgave() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("23132");
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        behandling.setFagsak(fagsak);
        behandling.setId(BEHANDLING_ID);
        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));
        behandlingService.endreStatus(BEHANDLING_ID, Behandlingsstatus.AVSLUTTET);
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(fagsak.getSaksnummer());
    }

    @Test
    void oppdaterStatus_statusErAlleredeVurderDokument_ingentingSkjer() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);

        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));
        behandlingService.endreStatus(BEHANDLING_ID, Behandlingsstatus.VURDER_DOKUMENT);
        verify(behandlingRepository, never()).save(any());
    }

    @Test
    void knyttMedlemsperioder_ingenBehandling() {
        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> behandlingService.knyttMedlemsperioder(BEHANDLING_ID, PERIODE_IDS))
            .withMessage("Finner ikke behandling med id " + BEHANDLING_ID);
    }

    @Test
    void knyttMedlemsperioder_avsluttetBehandling() {
        Behandlingsstatus behandlingsstatus = Behandlingsstatus.AVSLUTTET;
        Behandling behandling = new Behandling();
        behandling.setStatus(behandlingsstatus);
        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> behandlingService.knyttMedlemsperioder(BEHANDLING_ID, PERIODE_IDS))
            .withMessage("Medlemsperioder kan ikke lagres på behandling med status " + behandlingsstatus);
    }

    @Test
    void knyttMedlemsperioder() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        when(behandlingRepository.findById(anyLong())).thenReturn(Optional.of(behandling));

        behandlingService.knyttMedlemsperioder(BEHANDLING_ID, PERIODE_IDS);
        verify(tidligereMedlemsperiodeRepo).deleteById_BehandlingId(BEHANDLING_ID);
        verify(tidligereMedlemsperiodeRepo).saveAll(anyList());
    }

    @Test
    void finnMedlemsperioder_ingenTidligereMedlemsperioder() {
        when(tidligereMedlemsperiodeRepo.findById_BehandlingId(anyLong())).thenReturn(new ArrayList<>());

        List<Long> periodeIder = behandlingService.hentMedlemsperioder(BEHANDLING_ID);
        assertThat(periodeIder).isEmpty();
    }

    @Test
    void hentMedlemsperioder() {
        List<TidligereMedlemsperiode> tidligereMedlemsperioder = Arrays.asList(
            new TidligereMedlemsperiode(BEHANDLING_ID, 2L),
            new TidligereMedlemsperiode(BEHANDLING_ID, 3L));
        when(tidligereMedlemsperiodeRepo.findById_BehandlingId(anyLong())).thenReturn(tidligereMedlemsperioder);

        List<Long> periodeIder = behandlingService.hentMedlemsperioder(BEHANDLING_ID);
        assertThat(periodeIder).containsExactly(2L, 3L);
    }

    @Test
    void nyBehandling() {
        String initierendeJournalpostId = "234";
        String initierendeDokumentId = "221234";
        Behandling behandling = behandlingService.nyBehandling(new Fagsak(), Behandlingsstatus.OPPRETTET, SOEKNAD, Behandlingstema.UTSENDT_ARBEIDSTAKER, initierendeJournalpostId, initierendeDokumentId);
        verify(behandlingRepository).save(behandling);
        verify(behandlingsresultatService).lagreNyttBehandlingsresultat(behandling);
        assertThat(behandling.getType()).isEqualTo(SOEKNAD);
        assertThat(behandling.getStatus()).isEqualTo(Behandlingsstatus.OPPRETTET);
        assertThat(behandling.getInitierendeJournalpostId()).isEqualTo(initierendeJournalpostId);
        assertThat(behandling.getInitierendeDokumentId()).isEqualTo(initierendeDokumentId);
    }

    @Test
    void replikerBehandling_replikererObjekterOgCollections() throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Behandling tidligsteInaktiveBehandling = opprettBehandlingMedData();
        Behandling replikertBehandling = behandlingService.replikerBehandling(tidligsteInaktiveBehandling, ENDRET_PERIODE);

        assertThat(replikertBehandling.getId()).isNull();
        assertThat(replikertBehandling.getTema()).isEqualTo(tidligsteInaktiveBehandling.getTema());
        assertThat(replikertBehandling.getStatus()).isEqualTo(OPPRETTET);
        assertThat(replikertBehandling.getDokumentasjonSvarfristDato()).isEqualTo(tidligsteInaktiveBehandling.getDokumentasjonSvarfristDato());
        assertThat(replikertBehandling.getInitierendeJournalpostId()).isEqualTo(tidligsteInaktiveBehandling.getInitierendeJournalpostId());
        assertThat(replikertBehandling.getBehandlingsfrist()).isEqualTo(LocalDate.now().plusWeeks(4));

        assertThat(replikertBehandling.getSaksopplysninger()).hasSize(1);
        assertThat(replikertBehandling.getSaksopplysninger()).allMatch(saksopplysning -> saksopplysning.getId() == null);
        assertThat(replikertBehandling.getSaksopplysninger()).allMatch(saksopplysning -> saksopplysning.getBehandling().equals(replikertBehandling));
        assertThat(replikertBehandling.getSaksopplysninger()).allMatch(saksopplysning -> saksopplysning.getKilder().iterator().next().getMottattDokument().equals("dokxml"));
        assertThat(replikertBehandling.getSaksopplysninger()).allMatch(saksopplysning -> saksopplysning.getType().equals(SaksopplysningType.INNTK));
        assertThat(replikertBehandling.getSaksopplysninger()).allMatch(saksopplysning -> saksopplysning.getEndretDato().toString().equals("2020-02-11T09:37:30Z"));
        assertThat(replikertBehandling.getSaksopplysninger()).flatExtracting(Saksopplysning::getKilder).allMatch(saksopplysningKilde -> saksopplysningKilde.getId() == null);
    }

    @Test
    void replikerBehandling_utenBehandlingsgrunnlag_blirReplikert() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Behandling tidligsteInaktiveBehandling = opprettBehandlingMedData();
        tidligsteInaktiveBehandling.setBehandlingsgrunnlag(null);

        assertThat(behandlingService.replikerBehandling(tidligsteInaktiveBehandling, Behandlingstyper.NY_VURDERING))
            .extracting(Behandling::getBehandlingsgrunnlag).isNull();
    }

    @Test
    void replikerBehandlingMedNyttBehandlingsresultat_replikererOgLagrerNyttBehandlingsresultat() {
        Behandling tidligsteInaktiveBehandling = opprettBehandlingMedData();
        assertThat(tidligsteInaktiveBehandling.getBehandlingsgrunnlag()).isNotNull();

        Behandling replikertBehandling = behandlingService.replikerBehandlingMedNyttBehandlingsresultat(tidligsteInaktiveBehandling, NY_VURDERING);

        assertThat(replikertBehandling.getId()).isNull();
        assertThat(replikertBehandling.getTema()).isEqualTo(tidligsteInaktiveBehandling.getTema());
        assertThat(replikertBehandling.getStatus()).isEqualTo(OPPRETTET);
        assertThat(replikertBehandling.getDokumentasjonSvarfristDato()).isEqualTo(tidligsteInaktiveBehandling.getDokumentasjonSvarfristDato());
        assertThat(replikertBehandling.getInitierendeJournalpostId()).isEqualTo(tidligsteInaktiveBehandling.getInitierendeJournalpostId());
        assertThat(replikertBehandling.getBehandlingsfrist()).isEqualTo(LocalDate.now().plusWeeks(4));
        assertThat(replikertBehandling.getBehandlingsgrunnlag()).isNull();
        assertThat(replikertBehandling.getSaksopplysninger()).isEmpty();

        verify(behandlingRepository).save(replikertBehandling);
        verify(behandlingsresultatService).lagreNyttBehandlingsresultat(replikertBehandling);
    }

    @Test
    void avsluttBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        when(behandlingRepository.findById(BEHANDLING_ID)).thenReturn(Optional.of(behandling));

        behandlingService.avsluttBehandling(BEHANDLING_ID);

        verify(behandlingRepository).save(behandlingCaptor.capture());
        verify(applicationEventPublisher).publishEvent(behandlingEndretStatusEventCaptor.capture());
        Behandling lagretBehandling = behandlingCaptor.getValue();
        assertThat(lagretBehandling.getStatus()).isEqualTo(Behandlingsstatus.AVSLUTTET);
        BehandlingEndretStatusEvent behandlingEndretStatusEvent = behandlingEndretStatusEventCaptor.getValue();
        assertThat(behandlingEndretStatusEvent.getBehandlingID()).isEqualTo(BEHANDLING_ID);
        assertThat(behandlingEndretStatusEvent.getBehandlingsstatus()).isEqualTo(AVSLUTTET);
    }

    @Test
    void avsluttBehandling_kasterFunksjonellException_dersomBehandlingErAvsluttet() {
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        behandling.setStatus(AVSLUTTET);
        when(behandlingRepository.findById(BEHANDLING_ID)).thenReturn(Optional.of(behandling));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> behandlingService.avsluttBehandling(BEHANDLING_ID))
            .withMessageContaining("Behandling " + BEHANDLING_ID + " er allerede avsluttet!");
    }

    @Test
    void avsluttNyVurdering_avslutterBehandlingOgOppdatererBehandlingsresultattype() {
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        behandling.setType(Behandlingstyper.NY_VURDERING);
        when(behandlingRepository.findById(BEHANDLING_ID)).thenReturn(Optional.of(behandling));

        behandlingService.avsluttNyVurdering(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE_BORTFALT);

        verify(behandlingRepository).save(behandlingCaptor.capture());
        verify(applicationEventPublisher).publishEvent(behandlingEndretStatusEventCaptor.capture());
        Behandling lagretBehandling = behandlingCaptor.getValue();
        assertThat(lagretBehandling.getStatus()).isEqualTo(Behandlingsstatus.AVSLUTTET);
        BehandlingEndretStatusEvent behandlingEndretStatusEvent = behandlingEndretStatusEventCaptor.getValue();
        assertThat(behandlingEndretStatusEvent.getBehandlingID()).isEqualTo(BEHANDLING_ID);
        assertThat(behandlingEndretStatusEvent.getBehandlingsstatus()).isEqualTo(AVSLUTTET);
        verify(behandlingsresultatService).oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE_BORTFALT);
    }

    @Test
    void avsluttNyVurdering_oppdatererBehandlingsresultattype() {
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        behandling.setType(Behandlingstyper.NY_VURDERING);
        when(behandlingRepository.findById(BEHANDLING_ID)).thenReturn(Optional.of(behandling));

        behandlingService.avsluttNyVurdering(BEHANDLING_ID, Behandlingsresultattyper.FERDIGBEHANDLET);
    }

    @Test
    void endreStatus_setterSvarFristPåToUker_nårNyStatusErAnmodningUnntakSendt() {
        Behandling behandling = opprettBehandlingUnderBehandling();

        behandlingService.endreStatus(behandling, ANMODNING_UNNTAK_SENDT);

        verify(behandlingRepository).save(behandlingCaptor.capture());
        Behandling lagretBehandling = behandlingCaptor.getValue();

        assertThat(lagretBehandling.getDokumentasjonSvarfristDato()).isNotNull();
        Instant forventetInstant = Instant.now().plus(Period.ofWeeks(2));
        assertThat(lagretBehandling.getDokumentasjonSvarfristDato())
            .isBetween(forventetInstant.minusSeconds(60), forventetInstant.plusSeconds(60));
    }

    @Test
    void endreStatus_setterSvarFristPåToUker_nårNyStatusErAvventDokPart() {
        Behandling behandling = opprettBehandlingUnderBehandling();

        behandlingService.endreStatus(behandling, AVVENT_DOK_PART);

        verify(behandlingRepository).save(behandlingCaptor.capture());
        Behandling lagretBehandling = behandlingCaptor.getValue();

        assertThat(lagretBehandling.getDokumentasjonSvarfristDato()).isNotNull();
        Instant forventetInstant = Instant.now().plus(Period.ofWeeks(2));
        assertThat(lagretBehandling.getDokumentasjonSvarfristDato())
            .isBetween(forventetInstant.minusSeconds(60), forventetInstant.plusSeconds(60));
    }

    @Test
    void endreStatus_setterSvarFristPåToUker_nårNyStatusErAvventDokUtl() {
        Behandling behandling = opprettBehandlingUnderBehandling();

        behandlingService.endreStatus(behandling, AVVENT_DOK_UTL);

        verify(behandlingRepository).save(behandlingCaptor.capture());
        Behandling lagretBehandling = behandlingCaptor.getValue();

        assertThat(lagretBehandling.getDokumentasjonSvarfristDato()).isNotNull();
        Instant forventetInstant = Instant.now().plus(Period.ofWeeks(2));
        assertThat(lagretBehandling.getDokumentasjonSvarfristDato())
            .isBetween(forventetInstant.minusSeconds(60), forventetInstant.plusSeconds(60));
    }

    @Test
    void endreStatus_setterSvarFristTilNull_nårNyStatusErUnderBehandling() {
        Behandling behandling = new Behandling();
        behandling.setFagsak(new Fagsak());
        behandling.setId(BEHANDLING_ID);
        behandling.setStatus(AVVENT_DOK_UTL);

        behandlingService.endreStatus(behandling, UNDER_BEHANDLING);

        verify(behandlingRepository).save(behandlingCaptor.capture());
        Behandling lagretBehandling = behandlingCaptor.getValue();

        assertThat(lagretBehandling.getDokumentasjonSvarfristDato()).isNull();
    }

    @Test
    void settNyVurderingTilFerdigbehandlet_avslutterBehandlingOgSetterBehandlingsresultattypeRiktig() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        behandling.setType(Behandlingstyper.NY_VURDERING);
        behandling.setFagsak(fagsak);
        when(behandlingRepository.findById(BEHANDLING_ID)).thenReturn(Optional.of(behandling));

        behandlingService.settNyVurderingTilFerdigbehandlet(BEHANDLING_ID);

        verify(behandlingRepository).save(behandlingCaptor.capture());
        verify(applicationEventPublisher).publishEvent(behandlingEndretStatusEventCaptor.capture());
        Behandling lagretBehandling = behandlingCaptor.getValue();
        assertThat(lagretBehandling.getStatus()).isEqualTo(Behandlingsstatus.AVSLUTTET);
        BehandlingEndretStatusEvent behandlingEndretStatusEvent = behandlingEndretStatusEventCaptor.getValue();
        assertThat(behandlingEndretStatusEvent.getBehandlingID()).isEqualTo(BEHANDLING_ID);
        assertThat(behandlingEndretStatusEvent.getBehandlingsstatus()).isEqualTo(AVSLUTTET);
        verify(behandlingsresultatService).oppdaterBehandlingsresultattype(BEHANDLING_ID, Behandlingsresultattyper.FERDIGBEHANDLET);
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(SAKSNUMMER);
    }

    @Test
    void avsluttNyVurdering_kasterFunksjonellException_dersomBehandlingTypeIkkeErNyVurdering() {
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        behandling.setStatus(AVSLUTTET);
        when(behandlingRepository.findById(BEHANDLING_ID)).thenReturn(Optional.of(behandling));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> behandlingService.avsluttNyVurdering(BEHANDLING_ID, Behandlingsresultattyper.FERDIGBEHANDLET))
            .withMessageContaining("Behandling " + BEHANDLING_ID + " er ikke typen NY_VURDERING!");
    }

    @Test
    void avsluttNyVurdering_kasterFunksjonellException_dersomBehandlingErAvsluttet() {
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        behandling.setStatus(AVSLUTTET);
        behandling.setType(Behandlingstyper.NY_VURDERING);
        when(behandlingRepository.findById(BEHANDLING_ID)).thenReturn(Optional.of(behandling));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> behandlingService.avsluttNyVurdering(BEHANDLING_ID, Behandlingsresultattyper.HENLEGGELSE_BORTFALT))
            .withMessageContaining("Behandling " + BEHANDLING_ID + " er allerede avsluttet!");
    }

    @Test
    void endreBehandlingsstatusFraOpprettetTilUnderBehandling_harStatusOpprettet_statusBlirSattTilUnderBehandling() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.OPPRETTET);

        behandlingService.endreBehandlingsstatusFraOpprettetTilUnderBehandling(behandling);

        assertThat(behandling.getStatus()).isEqualTo(Behandlingsstatus.UNDER_BEHANDLING);
        verify(behandlingRepository).save(behandling);
    }

    @Test
    void endreBehandlingsstatusFraOpprettetTilUnderBehandling_harStatusAvventerSvar_ingenStatusendring() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.AVVENT_DOK_PART);

        behandlingService.endreBehandlingsstatusFraOpprettetTilUnderBehandling(behandling);

        verify(behandlingRepository, never()).save(any());
    }

    @Test
    void endreBehandlingsfrist_enUkeFrem_fristOppdateres() {
        LocalDate nå = LocalDate.now();
        Behandling behandling = new Behandling();
        behandling.setBehandlingsfrist(nå);
        when(behandlingRepository.findById(BEHANDLING_ID)).thenReturn(Optional.of(behandling));

        behandlingService.endreBehandlingsfrist(BEHANDLING_ID, nå.plusWeeks(1));

        assertThat(behandling.getBehandlingsfrist()).isEqualTo(nå.plusWeeks(1));
        verify(behandlingRepository).save(behandling);
        verify(applicationEventPublisher).publishEvent(any(BehandlingsfristEndretEvent.class));
    }

    private Behandling opprettBehandlingMedData() {
        Behandling behandling = opprettTomBehandlingMedId();
        behandling.setTema(Behandlingstema.BESLUTNING_LOVVALG_NORGE);
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setInitierendeJournalpostId("initierendeJournalpostId");
        behandling.setDokumentasjonSvarfristDato(Instant.parse("2017-12-11T09:37:30.00Z"));
        behandling.setSaksopplysninger(new LinkedHashSet<>());

        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getSaksopplysninger().add(opprettSaksopplysning());
        return behandling;
    }

    private Saksopplysning opprettSaksopplysning() {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setBehandling(opprettTomBehandlingMedId());
        saksopplysning.setKilder(opprettSaksopplysningkildeMedID());
        saksopplysning.setType(SaksopplysningType.INNTK);
        saksopplysning.setEndretDato(Instant.parse("2020-02-11T09:37:30Z"));
        return saksopplysning;
    }

    private Set<SaksopplysningKilde> opprettSaksopplysningkildeMedID() {
        var kilde = new SaksopplysningKilde();
        kilde.setId(123321L);
        kilde.setKilde(SaksopplysningKildesystem.EREG);
        kilde.setMottattDokument("dokxml");
        return Collections.singleton(kilde);
    }

    private Behandling opprettTomBehandlingMedId() {
        Behandling behandling = new Behandling();
        behandling.setId(665L);
        return behandling;
    }

    private Behandlingsgrunnlag opprettBehandlingsgrunnlag() {
        var behandlingsgrunnlag = new Behandlingsgrunnlag();
        var behandlingsgrunnlagdata = new BehandlingsgrunnlagData();
        behandlingsgrunnlagdata.soeknadsland.landkoder.add(Landkoder.SE.getKode());
        behandlingsgrunnlagdata.periode = new Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1));
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(behandlingsgrunnlagdata);
        return behandlingsgrunnlag;
    }

    private Behandling opprettBehandlingUnderBehandling() {
        Behandling behandling = new Behandling();
        behandling.setFagsak(new Fagsak());
        behandling.setId(BEHANDLING_ID);
        behandling.setStatus(UNDER_BEHANDLING);
        return behandling;
    }
}
