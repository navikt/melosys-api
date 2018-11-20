package no.nav.melosys.integrasjon.gsak;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.gsak.oppgave.OppgaveConsumer;
import no.nav.melosys.integrasjon.gsak.sak.SakConsumer;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;

public final class GsakServiceTest {

    private final GsakService instans;

    public GsakServiceTest() {
        SakConsumer sakConsumer = mock(SakConsumer.class);
        OppgaveConsumer oppgaveConsumer = mock(OppgaveConsumer.class);
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

}
