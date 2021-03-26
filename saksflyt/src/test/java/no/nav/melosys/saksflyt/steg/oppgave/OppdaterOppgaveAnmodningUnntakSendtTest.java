package no.nav.melosys.saksflyt.steg.oppgave;

import java.time.*;

import no.nav.melosys.domain.behandling.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.integrasjon.oppgave.OppgaveOppdatering;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OppdaterOppgaveAnmodningUnntakSendtTest {

    private static final String OPPGAVE_ID = "123";
    private static final String ANMODNING_UNNTAK_BESKRIVELSE = "Anmodning om unntak er sendt utenlandsk trygdemyndighet.";
    private static final String SAKSNUMMER = "234";

    @Mock
    private OppgaveService oppgaveService;

    @Captor
    private ArgumentCaptor<OppgaveOppdatering> oppgaveCaptor;

    private OppdaterOppgaveAnmodningUnntakSendt oppdaterOppgaveAnmodningUnntakSendt;

    private final Prosessinstans prosessinstans = new Prosessinstans();

    @BeforeEach
    public void setUp() {
        oppdaterOppgaveAnmodningUnntakSendt = new OppdaterOppgaveAnmodningUnntakSendt(oppgaveService);
        Behandling behandling = new Behandling();
        LocalDate toMånederFremITid = LocalDate.now().plusMonths(2L);
        behandling.setDokumentasjonSvarfristDato(Instant.from(ZonedDateTime.of(toMånederFremITid, LocalTime.MAX, ZoneId.systemDefault())));
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        behandling.setFagsak(fagsak);
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.OPPGAVE_ID, OPPGAVE_ID);

    }

    @Test
    void oppdaterOppgaveBeskrivelseOgFrist_eksisterendeFristForKort_fristSettes() throws Exception {
        LocalDate enMånedFremITid = LocalDate.now().plusMonths(1L);

        Oppgave oppgave = lagOppgave(enMånedFremITid, null);
        when(oppgaveService.hentOppgaveMedFagsaksnummer(anyString())).thenReturn(oppgave);

        LocalDate toMånederFremITid = LocalDate.now().plusMonths(2L);

        oppdaterOppgaveAnmodningUnntakSendt.utfør(prosessinstans);
        verify(oppgaveService).hentOppgaveMedFagsaksnummer(SAKSNUMMER);
        verify(oppgaveService).oppdaterOppgave(eq(oppgave.getOppgaveId()), oppgaveCaptor.capture());
        assertThat(oppgaveCaptor.getValue().getFristFerdigstillelse()).isEqualTo(toMånederFremITid);
        assertThat(oppgaveCaptor.getValue().getBeskrivelse()).isEqualTo(ANMODNING_UNNTAK_BESKRIVELSE);
    }

    @Test
    void oppdaterOppgaveBeskrivelseOgFrist_eksisterendeFristLangNok_fristBeholdes() throws Exception {
        String eksisterendeBeskrivelse = "Eksisterende beskrivelse";
        LocalDate treMånederFremITid = LocalDate.now().plusMonths(3L);

        Oppgave oppgave = lagOppgave(treMånederFremITid, eksisterendeBeskrivelse);
        when(oppgaveService.hentOppgaveMedFagsaksnummer(anyString())).thenReturn(oppgave);

        oppdaterOppgaveAnmodningUnntakSendt.utfør(prosessinstans);
        verify(oppgaveService).hentOppgaveMedFagsaksnummer(SAKSNUMMER);
        verify(oppgaveService).oppdaterOppgave(eq(oppgave.getOppgaveId()), oppgaveCaptor.capture());
        assertThat(oppgaveCaptor.getValue().getFristFerdigstillelse()).isNull();
        assertThat(oppgaveCaptor.getValue().getBeskrivelse()).isEqualTo(ANMODNING_UNNTAK_BESKRIVELSE);
    }

    private Oppgave lagOppgave(LocalDate fristFerdigstillelse, String beskrivelse) {
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setFristFerdigstillelse(fristFerdigstillelse);
        oppgaveBuilder.setOppgaveId(OPPGAVE_ID);
        oppgaveBuilder.setBeskrivelse(beskrivelse);
        return oppgaveBuilder.build();
    }
}