package no.nav.melosys.integrasjon.utbetaldata.utbetaling;

import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.tjeneste.virksomhet.utbetaling.v1.binding.UtbetalingV1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import static no.nav.melosys.sikkerhet.sts.NAVSTSClient.StsClientType.SYSTEM_SAML;

@Configuration
public class UtbetalingConsumerProducer {
    private UtbetalingConsumerConfig consumerConfig;

    @Autowired
    public void setConfig(UtbetalingConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    @Bean
    public UtbetalingConsumer utbetalingConsumer() {
        UtbetalingV1 port = wrapWithSts(consumerConfig.getPort(), SYSTEM_SAML);
        return new UtbetalingConsumerImpl(port);
    }

    UtbetalingV1 wrapWithSts(UtbetalingV1 port, NAVSTSClient.StsClientType samlTokenType) {
        return StsConfigurationUtil.wrapWithSts(port, samlTokenType);
    }
}
