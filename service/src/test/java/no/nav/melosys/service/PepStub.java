package no.nav.melosys.service;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;

public class PepStub implements Pep {

    @Override
    public void sjekkTilgangTil(String fnr) throws SikkerhetsbegrensningException {
        
    }

    @Override
    public void sjekkTilgangTil(Aktoer aktør) throws SikkerhetsbegrensningException, IkkeFunnetException {

    }
}
