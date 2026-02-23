package no.nav.melosys.integrasjon.doksys.dokumentproduksjon;

import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.DokumentproduksjonV3;

public class DokumentproduksjonSelftestClientImpl implements DokumentproduksjonSelftestClient {

    private DokumentproduksjonV3 port;

    private String endpointUrl;

    public DokumentproduksjonSelftestClientImpl(DokumentproduksjonV3 port, String endpointUrl) {
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
