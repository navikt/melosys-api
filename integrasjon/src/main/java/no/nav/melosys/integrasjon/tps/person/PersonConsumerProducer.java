package no.nav.melosys.integrasjon.tps.person;


import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static no.nav.melosys.sikkerhet.sts.NAVSTSClient.StsClientType.SECURITYCONTEXT_TIL_SAML;
import static no.nav.melosys.sikkerhet.sts.NAVSTSClient.StsClientType.SYSTEM_SAML;


@Configuration
public class PersonConsumerProducer {
    private PersonConsumerConfig consumerConfig;

    @Autowired
    public void setConfig(PersonConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    @Bean
    public PersonConsumer personConsumer() {
        PersonV3 port = wrapWithSts(consumerConfig.getPort(), SECURITYCONTEXT_TIL_SAML);
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
