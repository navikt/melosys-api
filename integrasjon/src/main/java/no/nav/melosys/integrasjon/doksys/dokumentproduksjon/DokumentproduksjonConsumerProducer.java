package no.nav.melosys.integrasjon.doksys.dokumentproduksjon;

import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.tjeneste.virksomhet.dokumentproduksjon.v3.DokumentproduksjonV3;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DokumentproduksjonConsumerProducer {

    private DokumentproduksjonConsumerConfig config;

    public DokumentproduksjonConsumerProducer(DokumentproduksjonConsumerConfig config) {
        this.config = config;
    }

    @Bean
    @Primary
    public DokumentproduksjonConsumer dokumentproduksjonConsumer() {
        DokumentproduksjonV3 port = wrapWithSts(config.getPort(), NAVSTSClient.StsClientType.SECURITYCONTEXT_TIL_SAML);
        return new DokumentproduksjonConsumerImpl(port);
    }

    @Bean

    public DokumentproduksjonConsumer dokumentproduksjonSystemConsumer() {
        DokumentproduksjonV3 port = wrapWithSts(config.getPort(), NAVSTSClient.StsClientType.SYSTEM_SAML);
        return new DokumentproduksjonConsumerImpl(port);
    }

    @Bean
    public DokumentproduksjonSelftestConsumer dokumentproduksjonSelftestConsumer() {
        DokumentproduksjonV3 port = wrapWithSts(config.getPort(), NAVSTSClient.StsClientType.SYSTEM_SAML);
        return new DokumentproduksjonSelftestConsumerImpl(port, config.getEndpointUrl());
    }

    private DokumentproduksjonV3 wrapWithSts(DokumentproduksjonV3 port, NAVSTSClient.StsClientType oidcTilSaml) {
        return StsConfigurationUtil.wrapWithSts(port, oidcTilSaml);
    }
}
