package no.nav.melosys.integrasjon.ereg.organisasjon;

import static no.nav.vedtak.sts.client.NAVSTSClient.StsClientType.OIDC_TIL_SAML;
import static no.nav.vedtak.sts.client.NAVSTSClient.StsClientType.SYSTEM_SAML;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.OrganisasjonV4;
import no.nav.vedtak.sts.client.NAVSTSClient;
import no.nav.vedtak.sts.client.StsConfigurationUtil;

@Configuration
public class OrganisasjonConsumerProducer {
    private OrganisasjonConsumerConfig config;

    @Autowired
    public void setConfig(OrganisasjonConsumerConfig config) {
        this.config = config;
    }

    @Bean
    OrganisasjonConsumer organisasjonConsumer() {
        OrganisasjonV4 port = wrapWithSts(config.getPort(), OIDC_TIL_SAML);
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
