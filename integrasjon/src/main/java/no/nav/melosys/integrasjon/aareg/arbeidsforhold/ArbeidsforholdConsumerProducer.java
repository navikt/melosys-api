package no.nav.melosys.integrasjon.aareg.arbeidsforhold;


import static no.nav.vedtak.sts.client.NAVSTSClient.StsClientType.OIDC_TIL_SAML;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3;
import no.nav.vedtak.sts.client.NAVSTSClient;
import no.nav.vedtak.sts.client.StsConfigurationUtil;

@Configuration
public class ArbeidsforholdConsumerProducer {
    private ArbeidsforholdConsumerConfig config;

    @Autowired
    public void setConfig(ArbeidsforholdConsumerConfig config) {
        this.config = config;
    }

    @Bean
    ArbeidsforholdConsumer arbeidsforholdConsumer() {
        ArbeidsforholdV3 port = wrapWithSts(config.getPort(), OIDC_TIL_SAML);
        return new ArbeidsforholdConsumerImpl(port);
    }

    private ArbeidsforholdV3 wrapWithSts(ArbeidsforholdV3 port, NAVSTSClient.StsClientType
            oidcTilSaml) {
        return StsConfigurationUtil.wrapWithSts(port, oidcTilSaml);
    }
}
