package no.nav.melosys.integrasjon.tps.person;


import static no.nav.vedtak.sts.client.NAVSTSClient.StsClientType.OIDC_TIL_SAML;
import static no.nav.vedtak.sts.client.NAVSTSClient.StsClientType.SYSTEM_SAML;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3;
import no.nav.vedtak.sts.client.NAVSTSClient;
import no.nav.vedtak.sts.client.StsConfigurationUtil;

@Configuration
public class PersonConsumerProducer {
    private PersonConsumerConfig consumerConfig;

    @Autowired
    public void setConfig(PersonConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    @Bean
    @Profile("utvikling")
    PersonConsumer personMock() {
        return new PersonMock();
    }

    @Bean
    @Profile("!utvikling")
    public PersonConsumer personConsumer() {
        PersonV3 port = wrapWithSts(consumerConfig.getPort(), OIDC_TIL_SAML);
        return new PersonConsumerImpl(port);
    }

    @Bean
    public PersonSelftestConsumer personSelftestConsumer() {
        PersonV3 port = wrapWithSts(consumerConfig.getPort(), SYSTEM_SAML);
        return new PersonSelftestConsumerImpl(port, consumerConfig.getEndpointUrl());
    }

    PersonV3 wrapWithSts(PersonV3 port, NAVSTSClient.StsClientType samlTokenType) {
        return StsConfigurationUtil.wrapWithSts(port, samlTokenType);
    }

}
