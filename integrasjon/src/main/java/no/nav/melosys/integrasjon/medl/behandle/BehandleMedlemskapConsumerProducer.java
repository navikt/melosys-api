package no.nav.melosys.integrasjon.medl.behandle;

import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.BehandleMedlemskapV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BehandleMedlemskapConsumerProducer {

    private BehandleMedlemskapConsumerConfig consumerConfig;

    @Autowired
    public void setConfig(BehandleMedlemskapConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    @Bean
    public BehandleMedlemskapConsumer getConsumer() {
        BehandleMedlemskapV2 port = wrapWithSts(consumerConfig.getPort(), NAVSTSClient.StsClientType.SYSTEM_SAML);
        return new BehandleMedlemskapConsumerImpl(port);
    }

    @Bean
    public BehandleMedlemskapSelftestConsumer getSelftestConsumer() {
        BehandleMedlemskapV2 port = wrapWithSts(consumerConfig.getPort(), NAVSTSClient.StsClientType.SYSTEM_SAML);
        return new BehandleMedlemskapSelftestConsumerImpl(port, consumerConfig.getEndpointUrl());
    }

    private BehandleMedlemskapV2 wrapWithSts(BehandleMedlemskapV2 port, NAVSTSClient.StsClientType samlTokenType) {
        return StsConfigurationUtil.wrapWithSts(port, samlTokenType);
    }
}
