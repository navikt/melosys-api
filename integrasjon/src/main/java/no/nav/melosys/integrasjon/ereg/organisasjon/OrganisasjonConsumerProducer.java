package no.nav.melosys.integrasjon.ereg.organisasjon;


import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.melosys.sikkerhet.sts.StsLoginConfig;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.OrganisasjonV4;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OrganisasjonConsumerProducer {
    private final OrganisasjonConsumerConfig config;
    private final StsLoginConfig stsLoginConfig;

    public OrganisasjonConsumerProducer(OrganisasjonConsumerConfig config, StsLoginConfig stsLoginConfig) {
        this.config = config;
        this.stsLoginConfig = stsLoginConfig;
    }

    @Bean
    OrganisasjonConsumer organisasjonConsumer() {
        return new OrganisasjonConsumerAutoTokenAware(config, stsLoginConfig);
    }

    @Bean
    OrganisasjonSelftestConsumer organisasjonSelftestConsumer() {
        OrganisasjonV4 port = StsConfigurationUtil.wrapWithSts(config.getPort(), NAVSTSClient.StsClientType.SYSTEM_SAML, stsLoginConfig);
        return new OrganisasjonSelftestConsumerImpl(port, config.getEndpointUrl());
    }
}
