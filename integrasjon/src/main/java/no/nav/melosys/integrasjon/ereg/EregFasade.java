package no.nav.melosys.integrasjon.ereg;

import no.nav.melosys.domain.Saksopplysning;

/**
 * Fasade mot Enhetsregisteret (EREG)
 *
 */
public interface EregFasade {

    Saksopplysning hentOrganisasjon(String orgnummer);

    String hentOrganisasjonNavn(String orgnummer);

    boolean organisasjonFinnes(String orgnummer);
}
