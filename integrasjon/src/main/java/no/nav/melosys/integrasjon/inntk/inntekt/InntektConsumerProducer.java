package no.nav.melosys.integrasjon.inntk.inntekt;


import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.melosys.sikkerhet.sts.StsLogin;
import no.nav.tjeneste.virksomhet.inntekt.v3.binding.InntektV3;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class InntektConsumerProducer {

    private final InntektConsumerConfig config;
    private final StsLogin stsLogin;

    public InntektConsumerProducer(InntektConsumerConfig config, StsLogin stsLogin) {
        this.config = config;
        this.stsLogin = stsLogin;
    }

    @Bean
    InntektConsumer inntektConsumer() {
        InntektV3 port = wrapWithSts(config.getPort());
        return new InntektConsumerImpl(port);
    }

    @Bean
    InntektSelftestConsumer inntektSelftestConsumer() {
        InntektV3 port = wrapWithSts(config.getPort());
        return new InntektSelftestConsumerImpl(port, config.getEndpointUrl());
    }

    private InntektV3 wrapWithSts(InntektV3 port) {
        return StsConfigurationUtil.wrapWithSts(port, NAVSTSClient.StsClientType.SYSTEM_SAML, stsLogin);
    }

}
