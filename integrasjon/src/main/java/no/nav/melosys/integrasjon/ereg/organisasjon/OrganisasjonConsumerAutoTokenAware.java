package no.nav.melosys.integrasjon.ereg.organisasjon;

import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import no.nav.melosys.sikkerhet.sts.NAVSTSClient;
import no.nav.melosys.sikkerhet.sts.StsWrapper;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonOrganisasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonUgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.OrganisasjonV4;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonRequest;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonResponse;

public class OrganisasjonConsumerAutoTokenAware implements OrganisasjonConsumer {

    private final StsWrapper stsWrapper;
    private final OrganisasjonV4 systemPort;
    private final OrganisasjonV4 saksbehandlerPort;

    public OrganisasjonConsumerAutoTokenAware(OrganisasjonConsumerConfig config, StsWrapper stsWrapper) {
        this.stsWrapper = stsWrapper;
        saksbehandlerPort = wrapWithSts(config.getPort(), NAVSTSClient.StsClientType.SECURITYCONTEXT_TIL_SAML);
        systemPort = wrapWithSts(config.getPort(), NAVSTSClient.StsClientType.SYSTEM_SAML);
    }

    @Override
    public HentOrganisasjonResponse hentOrganisasjon(HentOrganisasjonRequest request) throws HentOrganisasjonOrganisasjonIkkeFunnet, HentOrganisasjonUgyldigInput {
        if (ThreadLocalAccessInfo.shouldUseSystemToken()) {
            return systemPort.hentOrganisasjon(request);
        }
        return saksbehandlerPort.hentOrganisasjon(request);
    }

    private OrganisasjonV4 wrapWithSts(OrganisasjonV4 port, NAVSTSClient.StsClientType oidcTilSaml) {
        return stsWrapper.wrapWithSts(port, oidcTilSaml);
    }
}
