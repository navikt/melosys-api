package no.nav.melosys.integrasjon.sakogbehandling.behandlingskjede;

import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfig;
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.binding.SakOgBehandlingV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BehandlingskjedeConsumerProducer {

    private final BehandlingskjedeConsumerConfig config;
    private final StsConfig stsConfig;

    public BehandlingskjedeConsumerProducer(BehandlingskjedeConsumerConfig config, StsConfig stsConfig) {
        this.config = config;
        this.stsConfig = stsConfig;
    }

    @Bean
    BehandlingskjedeConsumer behandlingskjedeConsumer() {
        SakOgBehandlingV1 port = wrapWithSts(config.getPort());
        return new BehandlingskjedeConsumerImpl(port);
    }

    @Bean
    BehandlingskjedeSelftestConsumer behandlingskjedeSelftestConsumer() {
        SakOgBehandlingV1 port = wrapWithSts(config.getPort());
        return new BehandlingskjedeSelftestConsumerImpl(port, config.getEndpointUrl());
    }

    SakOgBehandlingV1 wrapWithSts(SakOgBehandlingV1 port) {
        return stsConfig.wrapWithSts(port, NAVSTSClient.StsClientType.SYSTEM_SAML);
    }
}
