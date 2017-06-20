package no.nav.melosys.integrasjon.joark.journal;

import static no.nav.melosys.integrasjon.felles.StsClient.Type.OIDC_TIL_SAML;
import static no.nav.melosys.integrasjon.felles.StsClient.Type.SYSTEM_SAML;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import no.nav.melosys.integrasjon.felles.StsClient;
import no.nav.tjeneste.virksomhet.journal.v2.binding.JournalV2;

@Configuration
public class JournalConsumerProducer {
    private JournalConsumerConfig consumerConfig;

    @Autowired
    public void setConfig(JournalConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    @Bean
    public JournalConsumer journalConsumer() {
        JournalV2 port = wrapWithSts(consumerConfig.getPort(), OIDC_TIL_SAML);
        return new JournalConsumerImpl(port);
    }

    @Bean
    public JournalSelftestConsumer journalSelftestConsumer() {
        JournalV2 port = wrapWithSts(consumerConfig.getPort(), SYSTEM_SAML);
        return new JournalSelftestConsumerImpl(port, consumerConfig.getEndpointUrl());
    }

    JournalV2 wrapWithSts(JournalV2 port, StsClient.Type samlTokenType) {
        return port; // FIXME
    }

}
