package no.nav.melosys.integrasjon.ereg;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;

/**
 * Fasade mot Enhetsregisteret (EREG)
 *
 */
public interface EregFasade {

    Saksopplysning hentOrganisasjon(String orgnummer) throws IkkeFunnetException, SikkerhetsbegrensningException;

}
