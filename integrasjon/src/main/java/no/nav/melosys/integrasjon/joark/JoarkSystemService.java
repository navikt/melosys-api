package no.nav.melosys.integrasjon.joark;

import no.nav.melosys.integrasjon.joark.inngaaendejournal.InngaaendeJournalConsumer;
import no.nav.melosys.integrasjon.joark.journal.JournalConsumer;
import no.nav.melosys.integrasjon.joark.journalfoerinngaaende.JournalfoerInngaaendeConsumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("system")
public class JoarkSystemService extends JoarkService {
    public JoarkSystemService(InngaaendeJournalConsumer inngåendeJournal, @Qualifier("system") JournalConsumer journal, JournalfoerInngaaendeConsumer journalfoerInngaaendeConsumer) {
        super(inngåendeJournal, journal, journalfoerInngaaendeConsumer);
    }
}
