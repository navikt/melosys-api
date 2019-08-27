package no.nav.melosys.integrasjon.gsak.oppgave;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveDto;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveSearchRequest;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OppgaveSvar;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class OppgaveConsumerImplTest {

    @Test
    public void hentOppgaveListe_antallTreff39_henterAlleSvarene() throws TekniskException, FunksjonellException {
        OppgaveConsumerImpl oppgaveConsumer = spy(new OppgaveConsumerImpl("", false));
        int antallTreffTotalt = 39;
        OppgaveSvar oppgaveSvar1 = lagOppgavesvar(antallTreffTotalt, 20);
        OppgaveSvar oppgaveSvar2 = lagOppgavesvar(antallTreffTotalt, 19);
        OppgaveSvar oppgaveSvar3 = lagOppgavesvar(antallTreffTotalt, 0);
        doReturn(oppgaveSvar1).when(oppgaveConsumer).hentOppgaveListe(any(), eq(0));
        doReturn(oppgaveSvar2).when(oppgaveConsumer).hentOppgaveListe(any(), eq(20));
        doReturn(oppgaveSvar3).when(oppgaveConsumer).hentOppgaveListe(any(), eq(40));

        List<OppgaveDto> oppgaveDtos = oppgaveConsumer.hentOppgaveListe(mock(OppgaveSearchRequest.class));
        assertThat(oppgaveDtos).hasSize(antallTreffTotalt);
    }

    @Test
    public void hentOppgaveListe_antallTreff40_henterAlleSvarene() throws TekniskException, FunksjonellException {
        OppgaveConsumerImpl oppgaveConsumer = spy(new OppgaveConsumerImpl("", false));
        int antallTreffTotalt = 40;
        OppgaveSvar oppgaveSvar1 = lagOppgavesvar(antallTreffTotalt, 20);
        OppgaveSvar oppgaveSvar2 = lagOppgavesvar(antallTreffTotalt, 20);
        OppgaveSvar oppgaveSvar3 = lagOppgavesvar(antallTreffTotalt, 0);
        doReturn(oppgaveSvar1).when(oppgaveConsumer).hentOppgaveListe(any(), eq(0));
        doReturn(oppgaveSvar2).when(oppgaveConsumer).hentOppgaveListe(any(), eq(20));
        doReturn(oppgaveSvar3).when(oppgaveConsumer).hentOppgaveListe(any(), eq(40));

        List<OppgaveDto> oppgaveDtos = oppgaveConsumer.hentOppgaveListe(mock(OppgaveSearchRequest.class));
        assertThat(oppgaveDtos).hasSize(antallTreffTotalt);
    }

    @Test
    public void hentOppgaveListe_antallTreff41_henterAlleSvarene() throws TekniskException, FunksjonellException {
        OppgaveConsumerImpl oppgaveConsumer = spy(new OppgaveConsumerImpl("", false));
        int antallTreffTotalt = 41;
        OppgaveSvar oppgaveSvar1 = lagOppgavesvar(antallTreffTotalt, 20);
        OppgaveSvar oppgaveSvar2 = lagOppgavesvar(antallTreffTotalt, 20);
        OppgaveSvar oppgaveSvar3 = lagOppgavesvar(antallTreffTotalt, 1);
        doReturn(oppgaveSvar1).when(oppgaveConsumer).hentOppgaveListe(any(), eq(0));
        doReturn(oppgaveSvar2).when(oppgaveConsumer).hentOppgaveListe(any(), eq(20));
        doReturn(oppgaveSvar3).when(oppgaveConsumer).hentOppgaveListe(any(), eq(40));

        List<OppgaveDto> oppgaveDtos = oppgaveConsumer.hentOppgaveListe(mock(OppgaveSearchRequest.class));
        assertThat(oppgaveDtos).hasSize(antallTreffTotalt);
    }

    @Test
    public void hentOppgaveListe_antallTreff3_henterAlleSvarene() throws TekniskException, FunksjonellException {
        OppgaveConsumerImpl oppgaveConsumer = spy(new OppgaveConsumerImpl("", false));
        int antallTreffTotalt = 3;
        OppgaveSvar oppgaveSvar1 = lagOppgavesvar(antallTreffTotalt, 3);
        doReturn(oppgaveSvar1).when(oppgaveConsumer).hentOppgaveListe(any(), eq(0));

        List<OppgaveDto> oppgaveDtos = oppgaveConsumer.hentOppgaveListe(mock(OppgaveSearchRequest.class));
        assertThat(oppgaveDtos).hasSize(antallTreffTotalt);
    }

    @Test
    public void hentOppgaveListe_antallTreff0_henterIngenSvar() throws TekniskException, FunksjonellException {
        OppgaveConsumerImpl oppgaveConsumer = spy(new OppgaveConsumerImpl("", false));
        int antallTreffTotalt = 0;
        OppgaveSvar oppgaveSvar1 = lagOppgavesvar(antallTreffTotalt, 0);
        doReturn(oppgaveSvar1).when(oppgaveConsumer).hentOppgaveListe(any(), eq(0));

        List<OppgaveDto> oppgaveDtos = oppgaveConsumer.hentOppgaveListe(mock(OppgaveSearchRequest.class));
        assertThat(oppgaveDtos).hasSize(antallTreffTotalt);
    }

    private OppgaveSvar lagOppgavesvar(int antallTreffTotalt, int antallSvar) {
        OppgaveSvar oppgaveSvar = new OppgaveSvar();
        ArrayList<OppgaveDto> oppgaver = lagOppgaver(antallSvar);
        oppgaveSvar.setOppgaver(oppgaver);
        oppgaveSvar.setAntallTreffTotalt(antallTreffTotalt);
        return oppgaveSvar;
    }

    private ArrayList<OppgaveDto> lagOppgaver(int antallSvar) {
        ArrayList<OppgaveDto> oppgaveDtos = new ArrayList<>();
        for (int i = 0; i < antallSvar; i++) {
            oppgaveDtos.add(new OppgaveDto());
        }
        return oppgaveDtos;
    }
}