package no.nav.melosys.integrasjon.doksys.dokumentproduksjon;

import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.melosys.sikkerhet.sts.StsLogin;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.DokumentproduksjonV3;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DokumentproduksjonConsumerProducer {

    private final DokumentproduksjonConsumerConfig config;
    private final StsLogin stsLogin;

    public DokumentproduksjonConsumerProducer(DokumentproduksjonConsumerConfig config, StsLogin stsLogin) {
        this.config = config;
        this.stsLogin = stsLogin;
    }

    @Bean
    @Primary
    public DokumentproduksjonConsumer dokumentproduksjonConsumer() {
        return new DokumentproduksjonConsumerAutoTokenAware(config, stsLogin);
    }

    @Bean
    @Qualifier("system")
    public DokumentproduksjonConsumer dokumentproduksjonSystemConsumer() {
        return new DokumentproduksjonConsumerAutoTokenAware(config, stsLogin);
    }

    @Bean
    public DokumentproduksjonSelftestConsumer dokumentproduksjonSelftestConsumer() {
        DokumentproduksjonV3 port = StsConfigurationUtil.wrapWithSts(config.getPort(), NAVSTSClient.StsClientType.SYSTEM_SAML, stsLogin);
        return new DokumentproduksjonSelftestConsumerImpl(port, config.getEndpointUrl());
    }
}
