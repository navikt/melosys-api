package no.nav.melosys.service.sak;

import java.time.LocalDate;
import java.util.Arrays;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.journalforing.dto.PeriodeDto;
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
class OpprettBehandlingForSakTest {
    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private FagsakService fagsakService;
    private OpprettBehandlingForSak opprettBehandlingForSak;

    private static final EasyRandom random = new EasyRandom(getRandomConfig());

    private static EasyRandomParameters getRandomConfig() {
        return new EasyRandomParameters().collectionSizeRange(1, 4)
            .randomize(PeriodeDto.class, () -> new PeriodeDto(LocalDate.now(), LocalDate.now().plusDays(1)))
            .stringLengthRange(2, 4);
    }

    @BeforeEach
    public void setUp() {
        opprettBehandlingForSak = new OpprettBehandlingForSak(fagsakService, prosessinstansService);
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

        opprettBehandlingForSak.opprettBehandling(fagsak.getSaksnummer(), opprettSakDto);

        verify(prosessinstansService).opprettNyBehandlingForSak(fagsak.getSaksnummer(), opprettSakDto);
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
            .isThrownBy(() -> opprettBehandlingForSak.opprettBehandling(fagsak.getSaksnummer(), opprettSakDto))
            .withMessageContaining("Det finnes allerede en aktiv behandling på fagsak");
    }

    @Test
    void lagNyBehandlingForSak_validerOpprettSakDto_manglerBehandlingstema_feiler() {
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);
        opprettSakDto.setSakstype(Sakstyper.EU_EOS);
        opprettSakDto.setBehandlingstema(null);
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-1");

        when(fagsakService.hentFagsak(fagsak.getSaksnummer())).thenReturn(fagsak);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettBehandlingForSak.opprettBehandling(fagsak.getSaksnummer(), opprettSakDto))
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

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettBehandlingForSak.opprettBehandling(fagsak.getSaksnummer(), opprettSakDto))
            .withMessageContaining("Behandlingstype");
    }

}
