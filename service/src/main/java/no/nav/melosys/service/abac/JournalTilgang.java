package no.nav.melosys.service.abac;

import no.nav.melosys.domain.Journalpost;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.Pep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JournalTilgang {

    private JoarkFasade joarkFasade;
    private Pep pep;

    @Autowired
    public JournalTilgang(JoarkFasade joarkFasade, Pep pep) {
        this.joarkFasade = joarkFasade;
        this.pep = pep;
    }

    public void sjekk(String journalId) throws SikkerhetsbegrensningException, IkkeFunnetException {
        Journalpost journalpost = joarkFasade.hentJournalpost(journalId);
        pep.sjekkTilgangTil(journalpost.getBrukerId());
    }
}
