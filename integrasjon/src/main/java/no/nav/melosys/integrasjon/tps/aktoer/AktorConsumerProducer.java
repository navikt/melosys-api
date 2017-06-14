package no.nav.melosys.integrasjon.tps.aktoer;

import static no.nav.melosys.integrasjon.felles.StsClient.Type.OIDC_TIL_SAML;
import static no.nav.melosys.integrasjon.felles.StsClient.Type.SYSTEM_SAML;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import no.nav.melosys.integrasjon.felles.StsClient;
import no.nav.tjeneste.virksomhet.aktoer.v2.binding.AktoerV2;

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

    AktoerV2 wrapWithSts(AktoerV2 port, StsClient.Type samlTokenType) {
        return port; // FIXME
    }

}
