package no.nav.melosys.integrasjon.ereg;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;

/**
 * Fasade mot Enhetsregisteret (EREG)
 *
 */
public interface EregFasade {

    Saksopplysning hentOrganisasjon(String orgnummer) throws IkkeFunnetException, IntegrasjonException;

    String hentOrganisasjonNavn(String orgnummer) throws IkkeFunnetException, IntegrasjonException;

    boolean organisasjonFinnes(String orgnummer) throws IntegrasjonException;
}
