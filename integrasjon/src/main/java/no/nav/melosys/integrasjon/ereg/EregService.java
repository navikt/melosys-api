package no.nav.melosys.integrasjon.ereg;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import no.nav.melosys.integrasjon.ereg.organisasjon.OrganisasjonConsumer;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonOrganisasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonUgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.Organisasjon;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonRequest;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonResponse;

@Service
public class EregService implements EregFasade {

    private static final Logger log = LoggerFactory.getLogger(EregService.class);

    private OrganisasjonConsumer organisasjonConsumer;

    @Autowired
    public EregService(OrganisasjonConsumer organisasjonConsumer) {
        this.organisasjonConsumer = organisasjonConsumer;
    }

    @Override
    public Organisasjon hentOrganisasjon(String orgnummer) throws HentOrganisasjonOrganisasjonIkkeFunnet, HentOrganisasjonUgyldigInput {
        HentOrganisasjonRequest request = new HentOrganisasjonRequest();
        request.setOrgnummer(orgnummer);

        HentOrganisasjonResponse response = organisasjonConsumer.hentOrganisasjon(request);

        return response.getOrganisasjon();
    }
}
