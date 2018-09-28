package no.nav.melosys.sikkerhet.abac;

import no.nav.melosys.exception.SikkerhetsbegrensningException;

public interface Pep {
    void sjekkTilgangTilFnr(String fnr) throws SikkerhetsbegrensningException;
    void sjekkTilgangTilAktoerId(String aktoerId) throws SikkerhetsbegrensningException;
}
