package no.nav.melosys.integrasjon.tps;

import java.util.Collection;
import java.util.Optional;

import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;

public interface TpsFasade {

    Optional<String> hentAktørIdForIdent(String fnr);

    Optional<String> hentIdentForAktørId(String aktørID);

    HentPersonResponse hentPerson(String ident) throws HentPersonPersonIkkeFunnet, HentPersonSikkerhetsbegrensning;

    HentPersonResponse hentPerson(String ident, Collection<Informasjonsbehov> behov) throws HentPersonPersonIkkeFunnet, HentPersonSikkerhetsbegrensning;
}
