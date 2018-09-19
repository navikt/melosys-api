package no.nav.melosys.service.oppgave;

import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.Pep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OppgaveTilgang {

    private final Pep pep;

    @Autowired
    public OppgaveTilgang(Pep pep) {
        this.pep = pep;
    }

    public boolean harIkkeTilgangTil(Oppgave oppgave) {
        try {
            pep.sjekkTilgangTilAktoer(oppgave.getAktørId());
        } catch (SikkerhetsbegrensningException | IkkeFunnetException e) {
            return true;
        }
        return false;
    }

    
    public void sjekkTilgangTilOppgave(Oppgave oppgave) throws SikkerhetsbegrensningException, IkkeFunnetException {
        pep.sjekkTilgangTilAktoer(oppgave.getAktørId());
    }
}
