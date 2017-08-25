package no.nav.melosys.integrasjon.ereg.organisasjon;

import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.OrganisasjonV4;

public class OrganisasjonSelftestConsumerImpl implements OrganisasjonSelftestConsumer {

    private OrganisasjonV4 port;

    private String endpointUrl;

    public OrganisasjonSelftestConsumerImpl(OrganisasjonV4 port, String endpointUrl) {
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
