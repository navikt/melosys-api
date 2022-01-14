package no.nav.melosys.service.sak;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.Journalposttype;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.felles.dto.SoeknadslandDto;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpprettNySakFraOppgaveTest {
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private ProsessinstansService prosessinstansService;

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
        opprettNySakFraOppgave = new OpprettNySakFraOppgave(joarkFasade, oppgaveService, prosessinstansService, unleash);
        unleash.enableAll();
    }

    @Test
    void bestillNySakOgBehandling_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.ØVRIGE_SED_MED);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId("1234").build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);
        when(joarkFasade.hentJournalpost("1234")).thenReturn(lagJournalpost(Journalposttype.INN));
        opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto);
        verify(prosessinstansService).opprettProsessinstansNySak(oppgave.getJournalpostId(), opprettSakDto, Behandlingstyper.SED);
    }

    @Test
    void bestillNySakOgBehandling_sakstypeFtrlFeatureToggleEnabled_oppretterProsess() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.FTRL);
        opprettSakDto.setBehandlingstema(Behandlingstema.ARBEID_I_UTLANDET);
        unleash.enableAll();
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId("1234").build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);
        when(joarkFasade.hentJournalpost("1234")).thenReturn(lagJournalpost(Journalposttype.INN));
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
        when(joarkFasade.hentJournalpost("1234")).thenReturn(lagJournalpost(Journalposttype.INN));
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
    void bestillNySakOgBehandling_sakstypeTrygdeavtaleFeatureToggleDisabled_kasterException() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.TRYGDEAVTALE);
        opprettSakDto.setBehandlingstema(Behandlingstema.YRKESAKTIV);
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
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId("jpID").build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);
        when(joarkFasade.hentJournalpost("jpID")).thenReturn(lagJournalpost(Journalposttype.UT));
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto))
            .withMessageContaining("utgående journalposter");
    }

    private Journalpost lagJournalpost(Journalposttype journalposttype) {
        final Journalpost journalpost = new Journalpost("jpID");
        journalpost.setJournalposttype(journalposttype);
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
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto))
            .withMessageContaining("Behandlingstema");
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
        opprettSakDto.setSoknadDto(null);
        Oppgave oppgave = new Oppgave.Builder().setOppgavetype(Oppgavetyper.BEH_SAK_MK).setJournalpostId("1234").build();
        when(oppgaveService.hentOppgaveMedOppgaveID(opprettSakDto.getOppgaveID())).thenReturn(oppgave);
        when(joarkFasade.hentJournalpost("1234")).thenReturn(lagJournalpost(Journalposttype.INN));
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
        when(joarkFasade.hentJournalpost("1234")).thenReturn(lagJournalpost(Journalposttype.INN));
        opprettNySakFraOppgave.bestillNySakOgBehandling(opprettSakDto);
        verify(prosessinstansService).opprettProsessinstansNySakFTRLTrygdeavtale(oppgave.getJournalpostId(), opprettSakDto);
    }

    @Test
    void validerOpprettSakDto_søknadUtenFom_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
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
