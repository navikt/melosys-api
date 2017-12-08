package no.nav.melosys.integrasjon.tps.aktoer;


import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.AktoerV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.melosys.sikkerhet.sts.NAVSTSClient.StsClientType.SECURITYCONTEXT_TIL_SAML;


@Configuration
public class AktorConsumerProducer {
    private AktorConsumerConfig consumerConfig;

    @Autowired
    public void setConfig(AktorConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    @Bean
    public AktorConsumer aktorConsumer() {
        AktoerV2 port = wrapWithSts(consumerConfig.getPort(), SECURITYCONTEXT_TIL_SAML);
        return new AktorConsumerImpl(port);
    }

    @Bean
    public AktorSelftestConsumer aktorSelftestConsumer() {
        AktoerV2 port = wrapWithSts(consumerConfig.getPort(), SECURITYCONTEXT_TIL_SAML);
        return new AktorSelftestConsumerImpl(port, consumerConfig.getEndpointUrl());
    }

    AktoerV2 wrapWithSts(AktoerV2 port, NAVSTSClient.StsClientType samlTokenType) {
        return StsConfigurationUtil.wrapWithSts(port, samlTokenType);
    }

}
