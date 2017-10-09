package no.nav.melosys.integrasjon.medl2.medlemskap;

import no.nav.tjeneste.virksomhet.medlemskap.v2.MedlemskapV2;
import no.nav.vedtak.sts.client.NAVSTSClient;
import no.nav.vedtak.sts.client.StsConfigurationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static no.nav.vedtak.sts.client.NAVSTSClient.StsClientType.OIDC_TIL_SAML;

@Configuration
public class MedlemskapConsumerProducer {

    private MedlemskapConsumerConfig config;

    @Autowired
    public void setConfig(MedlemskapConsumerConfig config) {
        this.config = config;
    }

    @Bean
    @Profile("utvikling")
    MedlemskapConsumer medlemskapMock() {
        return new MedlemskapMock();
    }

    @Bean
    @Profile("!utvikling")
    MedlemskapConsumer medlemskapConsumer() {
        // This will fail, since there's no SAML.
        //return new MedlemskapConsumerImpl(config.getPort());
        MedlemskapV2 port = wrapWithSts(config.getPort(), OIDC_TIL_SAML);
        return new MedlemskapConsumerImpl(port);
    }

    private MedlemskapV2 wrapWithSts(MedlemskapV2 port, NAVSTSClient.StsClientType samlTokenType) {
        if (port != null)
            return StsConfigurationUtil.wrapWithSts(port, samlTokenType);
        return null;
    }
}
