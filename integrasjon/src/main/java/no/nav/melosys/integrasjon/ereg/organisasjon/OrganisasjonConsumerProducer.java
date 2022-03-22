package no.nav.melosys.integrasjon.ereg.organisasjon;


import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsConfigurationUtil;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.OrganisasjonV4;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static no.nav.melosys.sikkerhet.sts.NAVSTSClient.StsClientType.SECURITYCONTEXT_TIL_SAML;
import static no.nav.melosys.sikkerhet.sts.NAVSTSClient.StsClientType.SYSTEM_SAML;


@Configuration
public class OrganisasjonConsumerProducer {
    private OrganisasjonConsumerConfig config;

    public OrganisasjonConsumerProducer(OrganisasjonConsumerConfig config) {
        this.config = config;
    }

    @Bean
    @Primary
    OrganisasjonConsumer organisasjonConsumer() {
        OrganisasjonV4 port = wrapWithSts(config.getPort(), SECURITYCONTEXT_TIL_SAML);
        return new OrganisasjonConsumerImpl(port);
    }

    @Bean
    @Qualifier("system")
    OrganisasjonConsumer organisasjonSystemConsumer() {
        OrganisasjonV4 port = wrapWithSts(config.getPort(), SYSTEM_SAML);
        return new OrganisasjonConsumerImpl(port);
    }

    @Bean
    OrganisasjonSelftestConsumer organisasjonSelftestConsumer() {
        OrganisasjonV4 port = wrapWithSts(config.getPort(), SYSTEM_SAML);
        return new OrganisasjonSelftestConsumerImpl(port, config.getEndpointUrl());
    }

    private OrganisasjonV4 wrapWithSts(OrganisasjonV4 port, NAVSTSClient.StsClientType oidcTilSaml) {
        return StsConfigurationUtil.wrapWithSts(port, oidcTilSaml);
    }
}
