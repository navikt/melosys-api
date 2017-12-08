package no.nav.melosys.integrasjon.inntk.inntekt;


import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static no.nav.melosys.sikkerhet.sts.NAVSTSClient.StsClientType.SECURITYCONTEXT_TIL_SAML;
import static no.nav.melosys.sikkerhet.sts.NAVSTSClient.StsClientType.SYSTEM_SAML;


@Configuration
public class InntektConsumerProducer {

    private InntektConsumerConfig config;

    @Autowired
    public InntektConsumerProducer(InntektConsumerConfig config) {
        this.config = config;
    }

    @Bean
    @Profile("utvikling")
    InntektConsumer inntektMock() {
        return new InntektMock();
    }

    @Bean
    @Profile("!utvikling")
    InntektConsumer inntektConsumer() {
        InntektV3 port = wrapWithSts(config.getPort(), SECURITYCONTEXT_TIL_SAML);
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
