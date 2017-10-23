package no.nav.melosys.integrasjon.tps;

import java.util.Collection;
import java.util.Optional;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov;

public interface TpsFasade {

    Optional<String> hentAktørIdForIdent(String fnr);

    Optional<String> hentIdentForAktørId(String aktørID);

    Saksopplysning hentPersonMedAdresse(String ident) throws IntegrasjonException, SikkerhetsbegrensningException;

    Saksopplysning hentPerson(String ident, Collection<Informasjonsbehov> behov) throws IntegrasjonException, SikkerhetsbegrensningException;
    
}
