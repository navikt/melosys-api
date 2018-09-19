package no.nav.melosys.service;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;

public interface Pep {
    void sjekkTilgangTil(String fnr) throws SikkerhetsbegrensningException;

    void sjekkTilgangTilAktoer(String bruker) throws SikkerhetsbegrensningException, IkkeFunnetException;
}
