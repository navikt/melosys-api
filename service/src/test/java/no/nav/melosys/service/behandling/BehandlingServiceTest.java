package no.nav.melosys.service.behandling;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BehandlingServiceTest {

    private static final String SAKSBEHANDLER = "Z990007";
    private static final long BEHANDLING_ID = 11L;
    private static final List<Long> PERIODE_IDS = Arrays.asList(2L, 3L);

    @Mock
    private BehandlingRepository behandlingRepo;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Mock
    private TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepo;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private BehandlingService behandlingService;

    @Captor
    private ArgumentCaptor<Behandling> behandlingCaptor;

    @BeforeEach
    public void setUp() {
        behandlingService = new BehandlingService(behandlingRepo, behandlingsresultatRepository, tidligereMedlemsperiodeRepo, behandlingsresultatService, oppgaveService, applicationEventPublisher);
    }

    @Test
    public void hentBehandling() throws FunksjonellException {
        when(behandlingRepo.findWithSaksopplysningerById(eq(BEHANDLING_ID))).thenReturn(null);

        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> behandlingService.hentBehandling(BEHANDLING_ID))
            .withMessage("Finner ikke behandling med id " + BEHANDLING_ID);
    }

    @Test
    public void oppdaterStatus_statusAvventDok_dokumentasjonSvarfristOppdatert() throws FunksjonellException, TekniskException {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));
        behandlingService.oppdaterStatus(BEHANDLING_ID, Behandlingsstatus.AVVENT_DOK_PART);
        assertThat(behandling.getDokumentasjonSvarfristDato()).isNotNull();
    }

    @Test
    public void oppdaterStatus_statusAnmodningUnntakSendt_behandlingLagret() throws FunksjonellException, TekniskException {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));
        behandlingService.oppdaterStatus(BEHANDLING_ID, Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        verify(behandlingRepo).save(behandling);
    }

    @Test
    public void oppdaterStatus_behIkkeFunnet() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.empty());

        assertThatExceptionOfType(IkkeFunnetException.class)
            .isThrownBy(() -> behandlingService.oppdaterStatus(BEHANDLING_ID, Behandlingsstatus.AVVENT_DOK_PART));
    }

    @Test
    public void oppdaterStatus_ugyldig() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> behandlingService.oppdaterStatus(BEHANDLING_ID, Behandlingsstatus.AVSLUTTET))
            .withMessage("Finner ikke behandling med id " + BEHANDLING_ID);
    }

    @Test
    public void oppdaterStatus_statusAvsluttet_ferdigstillOppgave() throws FunksjonellException, TekniskException {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("23132");
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        behandling.setFagsak(fagsak);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));
        behandlingService.oppdaterStatus(BEHANDLING_ID, Behandlingsstatus.AVSLUTTET);
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(eq(fagsak.getSaksnummer()));
    }

    @Test
    public void oppdaterStatus_statusErAlleredeVurderDokument_ingentingSkjer() throws FunksjonellException, TekniskException {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);

        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));
        behandlingService.oppdaterStatus(BEHANDLING_ID, Behandlingsstatus.VURDER_DOKUMENT);
        verify(behandlingRepo, never()).save(any());
    }

    @Test
    public void brukerOppdaterStatus_nyStatusErIkkeGyldig() {
        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.ARBEID_I_UTLANDET);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> behandlingService.brukerOppdaterStatus(BEHANDLING_ID, AVSLUTTET))
            .withMessage("Behandlingen kan ikke endres til status AVSLUTTET. Gyldige statuser er [AVVENT_DOK_PART, AVVENT_DOK_UTL, UNDER_BEHANDLING]");
    }

    @Test
    public void hentMuligeStatuser_temaOvrigeSedMed_avsluttetErMulig() throws FunksjonellException {
        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.ØVRIGE_SED_MED);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));

        Collection<Behandlingsstatus> muligeStatuser = behandlingService.hentMuligeStatuser(BEHANDLING_ID);
        assertThat(muligeStatuser).containsExactly(AVVENT_DOK_PART, AVVENT_DOK_UTL, UNDER_BEHANDLING, AVSLUTTET);
    }

    @Test
    public void hentMuligeStatuser_temaArbeidUtland_avsluttetErIkkeMulig() throws FunksjonellException {
        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.ARBEID_I_UTLANDET);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));

        Collection<Behandlingsstatus> muligeStatuser = behandlingService.hentMuligeStatuser(BEHANDLING_ID);
        assertThat(muligeStatuser).containsExactly(AVVENT_DOK_PART, AVVENT_DOK_UTL, UNDER_BEHANDLING);
    }

    @Test
    public void knyttMedlemsperioder_ingenBehandling() {
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.empty());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> behandlingService.knyttMedlemsperioder(BEHANDLING_ID, PERIODE_IDS))
            .withMessage("Finner ikke behandling med id " + BEHANDLING_ID);
    }

    @Test
    public void knyttMedlemsperioder_avsluttetBehandling() {
        Behandlingsstatus behandlingsstatus = Behandlingsstatus.AVSLUTTET;
        Behandling behandling = new Behandling();
        behandling.setStatus(behandlingsstatus);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> behandlingService.knyttMedlemsperioder(BEHANDLING_ID, PERIODE_IDS))
            .withMessage("Medlemsperioder kan ikke lagres på behandling med status " + behandlingsstatus);
    }

    @Test
    public void knyttMedlemsperioder() throws FunksjonellException {
        Behandlingsstatus behandlingsstatus = Behandlingsstatus.UNDER_BEHANDLING;
        Behandling behandling = new Behandling();
        behandling.setStatus(behandlingsstatus);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));

        behandlingService.knyttMedlemsperioder(BEHANDLING_ID, PERIODE_IDS);
        verify(tidligereMedlemsperiodeRepo).deleteById_BehandlingId(BEHANDLING_ID);
        verify(tidligereMedlemsperiodeRepo).saveAll(anyList());
    }

    @Test
    public void finnMedlemsperioder_ingenTidligereMedlemsperioder() {
        when(tidligereMedlemsperiodeRepo.findById_BehandlingId(anyLong())).thenReturn(new ArrayList<>());

        List<Long> periodeIder = behandlingService.hentMedlemsperioder(BEHANDLING_ID);
        assertThat(periodeIder).isEmpty();
    }

    @Test
    public void hentMedlemsperioder() {
        List<TidligereMedlemsperiode> tidligereMedlemsperioder = Arrays.asList(
            new TidligereMedlemsperiode(BEHANDLING_ID, 2L),
            new TidligereMedlemsperiode(BEHANDLING_ID, 3L));
        when(tidligereMedlemsperiodeRepo.findById_BehandlingId(anyLong())).thenReturn(tidligereMedlemsperioder);

        List<Long> periodeIder = behandlingService.hentMedlemsperioder(BEHANDLING_ID);
        assertThat(periodeIder).containsExactly(2L, 3L);
    }

    @Test
    public void nyBehandling() {
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
    public void replikerBehandling_replikererObjekterOgCollections() throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Behandling tidligsteInaktiveBehandling = opprettBehandlingMedData();
        Behandling replikertBehandling = behandlingService.replikerBehandling(tidligsteInaktiveBehandling, Behandlingsstatus.OPPRETTET, Behandlingstyper.ENDRET_PERIODE);

        assertThat(replikertBehandling.getId()).isNull();
        assertThat(replikertBehandling.getStatus()).isEqualTo(Behandlingsstatus.OPPRETTET);
        assertThat(replikertBehandling.getDokumentasjonSvarfristDato()).isEqualTo(tidligsteInaktiveBehandling.getDokumentasjonSvarfristDato());
        assertThat(replikertBehandling.getInitierendeJournalpostId()).isEqualTo(tidligsteInaktiveBehandling.getInitierendeJournalpostId());

        assertThat(replikertBehandling.getSaksopplysninger().size()).isEqualTo(1);
        assertThat(replikertBehandling.getSaksopplysninger()).allMatch(saksopplysning -> saksopplysning.getId() == null);
        assertThat(replikertBehandling.getSaksopplysninger()).allMatch(saksopplysning -> saksopplysning.getBehandling().equals(replikertBehandling));
        assertThat(replikertBehandling.getSaksopplysninger()).allMatch(saksopplysning -> saksopplysning.getKilder().iterator().next().getMottattDokument().equals("dokxml"));
        assertThat(replikertBehandling.getSaksopplysninger()).allMatch(saksopplysning -> saksopplysning.getType().equals(SaksopplysningType.INNTK));
        assertThat(replikertBehandling.getSaksopplysninger()).allMatch(saksopplysning -> saksopplysning.getEndretDato().toString().equals("2020-02-11T09:37:30Z"));
        assertThat(replikertBehandling.getSaksopplysninger()).flatExtracting(Saksopplysning::getKilder).allMatch(saksopplysningKilde -> saksopplysningKilde.getId() == null);
    }

    @Test
    public void avsluttBehandling() throws Exception {
        Behandling behandling = new Behandling();
        when(behandlingRepo.findById(eq(BEHANDLING_ID))).thenReturn(Optional.of(behandling));

        behandlingService.avsluttBehandling(BEHANDLING_ID);

        verify(behandlingRepo).save(behandlingCaptor.capture());
        Behandling lagretBehandling = behandlingCaptor.getValue();
        assertThat(lagretBehandling.getStatus()).isEqualTo(Behandlingsstatus.AVSLUTTET);
    }

    @Test
    public void endreBehandlingsstatusFraOpprettetTilUnderBehandling_harStatusOpprettet_statusBlirSattTilUnderBehandling() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.OPPRETTET);

        behandlingService.endreBehandlingsstatusFraOpprettetTilUnderBehandling(behandling);

        assertThat(behandling.getStatus()).isEqualTo(Behandlingsstatus.UNDER_BEHANDLING);
        verify(behandlingRepo).save(behandling);
    }

    @Test
    public void endreBehandlingsstatusFraOpprettetTilUnderBehandling_harStatusAvventerSvar_ingenStatusendring() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.AVVENT_DOK_PART);

        behandlingService.endreBehandlingsstatusFraOpprettetTilUnderBehandling(behandling);

        verify(behandlingRepo, never()).save(any());
    }

    @Test
    public final void testErBehandlingRedigerbarOgTilordnetSaksbehandler() throws FunksjonellException, TekniskException {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("12345678901");
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder().setTilordnetRessurs(SAKSBEHANDLER);

        when(oppgaveService.finnOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer())).thenReturn(Optional.of(oppgaveBuilder.build()));

        behandling.setStatus(Behandlingsstatus.OPPRETTET);
        assertThat(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, SAKSBEHANDLER)).isTrue();

        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        assertThat(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, SAKSBEHANDLER)).isTrue();

        behandling.setStatus(Behandlingsstatus.IVERKSETTER_VEDTAK);
        assertThat(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, SAKSBEHANDLER)).isFalse();

        behandling.setStatus(Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        assertThat(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, SAKSBEHANDLER)).isFalse();

        behandling.setStatus(Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.IKKE_YRKESAKTIV);
        assertThat(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, SAKSBEHANDLER)).isTrue();
        behandling.setType(null);

        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        assertThat(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, SAKSBEHANDLER)).isFalse();

        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        oppgaveBuilder.setTilordnetRessurs("noen andre");
        Oppgave oppgave2 = oppgaveBuilder.build();
        when(oppgaveService.finnOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer())).thenReturn(Optional.ofNullable(oppgave2));
        assertThat(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, SAKSBEHANDLER)).isFalse();

        oppgaveBuilder.setTilordnetRessurs(null);
        Oppgave oppgave3 = oppgaveBuilder.build();
        when(oppgaveService.finnOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer())).thenReturn(Optional.ofNullable(oppgave3));
        assertThat(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, SAKSBEHANDLER)).isFalse();
    }

    @Test
    public final void testErBehandlingRedigerbarOgTilordnetSaksbehandler_ingenOppgaveFunnet_kasterException() throws FunksjonellException, TekniskException {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("12345678901");
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setStatus(Behandlingsstatus.OPPRETTET);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        when(oppgaveService.finnOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer()))
            .thenThrow(new TekniskException("Finner ingen oppgave for fagsak"));

        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, SAKSBEHANDLER))
            .withMessage("Finner ingen oppgave for fagsak");
    }

    private Behandling opprettBehandlingMedData() {
        Behandling behandling = opprettTomBehandlingMedId();
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
}