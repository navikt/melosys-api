package no.nav.melosys.integrasjon.gsak;

import java.time.LocalDate;

import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.oppgave.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.oppgave.OppgaveConsumer;
import no.nav.melosys.integrasjon.gsak.oppgave.dto.OpprettOppgaveDto;
import no.nav.melosys.integrasjon.gsak.sak.SakConsumer;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public final class GsakServiceTest {

    private final GsakService instans;
    private final OppgaveConsumer oppgaveConsumer;

    public GsakServiceTest() {
        SakConsumer sakConsumer = mock(SakConsumer.class);
        oppgaveConsumer = mock(OppgaveConsumer.class);
        instans = new GsakService(sakConsumer, oppgaveConsumer);
    }

    @Test
    public final void tildelIkkeEksisterendeOppgaveGirIkkeFunnetException() throws Exception {
        Throwable unntak = catchThrowable(() -> instans.tildelOppgave("1", "2"));
        assertThat(unntak)
                .isInstanceOf(IkkeFunnetException.class)
                .hasMessageContaining("Feil")
                .hasMessageContaining("oppgave 1 for saksbehandler 2");
    }

    @Test
    public void opprettOppgave_vurderDokument_setterData() throws FunksjonellException, TekniskException {
        Oppgave oppgave = new Oppgave();
        oppgave.setOppgavetype(Oppgavetyper.VUR);
        oppgave.setTema(Tema.MED);
        oppgave.setBehandlingstema(Behandlingstema.ARB_EØS);
        instans.opprettOppgave(oppgave);

        ArgumentCaptor<OpprettOppgaveDto> captor = ArgumentCaptor.forClass(OpprettOppgaveDto.class);
        verify(oppgaveConsumer).opprettOppgave(captor.capture());
        OpprettOppgaveDto opprettOppgaveDto = captor.getValue();

        assertThat(opprettOppgaveDto.getOppgavetype()).isEqualTo(Oppgavetyper.VUR.getKode());
        assertThat(opprettOppgaveDto.getBehandlingstema()).isEqualTo(Behandlingstema.ARB_EØS.getKode());
        assertThat(opprettOppgaveDto.getFristFerdigstillelse()).isNotNull();
    }
}
