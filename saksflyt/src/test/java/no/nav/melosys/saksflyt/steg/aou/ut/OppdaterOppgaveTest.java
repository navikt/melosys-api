package no.nav.melosys.saksflyt.steg.aou.ut;

import java.time.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.integrasjon.gsak.OppgaveOppdatering;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.FERDIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterOppgaveTest {

    private static final String OPPGAVE_ID = "123";
    private static final String ANMODNING_UNNTAK_BESKRIVELSE = "Anmodning om unntak er sendt utenlandsk trygdemyndighet.";
    private static final String SAKSNUMMER = "234";

    @Mock
    private GsakFasade gsakFasade;

    @Captor
    private ArgumentCaptor<OppgaveOppdatering> oppgaveCaptor;

    private OppdaterOppgave agent;

    private Prosessinstans prosessinstans = new Prosessinstans();

    @Before
    public void setUp() {
        agent = new OppdaterOppgave(gsakFasade);
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
    public void oppdaterOppgaveBeskrivelseOgFrist_eksisterendeFristForKort_fristSettes() throws Exception {
        LocalDate enMånedFremITid = LocalDate.now().plusMonths(1L);

        Oppgave oppgave = lagOppgave(enMånedFremITid, null);
        when(gsakFasade.hentOppgaveMedSaksnummer(anyString())).thenReturn(oppgave);

        LocalDate toMånederFremITid = LocalDate.now().plusMonths(2L);

        agent.utførSteg(prosessinstans);
        verify(gsakFasade).hentOppgaveMedSaksnummer(SAKSNUMMER);
        verify(gsakFasade).oppdaterOppgave(eq(oppgave.getOppgaveId()), oppgaveCaptor.capture());
        assertThat(oppgaveCaptor.getValue().getFristFerdigstillelse()).isEqualTo(toMånederFremITid);
        assertThat(oppgaveCaptor.getValue().getBeskrivelse()).isEqualTo(ANMODNING_UNNTAK_BESKRIVELSE);
        assertThat(prosessinstans.getSteg()).isEqualTo(FERDIG);

    }

    @Test
    public void oppdaterOppgaveBeskrivelseOgFrist_eksisterendeFristLangNok_fristBeholdes() throws Exception {
        String eksisterendeBeskrivelse = "Eksisterende beskrivelse";
        LocalDate treMånederFremITid = LocalDate.now().plusMonths(3L);

        Oppgave oppgave = lagOppgave(treMånederFremITid, eksisterendeBeskrivelse);
        when(gsakFasade.hentOppgaveMedSaksnummer(anyString())).thenReturn(oppgave);

        agent.utførSteg(prosessinstans);
        verify(gsakFasade).hentOppgaveMedSaksnummer(SAKSNUMMER);
        verify(gsakFasade).oppdaterOppgave(eq(oppgave.getOppgaveId()), oppgaveCaptor.capture());
        assertThat(oppgaveCaptor.getValue().getFristFerdigstillelse()).isNull();
        assertThat(oppgaveCaptor.getValue().getBeskrivelse()).isEqualTo(ANMODNING_UNNTAK_BESKRIVELSE);
        assertThat(prosessinstans.getSteg()).isEqualTo(FERDIG);
    }

    private Oppgave lagOppgave(LocalDate fristFerdigstillelse, String beskrivelse) {
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setFristFerdigstillelse(fristFerdigstillelse);
        oppgaveBuilder.setOppgaveId(OPPGAVE_ID);
        oppgaveBuilder.setBeskrivelse(beskrivelse);
        return oppgaveBuilder.build();
    }

}