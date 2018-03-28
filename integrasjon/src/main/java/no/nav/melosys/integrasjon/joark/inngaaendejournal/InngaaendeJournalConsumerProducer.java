package no.nav.melosys.integrasjon.joark.inngaaendejournal;


import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.InngaaendeJournalV1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import static no.nav.melosys.sikkerhet.sts.NAVSTSClient.StsClientType.SECURITYCONTEXT_TIL_SAML;
import static no.nav.melosys.sikkerhet.sts.NAVSTSClient.StsClientType.SYSTEM_SAML;


@Configuration
public class InngaaendeJournalConsumerProducer {

    private InngaaendeJournalConsumerConfig consumerConfig;

    @Autowired
    public InngaaendeJournalConsumerProducer(InngaaendeJournalConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    public InngaaendeJournalConsumer inngaaendeJournalConsumer() {
        InngaaendeJournalV1 port = wrapWithSts(consumerConfig.getPort(), SECURITYCONTEXT_TIL_SAML);
        return new InngaaendeJournalConsumerImpl(port);
    }

    public InngaaendeJournalSelftestConsumer inngaaendeJournalSelftestConsumer() {
        InngaaendeJournalV1 port = wrapWithSts(consumerConfig.getPort(), SYSTEM_SAML);
        return new InngaaendeJournalSelftestConsumerImpl(port, consumerConfig.getEndpointUrl());
    }

    InngaaendeJournalV1 wrapWithSts(InngaaendeJournalV1 port, NAVSTSClient.StsClientType samlTokenType) {
        return StsConfigurationUtil.wrapWithSts(port, samlTokenType);
    }
}
