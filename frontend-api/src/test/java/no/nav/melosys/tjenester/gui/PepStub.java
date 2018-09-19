package no.nav.melosys.tjenester.gui;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.Pep;

public class PepStub implements Pep {

    @Override
    public void sjekkTilgangTil(String fnr) throws SikkerhetsbegrensningException {
        
    }

    @Override
    public void sjekkTilgangTil(Aktoer aktør) throws SikkerhetsbegrensningException, IkkeFunnetException {

    }
}
