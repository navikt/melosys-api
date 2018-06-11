package no.nav.melosys.integrasjon.tps.person;


import no.nav.tjeneste.virksomhet.person.v3.binding.*;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.*;

public class PersonConsumerImpl implements PersonConsumer {
    private PersonV3 port;

    public PersonConsumerImpl(PersonV3 port) {
        this.port = port;
    }

    @Override
    public HentPersonResponse hentPerson(HentPersonRequest request) throws HentPersonSikkerhetsbegrensning, HentPersonPersonIkkeFunnet {
        return port.hentPerson(request);
    }

    @Override
    public HentPersonerMedSammeAdresseResponse hentPersonerMedSammeAdresse(HentPersonerMedSammeAdresseRequest request)
            throws HentPersonerMedSammeAdresseSikkerhetsbegrensning, HentPersonerMedSammeAdresseIkkeFunnet {
        return port.hentPersonerMedSammeAdresse(request);
    }

    @Override
    public HentPersonhistorikkResponse hentPersonhistorikk(HentPersonhistorikkRequest request) throws HentPersonhistorikkPersonIkkeFunnet, HentPersonhistorikkSikkerhetsbegrensning {
        return port.hentPersonhistorikk(request);
    }
}
