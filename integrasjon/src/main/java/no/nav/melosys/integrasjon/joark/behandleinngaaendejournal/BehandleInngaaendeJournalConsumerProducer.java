package no.nav.melosys.integrasjon.joark.behandleinngaaendejournal;

import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.tjeneste.virksomhet.behandleinngaaendejournal.v1.binding.BehandleInngaaendeJournalV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.melosys.sikkerhet.sts.NAVSTSClient.StsClientType.SECURITYCONTEXT_TIL_SAML;
import static no.nav.melosys.sikkerhet.sts.NAVSTSClient.StsClientType.SYSTEM_SAML;

@Configuration
public class BehandleInngaaendeJournalConsumerProducer {

    private BehandleInngaaendeJournalConsumerConfig consumerConfig;

    public BehandleInngaaendeJournalConsumerProducer(BehandleInngaaendeJournalConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    @Bean
    public BehandleInngaaendeJournalConsumer behandleInngaaendeJournalConsumer() {
        BehandleInngaaendeJournalV1 port = wrapWithSts(consumerConfig.getPort(), SECURITYCONTEXT_TIL_SAML);
        return new BehandleInngaaendeJournalConsumerImpl(port);
    }

    @Bean
    public BehandleInngaaendeJournalSelftestConsumer behandleInngaaendeJournalSelftestConsumer() {
        BehandleInngaaendeJournalV1 port = wrapWithSts(consumerConfig.getPort(), SYSTEM_SAML);
        return new BehandleInngaaendeJournalSelftestConsumerImpl(port, consumerConfig.getEndpointUrl());
    }

    BehandleInngaaendeJournalV1 wrapWithSts(BehandleInngaaendeJournalV1 port, NAVSTSClient.StsClientType samlTokenType) {
        return StsConfigurationUtil.wrapWithSts(port, samlTokenType);
    }
}
