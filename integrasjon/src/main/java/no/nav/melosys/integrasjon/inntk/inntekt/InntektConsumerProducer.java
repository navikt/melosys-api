package no.nav.melosys.integrasjon.inntk.inntekt;


import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static no.nav.melosys.sikkerhet.sts.NAVSTSClient.StsClientType.SYSTEM_SAML;


@Configuration
public class InntektConsumerProducer {

    private InntektConsumerConfig config;

    public InntektConsumerProducer(InntektConsumerConfig config) {
        this.config = config;
    }

    @Bean
    InntektConsumer inntektConsumer() {
        InntektV3 port = wrapWithSts(config.getPort(), SYSTEM_SAML);
        return new InntektConsumerImpl(port);
    }

    @Bean
    InntektSelftestConsumer inntektSelftestConsumer() {
        InntektV3 port = wrapWithSts(config.getPort(), SYSTEM_SAML);
        return new InntektSelftestConsumerImpl(port, config.getEndpointUrl());
    }

    private InntektV3 wrapWithSts(InntektV3 port, NAVSTSClient.StsClientType oidcTilSaml) {
        return StsConfigurationUtil.wrapWithSts(port, oidcTilSaml);
    }

}
