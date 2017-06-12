package no.nav.melosys.integrasjon.gsak.behandlesak;

import static no.nav.vedtak.sts.client.NAVSTSClient.StsClientType.OIDC_TIL_SAML;
import static no.nav.vedtak.sts.client.NAVSTSClient.StsClientType.SYSTEM_SAML;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import no.nav.tjeneste.virksomhet.behandlesak.v1.binding.BehandleSakV1;
import no.nav.vedtak.sts.client.NAVSTSClient;
import no.nav.vedtak.sts.client.StsConfigurationUtil;

@Configuration
public class BehandleSakConsumerProducer {
    private BehandleSakConsumerConfig consumerConfig;

    @Autowired
    public void setConfig(BehandleSakConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    @Bean
    public BehandleSakConsumer behandleSakConsumer() {
        BehandleSakV1 port = wrapWithSts(consumerConfig.getPort(), OIDC_TIL_SAML);
        return new BehandleSakConsumerImpl(port);
    }

    @Bean
    public BehandleSakSelftestConsumer behandleSakSelftestConsumer() {
        BehandleSakV1 port = wrapWithSts(consumerConfig.getPort(), SYSTEM_SAML);
        return new BehandleSakSelftestConsumerImpl(port, consumerConfig.getEndpointUrl());
    }

    BehandleSakV1 wrapWithSts(BehandleSakV1 port, NAVSTSClient.StsClientType samlTokenType) {
        return StsConfigurationUtil.wrapWithSts(port, samlTokenType);
    }

}
