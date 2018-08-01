package no.nav.melosys.integrasjon.sakogbehandling.behandlingskjede;

import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.binding.SakOgBehandlingV1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.melosys.sikkerhet.sts.NAVSTSClient.StsClientType.SYSTEM_SAML;

@Configuration
public class BehandlingskjedeConsumerProducer {

    private BehandlingskjedeConsumerConfig config;

    @Autowired
    public void setConfig(BehandlingskjedeConsumerConfig config) {
        this.config = config;
    }

    @Bean
    BehandlingskjedeConsumer behandlingskjedeConsumer() {
        SakOgBehandlingV1 port = wrapWithSts(config.getPort(), SYSTEM_SAML);
        return new BehandlingskjedeConsumerImpl(port);
    }

    @Bean
    BehandlingskjedeSelftestConsumer behandlingskjedeSelftestConsumer() {
        SakOgBehandlingV1 port = wrapWithSts(config.getPort(), SYSTEM_SAML);
        return new BehandlingskjedeSelftestConsumerImpl(port, config.getEndpointUrl());
    }

    SakOgBehandlingV1 wrapWithSts(SakOgBehandlingV1 port, NAVSTSClient.StsClientType samlTokenType) {
        return StsConfigurationUtil.wrapWithSts(port, samlTokenType);
    }
}
