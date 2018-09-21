package no.nav.melosys.service.abac;

import no.nav.melosys.domain.Journalpost;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JournalTilgang {

    private JoarkFasade joarkFasade;
    private PepAktoerOversetter pep;

    @Autowired
    public JournalTilgang(JoarkFasade joarkFasade, PepAktoerOversetter pep) {
        this.joarkFasade = joarkFasade;
        this.pep = pep;
    }

    public void sjekk(Journalpost journalPost) throws SikkerhetsbegrensningException {
        sjekk(journalPost.getBrukerId());
    }

    public void sjekk(JournalfoeringOpprettDto journalPost) throws SikkerhetsbegrensningException {
        sjekk(journalPost.getBrukerID());
    }

    public void sjekk(JournalfoeringTilordneDto journalPost) throws SikkerhetsbegrensningException {
        sjekk(journalPost.getBrukerID());
    }

    public void sjekk(String journalId) throws SikkerhetsbegrensningException {
        Journalpost journalpost = joarkFasade.hentJournalpost(journalId);
        pep.sjekkTilgangTil(journalpost.getBrukerId());
    }
}
