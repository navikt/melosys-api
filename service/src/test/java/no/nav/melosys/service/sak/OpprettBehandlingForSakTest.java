package no.nav.melosys.service.sak;

import java.time.LocalDate;
import java.util.List;

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
    void opprett_behandling_med_aktiv_behandling_feiler() {
        Behandling behandling = lagBehandling();
        Fagsak fagsak = lagFagsak(behandling);
        OpprettSakDto opprettSakDto = random.nextObject(OpprettSakDto.class);

        when(fagsakService.hentFagsak(fagsak.getSaksnummer())).thenReturn(fagsak);

        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> opprettBehandlingForSak.opprettBehandling(fagsak.getSaksnummer(), opprettSakDto))
            .withMessageContaining(String.format("Det finnes allerede en aktiv behandling på fagsak %s", fagsak.getSaksnummer()));
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

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setFagsak(lagFagsak(behandling));
        behandling.setType(Behandlingstyper.SOEKNAD);
        return behandling;
    }

    private Fagsak lagFagsak(Behandling behandling) {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        behandling.setType(Behandlingstyper.SOEKNAD);
        fagsak.setBehandlinger(List.of(behandling));
        return fagsak;
    }
}
