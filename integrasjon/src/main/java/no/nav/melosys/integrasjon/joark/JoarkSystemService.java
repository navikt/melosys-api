package no.nav.melosys.integrasjon.joark;

import no.nav.melosys.integrasjon.joark.journalpostapi.JournalpostapiConsumer;
import no.nav.melosys.integrasjon.joark.saf.SafConsumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class JoarkSystemService extends JoarkService {
    public JoarkSystemService(JournalpostapiConsumer journalpostapiConsumer,
                              SafConsumer safConsumer) {
        super(journalpostapiConsumer, safConsumer);
    }
}
