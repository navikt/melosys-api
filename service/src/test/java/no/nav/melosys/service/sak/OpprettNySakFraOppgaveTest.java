package no.nav.melosys.service.sak;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.Journalposttype;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.felles.dto.SoeknadslandDto;
import no.nav.melosys.service.journalforing.JournalfoeringService;
import no.nav.melosys.service.journalforing.dto.PeriodeDto;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpprettNySakFraOppgaveTest {
    private static final String JP_ID = "jpID";

    @Mock
    private JournalfoeringService journalfoeringService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private FagsakService fagsakService;

    private final FakeUnleash unleash = new FakeUnleash();

    private static final EasyRandom random = new EasyRandom(getRandomConfig());

    private static EasyRandomParameters getRandomConfig() {
        return new EasyRandomParameters().collectionSizeRange(1, 4)
            .randomize(PeriodeDto.class, () -> new PeriodeDto(LocalDate.now(), LocalDate.now().plusDays(1)))
            .stringLengthRange(2, 4);
    }

    private OpprettNySakFraOppgave opprettNySakFraOppgave;

    @BeforeEach
    public void setUp() {
        opprettNySakFraOppgave = new OpprettNySakFraOppgave(journalfoeringService, oppgaveService, prosessinstansService, unleash, fagsakService);
        unleash.enableAll();
    }

    @Test
    void bestillNySakOgBehandling_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.ØVRIGE_SED_MED);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId("1234").build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);
        when(journalfoeringService.hentJournalpost("1234")).thenReturn(lagJournalpost(Journalposttype.INN, "skanning"));

        opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto);

        verify(prosessinstansService).opprettProsessinstansNySakEØS(oppgave.getJournalpostId(), opprettSakDto, Behandlingstyper.SED);
    }

    @Test
    void lagNySak_EU_EOS_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setHovedpart(Aktoersroller.BRUKER);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.HENVENDELSE);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId("1234").build();

        opprettNySakFraOppgave.lagNySak(opprettSakDto);

        verify(prosessinstansService).lagNySak(opprettSakDto);
    }

    @Test
    void lagNySak_TRYGDEAVTALE_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setHovedpart(Aktoersroller.BRUKER);
        opprettSakDto.setSakstype(Sakstyper.TRYGDEAVTALE);
        opprettSakDto.setSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.HENVENDELSE);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId("1234").build();

        opprettNySakFraOppgave.lagNySak(opprettSakDto);

        verify(prosessinstansService).lagNySak(opprettSakDto);
    }

    @Test
    void lagNySak_FTRL_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setHovedpart(Aktoersroller.BRUKER);
        opprettSakDto.setSakstype(Sakstyper.FTRL);
        opprettSakDto.setSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.HENVENDELSE);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId("1234").build();

        opprettNySakFraOppgave.lagNySak(opprettSakDto);

        verify(prosessinstansService).lagNySak(opprettSakDto);
    }

    @Test
    void lagNySakForBehandling_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setHovedpart(Aktoersroller.BRUKER);
        opprettSakDto.setSakstype(Sakstyper.FTRL);
        opprettSakDto.setSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.HENVENDELSE);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId("1234").build();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-1");

        when(fagsakService.hentFagsak(fagsak.getSaksnummer())).thenReturn(fagsak);

        opprettNySakFraOppgave.lagNyBehandlingForSak(fagsak.getSaksnummer(), opprettSakDto);

        verify(prosessinstansService).lagNyBehandlingForSak(fagsak.getSaksnummer(), opprettSakDto);
    }

    @Test
    void lagNySakForBehandling_oppretterProsess_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setHovedpart(Aktoersroller.BRUKER);
        opprettSakDto.setSakstype(Sakstyper.FTRL);
        opprettSakDto.setSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.HENVENDELSE);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId("1234").build();
        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();

        fagsak.setSaksnummer("MEL-1");
        fagsak.setBehandlinger(Arrays.asList(behandling));

        when(fagsakService.hentFagsak(fagsak.getSaksnummer())).thenReturn(fagsak);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettNySakFraOppgave.lagNyBehandlingForSak(fagsak.getSaksnummer(), opprettSakDto))
            .withMessageContaining("Det finnes allerede en aktiv behandling på fagsak");
    }

    @Test
    void bestillNySakOgBehandling_sakstypeFtrl_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.FTRL);
        opprettSakDto.setBehandlingstema(Behandlingstema.ARBEID_I_UTLANDET);
        opprettSakDto.setBehandlingstype(Behandlingstyper.FØRSTEGANG);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId("1234").build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);
        when(journalfoeringService.hentJournalpost("1234")).thenReturn(lagJournalpost(Journalposttype.INN, "skanning"));

        opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto);

        verify(prosessinstansService).opprettProsessinstansNySakFTRLTrygdeavtale(oppgave.getJournalpostId(), opprettSakDto);
    }

    @Test
    void bestillNySakOgBehandling_sakstypeTrygdeavtaleFeatureToggleEnabled_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.TRYGDEAVTALE);
        opprettSakDto.setBehandlingstema(Behandlingstema.YRKESAKTIV);
        unleash.enableAll();
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId("1234").build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);
        when(journalfoeringService.hentJournalpost("1234")).thenReturn(lagJournalpost(Journalposttype.INN, "skanning"));

        opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto);

        verify(prosessinstansService).opprettProsessinstansNySakFTRLTrygdeavtale(oppgave.getJournalpostId(), opprettSakDto);
    }

    @Test
    void bestillNySakOgBehandling_sakstypeFtrlFeatureToggleDisabled_kasterException() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.FTRL);
        opprettSakDto.setBehandlingstema(Behandlingstema.ARBEID_I_UTLANDET);
        unleash.disableAll();

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto))
            .withMessageContaining("Kan ikke opprette ny sak med");
    }

    @Test
    void bestillNySakOgBehandling_oppgaveIdMangler_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.getSoknadDto().setLand(lagSoeknadslandDto(true));
        opprettSakDto.setOppgaveID("");

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto))
            .withMessageContaining("OppgaveID mangler.");
    }

    @Test
    void bestillNySakOgBehandling_utenJournalpostID_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId(null).build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto))
            .withMessageContaining("mangler journalpost");
    }

    @Test
    void bestillNySakOgBehandling_journalpostUtgående_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.ØVRIGE_SED_MED);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId(JP_ID).build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);
        when(journalfoeringService.hentJournalpost(JP_ID)).thenReturn(lagJournalpost(Journalposttype.UT, "NAV"));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto))
            .withMessageContaining("utgående journalposter");
    }

    @Test
    void bestillNySakOgBehandling_journalpostFraSedErKnyttetTilEksisterendeSak_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.ØVRIGE_SED_MED);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId(JP_ID).build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);
        final Journalpost journalpost = lagJournalpost(Journalposttype.INN, "EESSI");
        when(journalfoeringService.hentJournalpost(JP_ID)).thenReturn(journalpost);
        when(journalfoeringService.finnSakTilknyttetSedJournalpost(journalpost)).thenReturn(Optional.of(new Fagsak()));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto))
            .withMessageContaining("SED-en som er tilknyttet Gosys-oppgaven du har valgt er allerede koblet til ");
    }

    @Test
    void bestillNySakOgBehandling_journalpostFraSedErIkkeKnyttetTilEksisterendeSak_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.ØVRIGE_SED_MED);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId(JP_ID).build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);
        final Journalpost journalpost = lagJournalpost(Journalposttype.INN, "EESSI");
        when(journalfoeringService.hentJournalpost(JP_ID)).thenReturn(journalpost);
        when(journalfoeringService.finnSakTilknyttetSedJournalpost(journalpost)).thenReturn(Optional.empty());

        opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto);

        verify(prosessinstansService).opprettProsessinstansNySakEØS(oppgave.getJournalpostId(), opprettSakDto, Behandlingstyper.SED);
    }

    @Test
    void validerOpprettSakDto_søknadUtenLandOgPeriodeTomFlyt_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.HENVENDELSE);
        opprettSakDto.getSoknadDto().getLand().setErUkjenteEllerAlleEosLand(false);
        opprettSakDto.getSoknadDto().getLand().getLandkoder().clear();
        opprettSakDto.getSoknadDto().setPeriode(null);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId(JP_ID).build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);
        final Journalpost journalpost = lagJournalpost(Journalposttype.INN, "EESSI");
        when(journalfoeringService.hentJournalpost(JP_ID)).thenReturn(journalpost);
        when(journalfoeringService.finnSakTilknyttetSedJournalpost(journalpost)).thenReturn(Optional.empty());

        opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto);

        verify(prosessinstansService).opprettProsessinstansNySakEØS(oppgave.getJournalpostId(), opprettSakDto, Behandlingstyper.SOEKNAD);
    }

    private Journalpost lagJournalpost(Journalposttype journalposttype, String mottakskanal) {
        final Journalpost journalpost = new Journalpost(JP_ID);
        journalpost.setJournalposttype(journalposttype);
        journalpost.setMottaksKanal(mottakskanal);
        return journalpost;
    }

    @Test
    void bestillNySakOgBehandling_oppgaveTypeUgyldig_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.getSoknadDto().setLand(lagSoeknadslandDto(false));
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.JFR).setJournalpostId("33").build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto))
            .withMessageContaining("kan ikke opprettes på bakgrunn av oppgave med type");
    }

    private SoeknadslandDto lagSoeknadslandDto(boolean erUkjenteEllerAlleEosLand) {
        List<String> landkoder = erUkjenteEllerAlleEosLand ? Collections.emptyList() : Collections.singletonList("DK");
        return new SoeknadslandDto(landkoder, erUkjenteEllerAlleEosLand);
    }

    @Test
    void validerOpprettSakDto_manglerBehandlingstema_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(null);

        doThrow(new FunksjonellException("Behandlingstema")).when(journalfoeringService).validerBehandlingstema(any(), any(), any(), any(), any());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto))
            .withMessageContaining("Behandlingstema");
    }

    @Test
    void lagNySak_validerOpprettSakDto_manglerBehandlingstema_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(null);

        doThrow(new FunksjonellException("Behandlingstema")).when(journalfoeringService).validerBehandlingstema(any(), any(), any(), any(), any());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettNySakFraOppgave.lagNySak(opprettSakDto))
            .withMessageContaining("Behandlingstema");
    }

    @Test
    void lagNySak_validerOpprettSakDto_manglerBehandlingstype_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstype(null);

        doThrow(new FunksjonellException("Behandlingstype")).when(journalfoeringService).validerBehandlingstype(any(), any(), any(), any(), any(), any());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettNySakFraOppgave.lagNySak(opprettSakDto))
            .withMessageContaining("Behandlingstype");
    }

    @Test
    void lagNyBehandlingForSak_validerOpprettSakDto_manglerBehandlingstema_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(null);
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-1");

        when(fagsakService.hentFagsak(fagsak.getSaksnummer())).thenReturn(fagsak);

        doThrow(new FunksjonellException("Behandlingstema")).when(journalfoeringService).validerBehandlingstema(any(), any(), any(), any(), any());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettNySakFraOppgave.lagNyBehandlingForSak(fagsak.getSaksnummer(), opprettSakDto))
            .withMessageContaining("Behandlingstema");
    }

    @Test
    void lagNyBehandlingForSak_validerOpprettSakDto_manglerBehandlingstype_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstype(null);
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-1");

        when(fagsakService.hentFagsak(fagsak.getSaksnummer())).thenReturn(fagsak);

        doThrow(new FunksjonellException("Behandlingstype")).when(journalfoeringService).validerBehandlingstype(any(), any(), any(), any(), any(), any());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettNySakFraOppgave.lagNyBehandlingForSak(fagsak.getSaksnummer(), opprettSakDto))
            .withMessageContaining("Behandlingstype");
    }

    @Test
    void validerOpprettSakDto_nullSøknad_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setSoknadDto(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto))
            .withMessageContaining("må ikke være null");
    }

    @Test
    void validerOpprettSakDto_nullSøknad_okForFTRL() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.FTRL);
        opprettSakDto.setBehandlingstema(Behandlingstema.ARBEID_I_UTLANDET);
        opprettSakDto.setBehandlingstype(Behandlingstyper.FØRSTEGANG);
        opprettSakDto.setSoknadDto(null);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId("1234").build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);
        when(journalfoeringService.hentJournalpost("1234")).thenReturn(lagJournalpost(Journalposttype.INN, "skanning"));

        opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto);

        verify(prosessinstansService).opprettProsessinstansNySakFTRLTrygdeavtale(oppgave.getJournalpostId(), opprettSakDto);
    }

    @Test
    void validerOpprettSakDto_nullSøknad_okForTrygdeavtale() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.TRYGDEAVTALE);
        opprettSakDto.setBehandlingstema(Behandlingstema.YRKESAKTIV);
        opprettSakDto.setSoknadDto(null);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId("1234").build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);
        when(journalfoeringService.hentJournalpost("1234")).thenReturn(lagJournalpost(Journalposttype.INN, "skanning"));

        opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto);

        verify(prosessinstansService).opprettProsessinstansNySakFTRLTrygdeavtale(oppgave.getJournalpostId(), opprettSakDto);
    }

    @Test
    void validerOpprettSakDto_søknadUtenFom_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.FØRSTEGANG);
        opprettSakDto.getSoknadDto().getPeriode().setFom(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto))
            .withMessageContaining("fra og med dato");
    }

    @Test
    void validerOpprettSakDto_søknadUtenLand_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.FØRSTEGANG);
        opprettSakDto.getSoknadDto().getLand().setErUkjenteEllerAlleEosLand(false);
        opprettSakDto.getSoknadDto().getLand().getLandkoder().clear();

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto))
            .withMessageContaining("land");
    }

    @Test
    void validerOpprettSakDto_søknadMedLandOgAlleLand_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.getSoknadDto().getLand().setErUkjenteEllerAlleEosLand(true);
        opprettSakDto.getSoknadDto().getLand().setLandkoder(Collections.singletonList("DK"));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto))
            .withMessageContaining("land");
    }
}
