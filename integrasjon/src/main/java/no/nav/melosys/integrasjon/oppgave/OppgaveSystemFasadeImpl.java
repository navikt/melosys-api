package no.nav.melosys.integrasjon.oppgave;

import no.nav.melosys.integrasjon.oppgave.konsument.OppgaveConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class OppgaveSystemFasadeImpl extends OppgaveFasadeImpl implements OppgaveFasade {

    @Autowired
    public OppgaveSystemFasadeImpl(@Qualifier("system")OppgaveConsumer oppgaveConsumer) {
        super(oppgaveConsumer);
    }
}
