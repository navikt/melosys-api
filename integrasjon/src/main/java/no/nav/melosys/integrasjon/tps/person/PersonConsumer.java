package no.nav.melosys.integrasjon.tps.person;

import no.nav.tjeneste.virksomhet.person.v3.binding.*;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.*;

public interface PersonConsumer {
    HentPersonResponse hentPerson(HentPersonRequest request) throws HentPersonSikkerhetsbegrensning, HentPersonPersonIkkeFunnet;

    HentPersonhistorikkResponse hentPersonhistorikk(HentPersonhistorikkRequest request) throws HentPersonhistorikkPersonIkkeFunnet, HentPersonhistorikkSikkerhetsbegrensning;
}
