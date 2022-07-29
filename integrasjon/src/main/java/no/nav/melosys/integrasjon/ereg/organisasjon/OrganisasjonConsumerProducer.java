package no.nav.melosys.integrasjon.ereg.organisasjon;


import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsWrapper;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.OrganisasjonV4;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OrganisasjonConsumerProducer {
    private final OrganisasjonConsumerConfig config;
    private final StsWrapper stsWrapper;

    public OrganisasjonConsumerProducer(OrganisasjonConsumerConfig config, StsWrapper stsWrapper) {
        this.config = config;
        this.stsWrapper = stsWrapper;
    }

    @Bean
    OrganisasjonConsumer organisasjonConsumer() {
        return new OrganisasjonConsumerAutoTokenAware(config, stsWrapper);
    }

    @Bean
    OrganisasjonSelftestConsumer organisasjonSelftestConsumer() {
        OrganisasjonV4 port = stsWrapper.wrapWithSts(config.getPort(), NAVSTSClient.StsClientType.SYSTEM_SAML);
        return new OrganisasjonSelftestConsumerImpl(port, config.getEndpointUrl());
    }
}
