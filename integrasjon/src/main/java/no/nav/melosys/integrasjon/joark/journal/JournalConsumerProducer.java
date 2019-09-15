package no.nav.melosys.integrasjon.joark.journal;

import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.tjeneste.virksomhet.journal.v3.JournalV3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JournalConsumerProducer {
    private JournalConsumerConfig consumerConfig;

    @Autowired
    public void setConfig(JournalConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    @Bean
    @Primary
    public JournalConsumer journalConsumer() {
        JournalV3 port = wrapWithSts(consumerConfig.getPort(), NAVSTSClient.StsClientType.SECURITYCONTEXT_TIL_SAML);
        return new JournalConsumerImpl(port);
    }

    @Bean
    @Qualifier("system")
    public JournalConsumer journalConsumerSystem() {
        JournalV3 port = wrapWithSts(consumerConfig.getPort(), NAVSTSClient.StsClientType.SYSTEM_SAML);
        return new JournalConsumerImpl(port);
    }

    @Bean
    public JournalSelftestConsumer journalSelftestConsumer() {
        JournalV3 port = wrapWithSts(consumerConfig.getPort(), NAVSTSClient.StsClientType.SYSTEM_SAML);
        return new JournalSelftestConsumerImpl(port, consumerConfig.getEndpointUrl());
    }

    private JournalV3 wrapWithSts(JournalV3 port, NAVSTSClient.StsClientType samlTokenType) {
        return StsConfigurationUtil.wrapWithSts(port, samlTokenType);
    }
}
