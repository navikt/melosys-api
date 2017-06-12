package no.nav.melosys.integrasjon.tps.aktoer;

import static no.nav.vedtak.sts.client.NAVSTSClient.StsClientType.OIDC_TIL_SAML;
import static no.nav.vedtak.sts.client.NAVSTSClient.StsClientType.SYSTEM_SAML;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import no.nav.tjeneste.virksomhet.aktoer.v2.binding.AktoerV2;
import no.nav.vedtak.sts.client.NAVSTSClient;
import no.nav.vedtak.sts.client.StsConfigurationUtil;

@Configuration
public class AktorConsumerProducer {
    private AktorConsumerConfig consumerConfig;

    @Autowired
    public void setConfig(AktorConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    @Bean
    public AktorConsumer aktorConsumer() {
        AktoerV2 port = wrapWithSts(consumerConfig.getPort(), OIDC_TIL_SAML);
        return new AktorConsumerImpl(port);
    }

    @Bean
    public AktorSelftestConsumer aktorSelftestConsumer() {
        AktoerV2 port = wrapWithSts(consumerConfig.getPort(), SYSTEM_SAML);
        return new AktorSelftestConsumerImpl(port, consumerConfig.getEndpointUrl());
    }

    AktoerV2 wrapWithSts(AktoerV2 port, NAVSTSClient.StsClientType samlTokenType) {
        return StsConfigurationUtil.wrapWithSts(port, samlTokenType);
    }

}
