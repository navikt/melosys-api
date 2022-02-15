package no.nav.melosys.service.behandling;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData;
import no.nav.melosys.domain.behandlingsgrunnlag.data.Periode;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsgrunnlagRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository;
import no.nav.melosys.service.oppgave.OppgaveService;
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
    private Behandlingsresultat BEHANDLINGSRESULTAT = new Behandlingsresultat();
    private static final String SAKSNUMMER = "12";
    private static final List<Long> PERIODE_IDS = Arrays.asList(2L, 3L);

    @Mock
    private BehandlingRepository behandlingRepo;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
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
        behandlingService = new BehandlingService(behandlingRepo, behandlingsresultatRepository, tidligereMedlemsperiodeRepo, behandlingsgrunnlagRepo, behandlingsresultatService, oppgaveService, applicationEventPublisher);

        behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
    }

    @Test
    void hentBehandling() {
        when(behandlingRepo.findWithSaksopplysningerById(BEHANDLING_ID)).thenReturn(null);

        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> behandlingService.hentBehandling(BEHANDLING_ID))
            .withMessage("Finner ikke behandling med id " + BEHANDLING_ID);
    }

    @Test
    void endreBehandling() {
        behandling.setTema(Behandlingstema.IKKE_YRKESAKTIV);
        behandling.setType(Behandlingstyper.ENDRET_PERIODE);
        behandling.setFagsak(new Fagsak());
        behandling.setBehandlingsgrunnlag(opprettBehandlingsgrunnlag());
        when(behandlingRepo.findById(BEHANDLING_ID)).thenReturn(Optional.of(behandling));
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(BEHANDLINGSRESULTAT);

        behandlingService.endreBehandling(BEHANDLING_ID, Sakstyper.EU_EOS, BEHANDLING_TYPE, BEHANDLING_TEMA, BEHANDLING_STATUS, BEHANDLING_FRIST);

        verify(behandlingRepo, times(4)).save(behandlingCaptor.capture());
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
        assertThat(((BehandlingEndretAvSaksbehandlerEvent) behandlingEndretEvents.get(1)).getBehandlingstype()).isEqualTo(BEHANDLING_TYPE);
        assertThat(behandlingEndretEvents.get(2).getBehandlingID()).isEqualTo(BEHANDLING_ID);
        assertThat(((BehandlingEndretAvSaksbehandlerEvent) behandlingEndretEvents.get(2)).getBehandlingstema()).isEqualTo(BEHANDLING_TEMA);
        assertThat(behandlingEndretEvents.get(3).getBehandlingID()).isEqualTo(BEHANDLING_ID);
        assertThat(((BehandlingEndretAvSaksbehandlerEvent) behandlingEndretEvents.get(3)).getBehandlingsfrist()).isEqualTo(BEHANDLING_FRIST);

    }

    @Test
    void endreBehandlingstema_gyldigEndringForSøknad_behandlingLagresBehandlingsresultatTømmesOgOppgaveOppdateres() {
        Behandlingsgrunnlag behandlingsgrunnlag = new Behandlingsgrunnlag();
        behandlingsgrunnlag.setBehandlingsgrunnlagdata(new BehandlingsgrunnlagData());
        behandling.setTema(ARBEID_FLERE_LAND);
        behandling.setBehandlingsgrunnlag(behandlingsgrunnlag);

        behandlingService.endreTema(behandling, UTSENDT_ARBEIDSTAKER);

        verify(behandlingRepo).save(behandlingCaptor.capture());
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

        verify(behandlingRepo).save(behandlingCaptor.capture());
        verify(behandlingsresultatService).tømBehandlingsresultat(BEHANDLING_ID);
        verify(applicationEventPublisher).publishEvent(behandlingEndretAvSaksbehandlerEventArgumentCaptor.capture());
        assertThat(behandlingCaptor.getValue().getTema()).isEqualTo(ØVRIGE_SED_MED);
        assertThat(behandlingCaptor.getValue().getId()).isEqualTo(BEHANDLING_ID);
        assertThat(behandlingEndretAvSaksbehandlerEventArgumentCaptor.getValue().getBehandlingstema()).isEqualTo(ØVRIGE_SED_MED);
    }

    @Test
    void endreBehandlingstema_ugyldigNyttTemaForSøknad_exceptionKastes() {
        behandling.setTema(ARBEID_FLERE_LAND);
        when(behandlingRepo.findById(BEHANDLING_ID)).thenReturn(Optional.of(behandling));
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID)).thenReturn(BEHANDLINGSRESULTAT);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> behandlingService.endreBehandlingstemaTilBehandling(BEHANDLING_ID, ØVRIGE_SED_MED))
            .withMessage("Ikke mulig å endre behandlingstema");
        verify(behandlingRepo, never()).save(any(Behandling.class));
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
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));

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
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));
        behandlingService.endreStatus(BEHANDLING_ID, Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        verify(behandlingRepo).save(behandling);
        verify(applicationEventPublisher).publishEvent(behandlingEndretStatusEventCaptor.capture());

        BehandlingEndretStatusEvent behandlingEndretStatusEvent = behandlingEndretStatusEventCaptor.getValue();
        assertThat(behandlingEndretStatusEvent.getBehandlingID()).isEqualTo(BEHANDLING_ID);
        assertThat(behandlingEndretStatusEvent.getBehandlingsstatus()).isEqualTo(ANMODNING_UNNTAK_SENDT);
    }

    @Test
    void oppdaterStatus_behIkkeFunnet() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.empty());

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
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));
        behandlingService.endreStatus(BEHANDLING_ID, Behandlingsstatus.AVSLUTTET);
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(fagsak.getSaksnummer());
    }

    @Test
    void oppdaterStatus_statusErAlleredeVurderDokument_ingentingSkjer() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);

        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));
        behandlingService.endreStatus(BEHANDLING_ID, Behandlingsstatus.VURDER_DOKUMENT);
        verify(behandlingRepo, never()).save(any());
    }

    @Test
    void knyttMedlemsperioder_ingenBehandling() {
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.empty());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> behandlingService.knyttMedlemsperioder(BEHANDLING_ID, PERIODE_IDS))
            .withMessage("Finner ikke behandling med id " + BEHANDLING_ID);
    }

    @Test
    void knyttMedlemsperioder_avsluttetBehandling() {
        Behandlingsstatus behandlingsstatus = Behandlingsstatus.AVSLUTTET;
        Behandling behandling = new Behandling();
        behandling.setStatus(behandlingsstatus);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> behandlingService.knyttMedlemsperioder(BEHANDLING_ID, PERIODE_IDS))
            .withMessage("Medlemsperioder kan ikke lagres på behandling med status " + behandlingsstatus);
    }

    @Test
    void knyttMedlemsperioder() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));

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
        Behandling behandling = behandlingService.nyBehandling(new Fagsak(), Behandlingsstatus.OPPRETTET, Behandlingstyper.SOEKNAD, Behandlingstema.UTSENDT_ARBEIDSTAKER, initierendeJournalpostId, initierendeDokumentId);
        verify(behandlingRepo).save(any(Behandling.class));
        assertThat(behandling.getType()).isEqualTo(Behandlingstyper.SOEKNAD);
        assertThat(behandling.getStatus()).isEqualTo(Behandlingsstatus.OPPRETTET);
        assertThat(behandling.getInitierendeJournalpostId()).isEqualTo(initierendeJournalpostId);
        assertThat(behandling.getInitierendeDokumentId()).isEqualTo(initierendeDokumentId);
    }

    @Test
    void replikerBehandling_replikererObjekterOgCollections() throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Behandling tidligsteInaktiveBehandling = opprettBehandlingMedData();
        Behandling replikertBehandling = behandlingService.replikerBehandling(tidligsteInaktiveBehandling, OPPRETTET, Behandlingstyper.ENDRET_PERIODE);

        assertThat(replikertBehandling.getId()).isNull();
        assertThat(replikertBehandling.getStatus()).isEqualTo(OPPRETTET);
        assertThat(replikertBehandling.getDokumentasjonSvarfristDato()).isEqualTo(tidligsteInaktiveBehandling.getDokumentasjonSvarfristDato());
        assertThat(replikertBehandling.getInitierendeJournalpostId()).isEqualTo(tidligsteInaktiveBehandling.getInitierendeJournalpostId());
        assertThat(replikertBehandling.getBehandlingsfrist()).isEqualTo(LocalDate.now().plusWeeks(4));

        assertThat(replikertBehandling.getSaksopplysninger().size()).isEqualTo(1);
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

        assertThat(behandlingService.replikerBehandling(tidligsteInaktiveBehandling, OPPRETTET, Behandlingstyper.NY_VURDERING))
            .extracting(Behandling::getBehandlingsgrunnlag).isNull();

    }

    @Test
    void avsluttBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLING_ID);
        when(behandlingRepo.findById(BEHANDLING_ID)).thenReturn(Optional.of(behandling));

        behandlingService.avsluttBehandling(BEHANDLING_ID);

        verify(behandlingRepo).save(behandlingCaptor.capture());
        verify(applicationEventPublisher).publishEvent(behandlingEndretStatusEventCaptor.capture());
        Behandling lagretBehandling = behandlingCaptor.getValue();
        assertThat(lagretBehandling.getStatus()).isEqualTo(Behandlingsstatus.AVSLUTTET);
        BehandlingEndretStatusEvent behandlingEndretStatusEvent = behandlingEndretStatusEventCaptor.getValue();
        assertThat(behandlingEndretStatusEvent.getBehandlingID()).isEqualTo(BEHANDLING_ID);
        assertThat(behandlingEndretStatusEvent.getBehandlingsstatus()).isEqualTo(AVSLUTTET);
    }

    @Test
    void endreBehandlingsstatusFraOpprettetTilUnderBehandling_harStatusOpprettet_statusBlirSattTilUnderBehandling() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.OPPRETTET);

        behandlingService.endreBehandlingsstatusFraOpprettetTilUnderBehandling(behandling);

        assertThat(behandling.getStatus()).isEqualTo(Behandlingsstatus.UNDER_BEHANDLING);
        verify(behandlingRepo).save(behandling);
    }

    @Test
    void endreBehandlingsstatusFraOpprettetTilUnderBehandling_harStatusAvventerSvar_ingenStatusendring() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.AVVENT_DOK_PART);

        behandlingService.endreBehandlingsstatusFraOpprettetTilUnderBehandling(behandling);

        verify(behandlingRepo, never()).save(any());
    }

    @Test
    void endreBehandlingsfrist_enUkeFrem_fristOppdateres() {
        LocalDate nå = LocalDate.now();
        Behandling behandling = new Behandling();
        behandling.setBehandlingsfrist(nå);
        when(behandlingRepo.findById(BEHANDLING_ID)).thenReturn(Optional.of(behandling));

        behandlingService.endreBehandlingsfrist(BEHANDLING_ID, nå.plusWeeks(1));

        assertThat(behandling.getBehandlingsfrist()).isEqualTo(nå.plusWeeks(1));
        verify(behandlingRepo).save(behandling);
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
        behandling.getSaksopplysninger().add(opprettSaksopplysning("dokxml", SaksopplysningType.INNTK, "2020-02-11T09:37:30Z"));
        return behandling;
    }

    private Saksopplysning opprettSaksopplysning(String dokxml, SaksopplysningType saksopplysningType, String endretDato) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setBehandling(opprettTomBehandlingMedId());
        saksopplysning.setKilder(opprettSaksopplysningkildeMedID(dokxml));
        saksopplysning.setType(saksopplysningType);
        saksopplysning.setEndretDato(Instant.parse(endretDato));
        return saksopplysning;
    }

    private Set<SaksopplysningKilde> opprettSaksopplysningkildeMedID(String dokxml) {
        var kilde = new SaksopplysningKilde();
        kilde.setId(123321L);
        kilde.setKilde(SaksopplysningKildesystem.EREG);
        kilde.setMottattDokument(dokxml);
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
}
