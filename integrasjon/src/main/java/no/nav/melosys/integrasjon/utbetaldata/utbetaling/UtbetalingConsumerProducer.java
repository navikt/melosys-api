package no.nav.melosys.integrasjon.utbetaldata.utbetaling;

import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.melosys.sikkerhet.sts.StsLogin;
import no.nav.tjeneste.virksomhet.utbetaling.v1.UtbetalingV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UtbetalingConsumerProducer {
    private final UtbetalingConsumerConfig consumerConfig;
    private final StsLogin stsLogin;

    public UtbetalingConsumerProducer(UtbetalingConsumerConfig consumerConfig, StsLogin stsLogin) {
        this.consumerConfig = consumerConfig;
        this.stsLogin = stsLogin;
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
        return StsConfigurationUtil.wrapWithSts(port, NAVSTSClient.StsClientType.SYSTEM_SAML, stsLogin);
    }
}
