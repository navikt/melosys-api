package no.nav.melosys.integrasjon.ereg;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;

/**
 * Fasade mot Enhetsregisteret (EREG)
 *
 */
public interface EregFasade {

    Saksopplysning hentOrganisasjon(String orgnummer) throws IntegrasjonException, SikkerhetsbegrensningException;

}
