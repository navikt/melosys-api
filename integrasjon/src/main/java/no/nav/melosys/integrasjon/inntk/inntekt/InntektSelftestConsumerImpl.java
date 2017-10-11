package no.nav.melosys.integrasjon.inntk.inntekt;

import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3;

public class InntektSelftestConsumerImpl implements InntektSelftestConsumer {

    private final InntektV3 port;

    private final String endpointUrl;

    public InntektSelftestConsumerImpl(InntektV3 port, String endpointUrl) {
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
