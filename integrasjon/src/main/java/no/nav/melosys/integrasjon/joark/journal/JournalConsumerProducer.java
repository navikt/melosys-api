package no.nav.melosys.integrasjon.joark.journal;

import static no.nav.vedtak.sts.client.NAVSTSClient.StsClientType.OIDC_TIL_SAML;
import static no.nav.vedtak.sts.client.NAVSTSClient.StsClientType.SYSTEM_SAML;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import no.nav.tjeneste.virksomhet.journal.v2.binding.JournalV2;
import no.nav.vedtak.sts.client.NAVSTSClient;
import no.nav.vedtak.sts.client.StsConfigurationUtil;

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

    JournalV2 wrapWithSts(JournalV2 port, NAVSTSClient.StsClientType samlTokenType) {
        return StsConfigurationUtil.wrapWithSts(port, samlTokenType);
    }

}
