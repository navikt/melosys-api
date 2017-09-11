package no.nav.melosys.integrasjon.ereg.organisasjon;

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import no.nav.tjeneste.virksomhet.organisasjon.v4.HentOrganisasjonResponse;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonOrganisasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonUgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjon.v4.feil.OrganisasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonRequest;

public class OrganisasjonMock implements OrganisasjonConsumer {

    @Override
    public no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonResponse hentOrganisasjon(HentOrganisasjonRequest request) throws HentOrganisasjonOrganisasjonIkkeFunnet, HentOrganisasjonUgyldigInput {
        String orgnummer = request.getOrgnummer();

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(HentOrganisasjonResponse.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            InputStream is = getClass().getClassLoader().getResourceAsStream("mock/organisasjon/" + orgnummer + ".xml");
            if (is == null) {
                throw new HentOrganisasjonOrganisasjonIkkeFunnet("Organisasjon med " + orgnummer + "ikke funnet.", new OrganisasjonIkkeFunnet());
            }
            Object xmlBean = unmarshaller.unmarshal(is);
            return ((HentOrganisasjonResponse) xmlBean).getResponse();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }

    }
}
