package no.nav.melosys.integrasjon.tps.person;


import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;

public class PersonConsumerImpl implements PersonConsumer {
    private PersonV3 port;

    public PersonConsumerImpl(PersonV3 port) {
        this.port = port;
    }

    @Override
    public HentPersonResponse hentPerson(HentPersonRequest request) throws HentPersonSikkerhetsbegrensning, HentPersonPersonIkkeFunnet {
        return port.hentPerson(request);
    }

}
