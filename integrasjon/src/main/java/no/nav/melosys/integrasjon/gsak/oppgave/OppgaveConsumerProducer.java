package no.nav.melosys.integrasjon.gsak.oppgave;

import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.tjeneste.virksomhet.oppgave.v3.binding.OppgaveV3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.melosys.sikkerhet.sts.NAVSTSClient.StsClientType.SECURITYCONTEXT_TIL_SAML;
import static no.nav.melosys.sikkerhet.sts.NAVSTSClient.StsClientType.SYSTEM_SAML;

@Configuration
public class OppgaveConsumerProducer {
    private OppgaveConsumerConfig consumerConfig;

    @Autowired
    public void setConfig(OppgaveConsumerConfig consumerConfig) {
        this.consumerConfig = consumerConfig;
    }

    @Bean
    OppgaveConsumer oppgaveConsumer() {
        OppgaveV3 port = wrapWithSts(consumerConfig.getPort(), SECURITYCONTEXT_TIL_SAML);
        return new OppgaveConsumerImpl(port);
    }

    @Bean
    OppgaveSelftestConsumer oppgaveSelftestConsumer() {
        OppgaveV3 port = wrapWithSts(consumerConfig.getPort(), SYSTEM_SAML);
        return new OppgaveSelftestConsumerImpl(port, consumerConfig.getEndpointUrl());
    }

    private OppgaveV3 wrapWithSts(OppgaveV3 port, NAVSTSClient.StsClientType samlTokenType) {
        return StsConfigurationUtil.wrapWithSts(port, samlTokenType);
    }
}
