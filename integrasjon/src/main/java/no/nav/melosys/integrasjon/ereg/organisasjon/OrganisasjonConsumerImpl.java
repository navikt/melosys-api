package no.nav.melosys.integrasjon.ereg.organisasjon;

import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonOrganisasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonUgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.OrganisasjonV4;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonRequest;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonResponse;

public class OrganisasjonConsumerImpl implements OrganisasjonConsumer {

    private OrganisasjonV4 port;

    public OrganisasjonConsumerImpl(OrganisasjonV4 port) {
        this.port = port;
    }

    @Override
    public HentOrganisasjonResponse hentOrganisasjon(HentOrganisasjonRequest request) throws HentOrganisasjonOrganisasjonIkkeFunnet, HentOrganisasjonUgyldigInput {
        return port.hentOrganisasjon(request);
    }
}
