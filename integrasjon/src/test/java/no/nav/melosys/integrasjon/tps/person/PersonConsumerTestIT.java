package no.nav.melosys.integrasjon.tps.person;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import no.nav.melosys.integrasjon.test.Gen3WsProxyServiceITBase;
import no.nav.melosys.integrasjon.test.TpsTestData;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.person.v3.binding.HentPersonSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
public class PersonConsumerTestIT extends Gen3WsProxyServiceITBase {

    @Autowired
    PersonConsumerConfig config;

    PersonConsumer personConsumer;

    PersonSelftestConsumer selftestConsumer;

    @Before
    public void setup() throws Exception {
        PersonConsumerProducer producer = new PersonConsumerProducer();
        producer.setConfig(config);

        personConsumer = producer.personConsumer();
        selftestConsumer = producer.personSelftestConsumer();
    }

    @Test
    public void hentPerson() throws HentPersonPersonIkkeFunnet, HentPersonSikkerhetsbegrensning {
        HentPersonRequest request = new HentPersonRequest();

        String ident = TpsTestData.STD_KVINNE_FNR;

        NorskIdent norskIdent = new NorskIdent();
        norskIdent.setIdent(ident);

        PersonIdent personIdent = new PersonIdent();
        personIdent.setIdent(norskIdent);

        request.setAktoer(personIdent);

        HentPersonResponse response = personConsumer.hentPerson(request);
        assertThat(response.getPerson().getPersonnavn().getEtternavn()).isEqualTo(TpsTestData.STD_KVINNE_ETTERNAVN);
    }

    public void xml() throws JAXBException, HentPersonPersonIkkeFunnet, HentPersonSikkerhetsbegrensning {
        JAXBContext jaxbContext = JAXBContext.newInstance(no.nav.tjeneste.virksomhet.person.v3.HentPerson.class, no.nav.tjeneste.virksomhet.person.v3.HentPersonResponse.class);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        HentPersonRequest request = new HentPersonRequest();

        String ident = TpsTestData.STD_KVINNE_FNR;

        NorskIdent norskIdent = new NorskIdent();
        norskIdent.setIdent(ident);

        PersonIdent personIdent = new PersonIdent();
        personIdent.setIdent(norskIdent);

        request.setAktoer(personIdent);

        request.getInformasjonsbehov().add(Informasjonsbehov.ADRESSE);

        HentPersonResponse response = personConsumer.hentPerson(request);

        no.nav.tjeneste.virksomhet.person.v3.HentPersonResponse xmlRootResponse = new no.nav.tjeneste.virksomhet.person.v3.HentPersonResponse();
        xmlRootResponse.setResponse(response);

        no.nav.tjeneste.virksomhet.person.v3.HentPerson xmlRootRequest = new no.nav.tjeneste.virksomhet.person.v3.HentPerson();
        xmlRootRequest.setRequest(request);

        marshaller.marshal(xmlRootRequest, System.out);
        marshaller.marshal(xmlRootResponse, System.out);
    }
}
