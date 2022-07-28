package no.nav.melosys.integrasjon.doksys.dokumentproduksjon;

import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfig;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.DokumentproduksjonV3;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DokumentproduksjonConsumerProducer {

    private final DokumentproduksjonConsumerConfig config;
    private final StsConfig stsConfig;

    public DokumentproduksjonConsumerProducer(DokumentproduksjonConsumerConfig config, StsConfig stsConfig) {
        this.config = config;
        this.stsConfig = stsConfig;
    }

    @Bean
    public DokumentproduksjonConsumer dokumentproduksjonSystemConsumer() {
        return new DokumentproduksjonConsumerAutoTokenAware(config, stsConfig);
    }
    @Bean
    public DokumentproduksjonSelftestConsumer dokumentproduksjonSelftestConsumer() {
        DokumentproduksjonV3 port = stsConfig.wrapWithSts(config.getPort(), NAVSTSClient.StsClientType.SYSTEM_SAML);
        return new DokumentproduksjonSelftestConsumerImpl(port, config.getEndpointUrl());
    }
}
