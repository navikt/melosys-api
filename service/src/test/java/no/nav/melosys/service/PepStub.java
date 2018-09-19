package no.nav.melosys.service;

import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.sikkerhet.abac.Pep;

public class PepStub implements Pep {

    @Override
    public void sjekkTilgangTil(String fnr) throws SikkerhetsbegrensningException {
        
    }
}
