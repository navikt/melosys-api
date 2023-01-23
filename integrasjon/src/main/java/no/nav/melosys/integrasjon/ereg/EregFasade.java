package no.nav.melosys.integrasjon.ereg;

import java.util.Optional;

import no.nav.melosys.domain.Saksopplysning;

/**
 * Fasade mot Enhetsregisteret (EREG)
 */
public interface EregFasade {

    Saksopplysning hentOrganisasjon(String orgnr);

    Optional<Saksopplysning> finnOrganisasjon(String orgnr);

    String hentOrganisasjonNavn(String orgnummer);
}
