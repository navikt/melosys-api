package no.nav.melosys.integrasjon.tps.person;

import static no.nav.melosys.integrasjon.felles.StsClient.Type.OIDC_TIL_SAML;
import static no.nav.melosys.integrasjon.felles.StsClient.Type.SYSTEM_SAML;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import no.nav.melosys.integrasjon.felles.StsClient;
import no.nav.tjeneste.virksomhet.person.v2.binding.PersonV2;

@Configuration
public class PersonConsumerProducer {
    private PersonConsumerConfig consumerConfig;

    @Autowired
    public void setConfig(PersonConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    @Bean
    public PersonConsumer personConsumer() {
        PersonV2 port = wrapWithSts(consumerConfig.getPort(), OIDC_TIL_SAML);
        return new PersonConsumerImpl(port);
    }

    @Bean
    public PersonSelftestConsumer personSelftestConsumer() {
        PersonV2 port = wrapWithSts(consumerConfig.getPort(), SYSTEM_SAML);
        return new PersonSelftestConsumerImpl(port, consumerConfig.getEndpointUrl());
    }

    PersonV2 wrapWithSts(PersonV2 port, StsClient.Type samlTokenType) {
        return port; // FIXME
    }

}
