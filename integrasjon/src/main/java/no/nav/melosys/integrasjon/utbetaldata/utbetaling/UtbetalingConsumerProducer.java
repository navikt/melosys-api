package no.nav.melosys.integrasjon.utbetaldata.utbetaling;

import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsWrapper;
import no.nav.tjeneste.virksomhet.utbetaling.v1.UtbetalingV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UtbetalingConsumerProducer {
    private final UtbetalingConsumerConfig consumerConfig;
    private final StsWrapper stsWrapper;

    public UtbetalingConsumerProducer(UtbetalingConsumerConfig consumerConfig, StsWrapper stsWrapper) {
        this.consumerConfig = consumerConfig;
        this.stsWrapper = stsWrapper;
    }

    @Bean
    public UtbetalingConsumer utbetalingConsumer() {
        UtbetalingV1 port = wrapWithSts(consumerConfig.getPort());
        return new UtbetalingConsumerImpl(port);
    }

    @Bean
    public UtbetalingSelftestConsumer utbetalingSelftestConsumer() {
        UtbetalingV1 port = wrapWithSts(consumerConfig.getPort());
        return new UtbetalingSelftestConsumerImpl(port, consumerConfig.getEndpointUrl());
    }

    UtbetalingV1 wrapWithSts(UtbetalingV1 port) {
        return stsWrapper.wrapWithSts(port, NAVSTSClient.StsClientType.SYSTEM_SAML);
    }
}
