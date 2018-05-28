package no.nav.melosys.integrasjon.doksys.dokumentproduksjon;

import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.DokumentproduksjonV3;

public class DokumentproduksjonSelftestConsumerImpl implements DokumentproduksjonSelftestConsumer {

    private DokumentproduksjonV3 port;

    private String endpointUrl;

    public DokumentproduksjonSelftestConsumerImpl(DokumentproduksjonV3 port, String endpointUrl) {
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
