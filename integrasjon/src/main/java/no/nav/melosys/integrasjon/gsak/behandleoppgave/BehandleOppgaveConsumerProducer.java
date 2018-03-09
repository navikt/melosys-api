package no.nav.melosys.integrasjon.gsak.behandleoppgave;

import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.tjeneste.virksomhet.behandleoppgave.v1.BehandleOppgaveV1;
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BehandleOppgaveConsumerProducer {

    private BehandleOppgaveConsumerConfig config;

    @Autowired
    public void setConfig(BehandleOppgaveConsumerConfig config) {
        this.config = config;
    }

    @Bean
    public BehandleOppgaveConsumer behandleOppgaveConsumer() {
        BehandleOppgaveV1 port = wrapWithSts(config.getPort(), NAVSTSClient.StsClientType.SYSTEM_SAML);
        return new BehandleOppgaveConsumerImpl(port);
    }

    @Bean
    public BehandleOppgaveSelftestConsumer behandleOppgaveSelftestConsumer() {
        BehandleOppgaveV1 port = config.getPort();
        return new BehandleOppgaveSelftestConsumerImpl(port, config.getEndpointUrl());
    }

    private BehandleOppgaveV1 wrapWithSts(BehandleOppgaveV1 port, NAVSTSClient.StsClientType oidcTilSaml) {
        return StsConfigurationUtil.wrapWithSts(port, oidcTilSaml);
    }
}
