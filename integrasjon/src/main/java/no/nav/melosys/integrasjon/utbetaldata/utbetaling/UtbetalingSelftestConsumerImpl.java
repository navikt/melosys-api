package no.nav.melosys.integrasjon.utbetaldata.utbetaling;

import no.nav.tjeneste.virksomhet.utbetaling.v1.binding.UtbetalingV1;

public class UtbetalingSelftestConsumerImpl implements UtbetalingSelftestConsumer {
    private UtbetalingV1 port;
    private String endpointUrl;

    public UtbetalingSelftestConsumerImpl(UtbetalingV1 port, String endpointUrl) {
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
