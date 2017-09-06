package no.nav.melosys.integrasjon.ereg;

import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonOrganisasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonUgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.Organisasjon;

/**
 * Fasade mot Enhetsregisteret (EREG)
 *
 */
public interface EregFasade {

    Organisasjon hentOrganisasjon(String orgnummer) throws HentOrganisasjonOrganisasjonIkkeFunnet, HentOrganisasjonUgyldigInput;

}
