package no.nav.melosys.integrasjon.inntk.inntekt;

import static no.nav.vedtak.sts.client.NAVSTSClient.StsClientType.OIDC_TIL_SAML;
import static no.nav.vedtak.sts.client.NAVSTSClient.StsClientType.SYSTEM_SAML;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3;
import no.nav.vedtak.sts.client.NAVSTSClient;
import no.nav.vedtak.sts.client.StsConfigurationUtil;

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
        InntektV3 port = wrapWithSts(config.getPort(), OIDC_TIL_SAML);
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
