package no.nav.melosys.integrasjon.ereg.organisasjon;


import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfig;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.OrganisasjonV4;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OrganisasjonConsumerProducer {
    private final OrganisasjonConsumerConfig config;
    private final StsConfig stsConfig;

    public OrganisasjonConsumerProducer(OrganisasjonConsumerConfig config, StsConfig stsConfig) {
        this.config = config;
        this.stsConfig = stsConfig;
    }

    @Bean
    OrganisasjonConsumer organisasjonConsumer() {
        return new OrganisasjonConsumerAutoTokenAware(config, stsConfig);
    }

    @Bean
    OrganisasjonSelftestConsumer organisasjonSelftestConsumer() {
        OrganisasjonV4 port = stsConfig.wrapWithSts(config.getPort(), NAVSTSClient.StsClientType.SYSTEM_SAML);
        return new OrganisasjonSelftestConsumerImpl(port, config.getEndpointUrl());
    }
}
