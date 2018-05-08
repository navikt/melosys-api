package no.nav.melosys.service.journalforing;

import no.nav.melosys.domain.Journalpost;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.journalforing.dto.JournalforingDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JournalforingService {

    JoarkFasade joarkFasade;

    @Autowired
    public JournalforingService(JoarkFasade joarkFasade) {
        this.joarkFasade = joarkFasade;
    }

    public Journalpost hentJournalpost(String journalpostID) throws SikkerhetsbegrensningException {
        return joarkFasade.hentJournalpost(journalpostID);
    }

    public void opprettSakOgJournalfør(JournalforingDto journalforingDto) {
        //FIXME i MELOSYS-1214
    }

    public void tilordneSakOgJournalfør(JournalforingDto journalforingDto) {
        //FIXME i MELOSYS-1214
    }
}
