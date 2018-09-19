package no.nav.melosys.sikkerhet.abac;

import no.nav.melosys.exception.SikkerhetsbegrensningException;

public interface Pep {
    void sjekkTilgangTil(String fnr) throws SikkerhetsbegrensningException;
}
