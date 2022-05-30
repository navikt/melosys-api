package no.nav.melosys.integrasjon.ereg.organisasjon;


import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.melosys.sikkerhet.sts.StsLogin;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.OrganisasjonV4;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Configuration
public class OrganisasjonConsumerProducer {
    private final OrganisasjonConsumerConfig config;
    private final StsLogin stsLogin;

    public OrganisasjonConsumerProducer(OrganisasjonConsumerConfig config, StsLogin stsLogin) {
        this.config = config;
        this.stsLogin = stsLogin;
    }

    @Bean
    @Primary
    OrganisasjonConsumer organisasjonConsumer() {
        return new OrganisasjonConsumerAutoTokenAware(config, stsLogin);
    }

    @Bean
    @Qualifier("system")
    OrganisasjonConsumer organisasjonSystemConsumer() {
        return new OrganisasjonConsumerAutoTokenAware(config, stsLogin);
    }

    @Bean
    OrganisasjonSelftestConsumer organisasjonSelftestConsumer() {
        OrganisasjonV4 port = StsConfigurationUtil.wrapWithSts(config.getPort(), NAVSTSClient.StsClientType.SYSTEM_SAML, stsLogin);
        return new OrganisasjonSelftestConsumerImpl(port, config.getEndpointUrl());
    }
}
