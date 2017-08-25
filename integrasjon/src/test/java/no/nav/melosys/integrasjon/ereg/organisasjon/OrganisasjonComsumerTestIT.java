package no.nav.melosys.integrasjon.ereg.organisasjon;

import static org.assertj.core.api.Assertions.assertThat;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import no.nav.melosys.integrasjon.test.Gen3WsProxyServiceITBase;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonOrganisasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonUgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonRequest;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonResponse;

@RunWith(SpringRunner.class)
public class OrganisasjonComsumerTestIT extends Gen3WsProxyServiceITBase {

    @Autowired
    OrganisasjonConsumerConfig config;

    OrganisasjonConsumer consumer;

    OrganisasjonSelftestConsumer selftestConsumer;

    @Before
    public void setUp() {
        OrganisasjonConsumerProducer producer = new OrganisasjonConsumerProducer();
        producer.setConfig(config);

        consumer = producer.organisasjonConsumer();
        selftestConsumer = producer.organisasjonSelftestConsumer();
    }

    @Test
    public void hentOrganisasjon() throws HentOrganisasjonOrganisasjonIkkeFunnet, HentOrganisasjonUgyldigInput {
        HentOrganisasjonRequest request = new HentOrganisasjonRequest();
        request.setOrgnummer("974600951");

        HentOrganisasjonResponse response = consumer.hentOrganisasjon(request);
        String navn = response.getOrganisasjon().getOrganisasjonDetaljer().getNavn().get(0).getRedigertNavn();
        assertThat(navn).isEqualTo("VANN- OG AVLØPSETATEN");
    }

    @Test
    public void xml() throws JAXBException, HentOrganisasjonOrganisasjonIkkeFunnet, HentOrganisasjonUgyldigInput {
        JAXBContext  jaxbContext = JAXBContext.newInstance(no.nav.tjeneste.virksomhet.organisasjon.v4.HentOrganisasjonResponse.class);
        Marshaller marshaller = jaxbContext.createMarshaller();

        HentOrganisasjonRequest request = new HentOrganisasjonRequest();
        request.setOrgnummer("974600951");

        HentOrganisasjonResponse response = consumer.hentOrganisasjon(request);

        no.nav.tjeneste.virksomhet.organisasjon.v4.HentOrganisasjonResponse xmlRoot = new no.nav.tjeneste.virksomhet.organisasjon.v4.HentOrganisasjonResponse();
        xmlRoot.setResponse(response);
        marshaller.marshal(xmlRoot, System.out);

    }

}
