package no.nav.melosys.integrasjon.tps.person;

import no.nav.tjeneste.virksomhet.person.v2.binding.PersonV2;

class PersonSelftestConsumerImpl implements PersonSelftestConsumer {
    private PersonV2 port;
    private String endpointUrl;

    public PersonSelftestConsumerImpl(PersonV2 port, String endpointUrl) {
        this.port = port;
        this.endpointUrl = endpointUrl;
    }

    @Override
    public void ping() {
        port.ping();
    }

    @Override
    public String getEndpointUrl() {
        return endpointUrl;
    }
}
