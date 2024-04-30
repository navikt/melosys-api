package no.nav.melosys.service.sak;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakTestFactory;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.Journalposttype;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.saksflytapi.ProsessinstansService;
import no.nav.melosys.service.felles.dto.SoeknadslandDto;
import no.nav.melosys.service.journalforing.JournalfoeringService;
import no.nav.melosys.service.journalforing.dto.PeriodeDto;
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerSaksbehandlingService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.jeasy.random.FieldPredicates.named;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpprettSakTest {
    private static final String JP_ID = "jpID";

    @Mock
    private JournalfoeringService journalfoeringService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private LovligeKombinasjonerSaksbehandlingService lovligeKombinasjonerSaksbehandlingService;
    @Mock
    private SaksbehandlingRegler saksbehandlingRegler;

    private static final EasyRandom random = new EasyRandom(getRandomConfig());

    @Captor
    private ArgumentCaptor<no.nav.melosys.saksflytapi.journalfoering.OpprettSakRequest> opprettSakRequestArgumentCaptor;


    private static EasyRandomParameters getRandomConfig() {
        return new EasyRandomParameters().collectionSizeRange(1, 4)
            .randomize(PeriodeDto.class, () -> new PeriodeDto(LocalDate.now(), LocalDate.now().plusDays(1)))
            .excludeField(named("behandlingsaarsakFritekst"))
            .stringLengthRange(2, 4);
    }

    private OpprettSak opprettSak;

    @BeforeEach
    public void setUp() {
        opprettSak = new OpprettSak(journalfoeringService, oppgaveService, prosessinstansService, saksbehandlingRegler, lovligeKombinasjonerSaksbehandlingService);
    }

    @Test
    void nySakOgBehandlingFraOppgave_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET);
        opprettSakDto.getSoknadDto().land.setFlereLandUkjentHvilke(false);

        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId("1234").build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);
        when(journalfoeringService.hentJournalpost("1234")).thenReturn(lagJournalpost(Journalposttype.INN, "skanning"));


        opprettSak.opprettNySakOgBehandlingFraOppgave(opprettSakDto);


        verify(prosessinstansService).opprettProsessinstansNySakEØS(oppgave.getJournalpostId(), opprettSakDto.tilOpprettSakRequest());
    }

    @Test
    void lagNySak_EU_EOS_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setHovedpart(Aktoersroller.BRUKER);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.HENVENDELSE);
        opprettSakDto.setSoknadDto(opprettSoknadDto());


        opprettSak.opprettNySakOgBehandling(opprettSakDto);


        verify(prosessinstansService).opprettNySakOgBehandling(opprettSakRequestArgumentCaptor.capture());
        assertThat(opprettSakRequestArgumentCaptor.getValue()).isEqualTo(opprettSakDto.tilOpprettSakRequest());
    }

    @Test
    void lagNySak_TRYGDEAVTALE_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setHovedpart(Aktoersroller.BRUKER);
        opprettSakDto.setSakstype(Sakstyper.TRYGDEAVTALE);
        opprettSakDto.setSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.HENVENDELSE);


        opprettSak.opprettNySakOgBehandling(opprettSakDto);


        verify(prosessinstansService).opprettNySakOgBehandling(opprettSakDto.tilOpprettSakRequest());
    }

    @Test
    void lagNySak_FTRL_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setHovedpart(Aktoersroller.BRUKER);
        opprettSakDto.setSakstype(Sakstyper.FTRL);
        opprettSakDto.setSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.HENVENDELSE);


        opprettSak.opprettNySakOgBehandling(opprettSakDto);


        verify(prosessinstansService).opprettNySakOgBehandling(opprettSakRequestArgumentCaptor.capture());
        assertThat(opprettSakRequestArgumentCaptor.getValue()).isEqualTo(opprettSakDto.tilOpprettSakRequest());
    }

    @Test
    void lagNySak_mottaksdatoMangler_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setHovedpart(Aktoersroller.BRUKER);
        opprettSakDto.setSakstype(Sakstyper.FTRL);
        opprettSakDto.setSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.HENVENDELSE);
        opprettSakDto.setMottaksdato(null);


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettSak.opprettNySakOgBehandling(opprettSakDto))
            .withMessageContaining("Mottaksdato");
    }

    @Test
    void lagNySak_årsakFritekstMedFeilType_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setBehandlingsaarsakFritekst("Fritekst");
        opprettSakDto.setBehandlingsaarsakType(Behandlingsaarsaktyper.SØKNAD);


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettSak.opprettNySakOgBehandling(opprettSakDto))
            .withMessageContaining("Kan ikke lagre fritekst som årsak når årsakstype");
    }

    @Test
    void nySakOgBehandlingFraOppgave_sakstypeFtrl_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.FTRL);
        opprettSakDto.setBehandlingstema(Behandlingstema.YRKESAKTIV);
        opprettSakDto.setBehandlingstype(Behandlingstyper.FØRSTEGANG);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId("1234").build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);
        when(journalfoeringService.hentJournalpost("1234")).thenReturn(lagJournalpost(Journalposttype.INN, "skanning"));


        opprettSak.opprettNySakOgBehandlingFraOppgave(opprettSakDto);


        verify(prosessinstansService).opprettProsessinstansNySakFTRLTrygdeavtale(oppgave.getJournalpostId(), opprettSakDto.tilOpprettSakRequest());
    }

    @Test
    void nySakOgBehandlingFraOppgave_sakstypeTrygdeavtale_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.TRYGDEAVTALE);
        opprettSakDto.setBehandlingstema(Behandlingstema.YRKESAKTIV);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId("1234").build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);
        when(journalfoeringService.hentJournalpost("1234")).thenReturn(lagJournalpost(Journalposttype.INN, "skanning"));


        opprettSak.opprettNySakOgBehandlingFraOppgave(opprettSakDto);


        verify(prosessinstansService).opprettProsessinstansNySakFTRLTrygdeavtale(oppgave.getJournalpostId(), opprettSakDto.tilOpprettSakRequest());
    }

    @Test
    void nySakOgBehandlingFraOppgave_oppgaveIdMangler_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.getSoknadDto().land = lagSoeknadslandDto(true);
        opprettSakDto.setOppgaveID("");

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettSak.opprettNySakOgBehandlingFraOppgave(opprettSakDto))
            .withMessageContaining("OppgaveID mangler.");
    }

    @Test
    void nySakOgBehandlingFraOppgave_utenJournalpostID_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId(null).build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettSak.opprettNySakOgBehandlingFraOppgave(opprettSakDto))
            .withMessageContaining("mangler journalpost");
    }

    @Test
    void nySakOgBehandlingFraOppgave_journalpostUtgående_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId(JP_ID).build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);
        when(journalfoeringService.hentJournalpost(JP_ID)).thenReturn(lagJournalpost(Journalposttype.UT, "NAV"));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettSak.opprettNySakOgBehandlingFraOppgave(opprettSakDto))
            .withMessageContaining("utgående journalposter");
    }

    @Test
    void nySakOgBehandlingFraOppgave_journalpostFraSedErKnyttetTilEksisterendeSak_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId(JP_ID).build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);
        final Journalpost journalpost = lagJournalpost(Journalposttype.INN, "EESSI");
        when(journalfoeringService.hentJournalpost(JP_ID)).thenReturn(journalpost);
        when(journalfoeringService.finnSakTilknyttetSedJournalpost(journalpost)).thenReturn(Optional.of(FagsakTestFactory.lagFagsak()));
        when(saksbehandlingRegler.harIngenFlyt(any(), any(), any(), any())).thenReturn(true);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettSak.opprettNySakOgBehandlingFraOppgave(opprettSakDto))
            .withMessageContaining("SED-en som er tilknyttet Gosys-oppgaven du har valgt er allerede koblet til ");
    }

    @Test
    void nySakOgBehandlingFraOppgave_journalpostFraSedErIkkeKnyttetTilEksisterendeSak_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET);
        opprettSakDto.setSoknadDto(opprettSoknadDto());
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId(JP_ID).build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);
        final Journalpost journalpost = lagJournalpost(Journalposttype.INN, "EESSI");
        when(journalfoeringService.hentJournalpost(JP_ID)).thenReturn(journalpost);
        when(journalfoeringService.finnSakTilknyttetSedJournalpost(journalpost)).thenReturn(Optional.empty());

        opprettSak.opprettNySakOgBehandlingFraOppgave(opprettSakDto);


        verify(prosessinstansService).opprettProsessinstansNySakEØS(oppgave.getJournalpostId(), opprettSakDto.tilOpprettSakRequest());
    }

    @Test
    void validerOpprettSakDto_søknadUtenLandOgPeriodeIngenFlyt_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.HENVENDELSE);
        opprettSakDto.getSoknadDto().land.setFlereLandUkjentHvilke(false);
        opprettSakDto.getSoknadDto().land.getLandkoder().clear();
        opprettSakDto.getSoknadDto().periode = null;
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId(JP_ID).build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);
        final Journalpost journalpost = lagJournalpost(Journalposttype.INN, "EESSI");
        when(journalfoeringService.hentJournalpost(JP_ID)).thenReturn(journalpost);
        when(journalfoeringService.finnSakTilknyttetSedJournalpost(journalpost)).thenReturn(Optional.empty());
        when(saksbehandlingRegler.harIngenFlyt(any(), any(), any(), any())).thenReturn(true);


        opprettSak.opprettNySakOgBehandlingFraOppgave(opprettSakDto);


        verify(prosessinstansService).opprettProsessinstansNySakEØS(oppgave.getJournalpostId(), opprettSakDto.tilOpprettSakRequest());
    }

    private Journalpost lagJournalpost(Journalposttype journalposttype, String mottakskanal) {
        final Journalpost journalpost = new Journalpost(JP_ID);
        journalpost.setJournalposttype(journalposttype);
        journalpost.setMottaksKanal(mottakskanal);
        journalpost.setForsendelseMottatt(Instant.EPOCH);
        return journalpost;
    }

    @Test
    void nySakOgBehandlingFraOppgave_oppgaveTypeUgyldig_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.getSoknadDto().land = lagSoeknadslandDto(false);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.JFR).setJournalpostId("33").build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettSak.opprettNySakOgBehandlingFraOppgave(opprettSakDto))
            .withMessageContaining("kan ikke opprettes på bakgrunn av oppgave med type");
    }

    private SoeknadslandDto lagSoeknadslandDto(boolean flereLandUkjentHvilke) {
        List<String> landkoder = flereLandUkjentHvilke ? Collections.emptyList() : Collections.singletonList("DK");
        return new SoeknadslandDto(landkoder, flereLandUkjentHvilke);
    }

    @Test
    void validerOpprettSakDto_manglerBehandlingstema_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(null);

        doThrow(new FunksjonellException("Behandlingstema")).when(lovligeKombinasjonerSaksbehandlingService).validerOpprettelseOgEndring(any(), any(), any(), any(), any());

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettSak.opprettNySakOgBehandlingFraOppgave(opprettSakDto))
            .withMessageContaining("Behandlingstema");
    }

    @Test
    void validerOpprettSakDto_nullSøknad_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.FØRSTEGANG);
        opprettSakDto.setSoknadDto(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettSak.opprettNySakOgBehandlingFraOppgave(opprettSakDto))
            .withMessageContaining("må ikke være null");
    }

    @Test
    void validerOpprettSakDto_nullSøknad_okForFTRL() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.FTRL);
        opprettSakDto.setBehandlingstema(Behandlingstema.YRKESAKTIV);
        opprettSakDto.setBehandlingstype(Behandlingstyper.FØRSTEGANG);
        opprettSakDto.setSoknadDto(null);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId("1234").build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);
        when(journalfoeringService.hentJournalpost("1234")).thenReturn(lagJournalpost(Journalposttype.INN, "skanning"));


        opprettSak.opprettNySakOgBehandlingFraOppgave(opprettSakDto);


        verify(prosessinstansService).opprettProsessinstansNySakFTRLTrygdeavtale(oppgave.getJournalpostId(), opprettSakDto.tilOpprettSakRequest());
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


        opprettSak.opprettNySakOgBehandlingFraOppgave(opprettSakDto);


        verify(prosessinstansService).opprettProsessinstansNySakFTRLTrygdeavtale(oppgave.getJournalpostId(), opprettSakDto.tilOpprettSakRequest());
    }

    @Test
    void validerOpprettSakDto_søknadUtenFom_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.FØRSTEGANG);
        opprettSakDto.getSoknadDto().periode.setFom(null);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettSak.opprettNySakOgBehandlingFraOppgave(opprettSakDto))
            .withMessageContaining("fra og med dato");
    }

    @Test
    void validerOpprettSakDto_søknadUtenLand_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.FØRSTEGANG);
        opprettSakDto.getSoknadDto().land.setFlereLandUkjentHvilke(false);
        opprettSakDto.getSoknadDto().land.getLandkoder().clear();

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettSak.opprettNySakOgBehandlingFraOppgave(opprettSakDto))
            .withMessageContaining("land");
    }

    @Test
    void validerOpprettSakDto_søknadMedLandOgAlleLand_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        opprettSakDto.setBehandlingstype(Behandlingstyper.FØRSTEGANG);
        opprettSakDto.getSoknadDto().land.setFlereLandUkjentHvilke(true);
        opprettSakDto.getSoknadDto().land.setLandkoder(Collections.singletonList("DK"));

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettSak.opprettNySakOgBehandlingFraOppgave(opprettSakDto))
            .withMessageContaining("land");
    }

    private SøknadDto opprettSoknadDto() {
        var søknadDto = new SøknadDto();
        søknadDto.periode = new PeriodeDto(LocalDate.now().minusMonths(4), LocalDate.now().minusMonths(3));
        søknadDto.land = SoeknadslandDto.av(Landkoder.DE);
        return søknadDto;
    }
}
