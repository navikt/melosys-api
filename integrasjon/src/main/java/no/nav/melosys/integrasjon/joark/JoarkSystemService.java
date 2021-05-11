package no.nav.melosys.integrasjon.joark;

import no.finn.unleash.Unleash;
import no.nav.melosys.integrasjon.joark.journal.JournalConsumer;
import no.nav.melosys.integrasjon.joark.journalfoerinngaaende.JournalfoerInngaaendeConsumer;
import no.nav.melosys.integrasjon.joark.journalpostapi.JournalpostapiConsumer;
import no.nav.melosys.integrasjon.joark.saf.SafConsumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class JoarkSystemService extends JoarkService {
    public JoarkSystemService(@Qualifier("system") JournalConsumer journal,
                              JournalfoerInngaaendeConsumer journalfoerInngaaendeConsumer,
                              JournalpostapiConsumer journalpostapiConsumer,
                              @Qualifier("system") SafConsumer safConsumer,
                              Unleash unleash) {
        super(journal, journalfoerInngaaendeConsumer, journalpostapiConsumer, safConsumer, unleash);
    }
}
