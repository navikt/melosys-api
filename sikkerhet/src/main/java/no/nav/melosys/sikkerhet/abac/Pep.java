package no.nav.melosys.sikkerhet.abac;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.exception.SikkerhetsbegrensningException;

public interface Pep {
    void sjekkTilgangTil(String fnr) throws SikkerhetsbegrensningException;
    void sjekkTilgangTil(Aktoer aktoer) throws SikkerhetsbegrensningException;
}
