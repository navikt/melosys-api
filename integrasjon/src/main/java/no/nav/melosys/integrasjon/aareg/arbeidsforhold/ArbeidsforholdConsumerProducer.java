package no.nav.melosys.integrasjon.aareg.arbeidsforhold;


import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static no.nav.melosys.sikkerhet.sts.NAVSTSClient.StsClientType.SECURITYCONTEXT_TIL_SAML;
import static no.nav.melosys.sikkerhet.sts.NAVSTSClient.StsClientType.SYSTEM_SAML;


@Configuration
public class ArbeidsforholdConsumerProducer {
    private ArbeidsforholdConsumerConfig config;

    @Autowired
    public void setConfig(ArbeidsforholdConsumerConfig config) {
        this.config = config;
    }

    @Bean
    @Profile("utvikling")
    ArbeidsforholdConsumer arbeidsforholdMock() {
        return new ArbeidsforholdMock();
    }

    @Bean
    @Profile("!utvikling")
    ArbeidsforholdConsumer arbeidsforholdConsumer() {
        ArbeidsforholdV3 port = wrapWithSts(config.getPort(), SECURITYCONTEXT_TIL_SAML);
        return new ArbeidsforholdConsumerImpl(port);
    }

    @Bean
    ArbeidsforholdSelftestConsumer arbeidsforholdSelftestConsumer() {
        ArbeidsforholdV3 port = wrapWithSts(config.getPort(), SYSTEM_SAML);
        return new ArbeidsforholdSelftestConsumerImpl(port, config.getEndpointUrl());
    }

    private ArbeidsforholdV3 wrapWithSts(ArbeidsforholdV3 port, NAVSTSClient.StsClientType
            oidcTilSaml) {
        return StsConfigurationUtil.wrapWithSts(port, oidcTilSaml);
    }
}
