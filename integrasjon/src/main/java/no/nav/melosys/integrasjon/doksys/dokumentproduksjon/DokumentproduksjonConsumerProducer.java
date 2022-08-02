package no.nav.melosys.integrasjon.doksys.dokumentproduksjon;

import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsWrapper;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.DokumentproduksjonV3;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DokumentproduksjonConsumerProducer {

    private final DokumentproduksjonConsumerConfig config;
    private final StsWrapper stsWrapper;

    public DokumentproduksjonConsumerProducer(DokumentproduksjonConsumerConfig config, StsWrapper stsWrapper) {
        this.config = config;
        this.stsWrapper = stsWrapper;
    }

    @Bean
    public DokumentproduksjonConsumer dokumentproduksjonSystemConsumer() {
        return new DokumentproduksjonConsumerAutoTokenAware(config, stsWrapper);
    }
    @Bean
    public DokumentproduksjonSelftestConsumer dokumentproduksjonSelftestConsumer() {
        DokumentproduksjonV3 port = stsWrapper.wrapWithSts(config.getPort(), NAVSTSClient.StsClientType.SYSTEM_SAML);
        return new DokumentproduksjonSelftestConsumerImpl(port, config.getEndpointUrl());
    }
}
