package no.nav.melosys.integrasjon.ereg;

import java.util.Optional;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.IkkeFunnetException;

/**
 * Fasade mot Enhetsregisteret (EREG)
 */
public interface EregFasade {

    Saksopplysning hentOrganisasjon(String orgnr) throws IkkeFunnetException;

    Optional<Saksopplysning> finnOrganisasjon(String orgnr);

    String hentOrganisasjonNavn(String orgnummer) throws IkkeFunnetException;
}
