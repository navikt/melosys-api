package no.nav.melosys.integrasjon.gsak;

import no.nav.melosys.integrasjon.gsak.oppgave.OppgaveConsumer;
import no.nav.melosys.integrasjon.gsak.sakapi.SakApiConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class GsakSystemService extends GsakService implements GsakFasade {

    @Autowired
    public GsakSystemService(@Qualifier("system")SakApiConsumer sakApiConsumer, @Qualifier("system")OppgaveConsumer oppgaveConsumer) {
        super(sakApiConsumer, oppgaveConsumer);
    }
}
