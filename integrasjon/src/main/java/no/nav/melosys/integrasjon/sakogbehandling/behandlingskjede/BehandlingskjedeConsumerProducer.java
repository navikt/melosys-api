package no.nav.melosys.integrasjon.sakogbehandling.behandlingskjede;

import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsWrapper;
import no.nav.tjeneste.virksomhet.sakogbehandling.v1.binding.SakOgBehandlingV1;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BehandlingskjedeConsumerProducer {

    private final BehandlingskjedeConsumerConfig config;
    private final StsWrapper stsWrapper;

    public BehandlingskjedeConsumerProducer(BehandlingskjedeConsumerConfig config, StsWrapper stsWrapper) {
        this.config = config;
        this.stsWrapper = stsWrapper;
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
        return stsWrapper.wrapWithSts(port, NAVSTSClient.StsClientType.SYSTEM_SAML);
    }
}
