package no.nav.melosys.integrasjon.medl.medlemskap;

import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.tjeneste.virksomhet.medlemskap.v2.MedlemskapV2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static no.nav.melosys.sikkerhet.sts.NAVSTSClient.StsClientType.SECURITYCONTEXT_TIL_SAML;


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
        MedlemskapV2 port = wrapWithSts(config.getPort(), SECURITYCONTEXT_TIL_SAML);
        return new MedlemskapConsumerImpl(port);
    }

    private MedlemskapV2 wrapWithSts(MedlemskapV2 port, NAVSTSClient.StsClientType samlTokenType) {
        if (port != null)
            return StsConfigurationUtil.wrapWithSts(port, samlTokenType);
        return null;
    }
}
