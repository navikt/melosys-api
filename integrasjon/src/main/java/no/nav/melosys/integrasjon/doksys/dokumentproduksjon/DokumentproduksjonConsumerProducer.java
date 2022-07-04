package no.nav.melosys.integrasjon.doksys.dokumentproduksjon;

import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.melosys.sikkerhet.sts.StsLoginConfig;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.DokumentproduksjonV3;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DokumentproduksjonConsumerProducer {

    private final DokumentproduksjonConsumerConfig config;
    private final StsLoginConfig stsLoginConfig;

    public DokumentproduksjonConsumerProducer(DokumentproduksjonConsumerConfig config, StsLoginConfig stsLoginConfig) {
        this.config = config;
        this.stsLoginConfig = stsLoginConfig;
    }

    @Bean
    public DokumentproduksjonConsumer dokumentproduksjonSystemConsumer() {
        return new DokumentproduksjonConsumerAutoTokenAware(config, stsLoginConfig);
    }
    @Bean
    public DokumentproduksjonSelftestConsumer dokumentproduksjonSelftestConsumer() {
        DokumentproduksjonV3 port = StsConfigurationUtil.wrapWithSts(config.getPort(), NAVSTSClient.StsClientType.SYSTEM_SAML, stsLoginConfig);
        return new DokumentproduksjonSelftestConsumerImpl(port, config.getEndpointUrl());
    }
}
