package no.nav.melosys.integrasjon.oppgave;

import no.nav.melosys.integrasjon.oppgave.konsument.OppgaveConsumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class OppgaveSystemFasadeImpl extends OppgaveFasadeImpl {

    public OppgaveSystemFasadeImpl(OppgaveConsumer oppgaveConsumer) {
        super(oppgaveConsumer);
    }
}
