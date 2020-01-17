package no.nav.melosys.service;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BehandlingServiceTest {
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

    private BehandlingService behandlingService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Captor
    private ArgumentCaptor<Behandling> behandlingCaptor;

    private static final String SAKSBEHANDLER = "Z990007";

    @Before
    public void setUp() {
        behandlingService = new BehandlingService(behandlingRepo, behandlingsresultatRepository, tidligereMedlemsperiodeRepo, behandlingsresultatService, oppgaveService);
    }

    @Test
    public void hentBehandling() throws FunksjonellException {
        long behandlingID = 11L;
        when(behandlingRepo.findWithSaksopplysningerById(eq(behandlingID))).thenReturn(null);

        expectedException.expect(IkkeFunnetException.class);
        expectedException.expectMessage("Finner ikke behandling med id " + behandlingID);

        behandlingService.hentBehandling(behandlingID);
    }

    @Test
    public void oppdaterStatus_statusAvventDok_dokumentasjonSvarfristOppdatert() throws FunksjonellException, TekniskException {
        long behandlingID = 11L;
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));
        behandlingService.oppdaterStatus(behandlingID, Behandlingsstatus.AVVENT_DOK_PART);
        assertThat(behandling.getDokumentasjonSvarfristDato()).isNotNull();
    }

    @Test
    public void oppdaterStatus_statusAnmodningUnntakSendt_behandlingLagret() throws FunksjonellException, TekniskException {
        long behandlingID = 11L;
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));
        behandlingService.oppdaterStatus(behandlingID, Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        verify(behandlingRepo).save(behandling);
    }

    @Test(expected = IkkeFunnetException.class)
    public void oppdaterStatus_behIkkeFunnet() throws FunksjonellException, TekniskException {
        long behandlingID = 11L;
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.empty());
        behandlingService.oppdaterStatus(behandlingID, Behandlingsstatus.AVVENT_DOK_PART);
    }

    @Test(expected = FunksjonellException.class)
    public void oppdaterStatus_ugyldig() throws FunksjonellException, TekniskException {
        long behandlingID = 11L;
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        behandlingService.oppdaterStatus(behandlingID, Behandlingsstatus.AVSLUTTET);
    }

    @Test
    public void oppdaterStatus_statusAvsluttet_ferdigstillOppgave() throws FunksjonellException, TekniskException {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("23132");
        long behandlingID = 11L;
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        behandling.setFagsak(fagsak);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));
        behandlingService.oppdaterStatus(behandlingID, Behandlingsstatus.AVSLUTTET);
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(eq(fagsak.getSaksnummer()));
    }

    @Test
    public void knyttMedlemsperioder_ingenBehandling() throws FunksjonellException {
        long behandlingID = 11L;
        List<Long> periodeIder = Arrays.asList(2L, 3L);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.empty());

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Behandling " + behandlingID + " finnes ikke.");

        behandlingService.knyttMedlemsperioder(behandlingID, periodeIder);
    }

    @Test
    public void knyttMedlemsperioder_avsluttetBehandling() throws FunksjonellException {
        long behandlingID = 11L;
        Behandlingsstatus behandlingsstatus = Behandlingsstatus.AVSLUTTET;
        Behandling behandling = new Behandling();
        behandling.setStatus(behandlingsstatus);
        List<Long> periodeIder = Arrays.asList(2L, 3L);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));

        expectedException.expect(FunksjonellException.class);
        expectedException.expectMessage("Medlemsperioder kan ikke lagres på behandling med status " + behandlingsstatus);

        behandlingService.knyttMedlemsperioder(behandlingID, periodeIder);
    }

    @Test
    public void knyttMedlemsperioder() throws FunksjonellException {
        long behandlingID = 11L;
        Behandlingsstatus behandlingsstatus = Behandlingsstatus.UNDER_BEHANDLING;
        Behandling behandling = new Behandling();
        behandling.setStatus(behandlingsstatus);
        List<Long> periodeIder = Arrays.asList(2L, 3L);
        when(behandlingRepo.findById(anyLong())).thenReturn(Optional.of(behandling));

        behandlingService.knyttMedlemsperioder(behandlingID, periodeIder);
        verify(tidligereMedlemsperiodeRepo, times(1)).deleteById_BehandlingId(behandlingID);
        verify(tidligereMedlemsperiodeRepo).saveAll(anyList());
    }

    @Test
    public void finnMedlemsperioder_ingenTidligereMedlemsperioder() {
        long behandlingID = 11L;
        when(tidligereMedlemsperiodeRepo.findById_BehandlingId(anyLong())).thenReturn(new ArrayList<>());

        List<Long> periodeIder = behandlingService.hentMedlemsperioder(behandlingID);
        assertThat(periodeIder).isNotNull();
        assertThat(periodeIder).isEmpty();
    }

    @Test
    public void hentMedlemsperioder() {
        long behandlingID = 11L;
        List<TidligereMedlemsperiode> tidligereMedlemsperioder = Arrays.asList(
            new TidligereMedlemsperiode(behandlingID, 2L),
            new TidligereMedlemsperiode(behandlingID, 3L));
        when(tidligereMedlemsperiodeRepo.findById_BehandlingId(anyLong())).thenReturn(tidligereMedlemsperioder);

        List<Long> periodeIder = behandlingService.hentMedlemsperioder(behandlingID);
        assertThat(periodeIder).isNotNull();
        assertThat(periodeIder).containsExactly(2L, 3L);
    }

    @Test
    public void nyBehandling() {
        String initierendeJournalpostId = "234";
        String initierendeDokumentId = "221234";
        Behandling behandling = behandlingService.nyBehandling(new Fagsak(), Behandlingsstatus.OPPRETTET, Behandlingstyper.SOEKNAD, initierendeJournalpostId, initierendeDokumentId);
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
        assertThat(replikertBehandling.getSaksopplysninger()).allMatch(saksopplysning -> saksopplysning.getDokumentXml().equals("dokxml"));
        assertThat(replikertBehandling.getSaksopplysninger()).allMatch(saksopplysning -> saksopplysning.getType().equals(SaksopplysningType.INNTK));
        assertThat(replikertBehandling.getSaksopplysninger()).allMatch(saksopplysning -> saksopplysning.getEndretDato().toString().equals("2020-02-11T09:37:30Z"));
    }

    @Test
    public void avsluttBehandling() throws Exception {
        long behandlingID = 1L;
        Behandling behandling = new Behandling();
        when(behandlingRepo.findById(eq(behandlingID))).thenReturn(Optional.of(behandling));

        behandlingService.avsluttBehandling(behandlingID);

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
        assertThat(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, SAKSBEHANDLER)).isEqualTo(true);

        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        assertThat(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, SAKSBEHANDLER)).isEqualTo(true);

        behandling.setStatus(Behandlingsstatus.IVERKSETTER_VEDTAK);
        assertThat(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, SAKSBEHANDLER)).isEqualTo(false);

        behandling.setStatus(Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        assertThat(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, SAKSBEHANDLER)).isEqualTo(false);

        behandling.setStatus(Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        behandling.setType(Behandlingstyper.SOEKNAD_IKKE_YRKESAKTIV);
        assertThat(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, SAKSBEHANDLER)).isEqualTo(true);
        behandling.setType(null);

        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        assertThat(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, SAKSBEHANDLER)).isEqualTo(false);

        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        oppgaveBuilder.setTilordnetRessurs("noen andre");
        Oppgave oppgave2 = oppgaveBuilder.build();
        when(oppgaveService.finnOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer())).thenReturn(Optional.ofNullable(oppgave2));
        assertThat(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, SAKSBEHANDLER)).isEqualTo(false);

        oppgaveBuilder.setTilordnetRessurs(null);
        Oppgave oppgave3 = oppgaveBuilder.build();
        when(oppgaveService.finnOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer())).thenReturn(Optional.ofNullable(oppgave3));
        assertThat(behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, SAKSBEHANDLER)).isEqualTo(false);
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

        expectedException.expect(TekniskException.class);
        behandlingService.erBehandlingRedigerbarOgTilordnetSaksbehandler(behandling, SAKSBEHANDLER);
    }

    private Behandling opprettBehandlingMedData() {
        Behandling behandling = opprettTomBehandlingMedId();
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setInitierendeJournalpostId("initierendeJournalpostId");
        behandling.setDokumentasjonSvarfristDato(Instant.parse("2017-12-11T09:37:30.00Z"));
        behandling.setSaksopplysninger(new LinkedHashSet<>());

        behandling.getSaksopplysninger().add(opprettSaksopplysning("dokxml", SaksopplysningType.INNTK, "2020-02-11T09:37:30Z"));
        return behandling;
    }

    private Saksopplysning opprettSaksopplysning(String dokxml, SaksopplysningType saksopplysningType, String endretDato) {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setBehandling(opprettTomBehandlingMedId());
        saksopplysning.setDokumentXml(dokxml);
        saksopplysning.setType(saksopplysningType);
        saksopplysning.setEndretDato(Instant.parse(endretDato));
        return saksopplysning;
    }

    private Behandling opprettTomBehandlingMedId() {
        Behandling behandling = new Behandling();
        behandling.setId(665L);
        return behandling;
    }
}