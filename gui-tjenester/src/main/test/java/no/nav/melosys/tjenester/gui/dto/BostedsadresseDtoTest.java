package no.nav.melosys.tjenester.gui.dto;

import static org.junit.Assert.assertEquals;

import java.io.Reader;
import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;

import no.nav.tjeneste.virksomhet.person.v3.HentPersonResponse;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.StedsadresseNorge;

public class BostedsadresseDtoTest {

    @Test
    public void tilDto() throws Exception {

        String xml ="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<ns2:hentPersonResponse xmlns:ns2=\"http://nav.no/tjeneste/virksomhet/person/v3\" xmlns:ns3=\"http://nav.no/tjeneste/virksomhet/person/v3/informasjon\">\n" +
                "    <response>\n" +
                "        <person xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"ns3:Bruker\">\n" +
                "            <bostedsadresse>\n" +
                "                <strukturertAdresse xsi:type=\"ns3:Gateadresse\">\n" +
                "                    <landkode>NOR</landkode>\n" +
                "                    <tilleggsadresseType>Offisiell adresse</tilleggsadresseType>\n" +
                "                    <poststed>0594</poststed>\n" +
                "                    <bolignummer>H1002</bolignummer>\n" +
                "                    <kommunenummer>0301</kommunenummer>\n" +
                "                    <gatenavn>ERICH MOGENSØNS VEI</gatenavn>\n" +
                "                    <husnummer>28</husnummer>\n" +
                "                </strukturertAdresse>\n" +
                "            </bostedsadresse>\n" +
                "            <sivilstand fomGyldighetsperiode=\"2010-01-30T00:00:00.000+01:00\">\n" +
                "                <sivilstand>GIFT</sivilstand>\n" +
                "            </sivilstand>\n" +
                "            <statsborgerskap>\n" +
                "                <land>NOR</land>\n" +
                "            </statsborgerskap>\n" +
                "            <aktoer xsi:type=\"ns3:PersonIdent\">\n" +
                "                <ident>\n" +
                "                    <ident>88888888888</ident>\n" +
                "                    <type>FNR</type>\n" +
                "                </ident>\n" +
                "            </aktoer>\n" +
                "            <kjoenn>\n" +
                "                <kjoenn>K</kjoenn>\n" +
                "            </kjoenn>\n" +
                "            <personnavn>\n" +
                "                <etternavn>NAVNESEN</etternavn>\n" +
                "                <fornavn>FORNAVN LOURDES</fornavn>\n" +
                "                <mellomnavn>NOE</mellomnavn>\n" +
                "                <sammensattNavn>NAVNESEN FORNAVN L NOE</sammensattNavn>\n" +
                "            </personnavn>\n" +
                "            <personstatus>\n" +
                "                <personstatus>BOSA</personstatus>\n" +
                "            </personstatus>\n" +
                "            <foedselsdato>\n" +
                "                <foedselsdato>1969-01-06+01:00</foedselsdato>\n" +
                "            </foedselsdato>\n" +
                "            <gjeldendePostadressetype>BOSTEDSADRESSE</gjeldendePostadressetype>\n" +
                "            <geografiskTilknytning xsi:type=\"ns3:Bydel\">\n" +
                "                <geografiskTilknytning>030109</geografiskTilknytning>\n" +
                "            </geografiskTilknytning>\n" +
                "        </person>\n" +
                "    </response>\n" +
                "</ns2:hentPersonResponse>";

        JAXBContext jaxbContext = JAXBContext.newInstance(HentPersonResponse.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        Reader reader = new StringReader(xml);
        Object xmlBean = unmarshaller.unmarshal(reader);
        HentPersonResponse response = (HentPersonResponse) xmlBean;

        Person person = response.getResponse().getPerson();
        PersonDto personDto = PersonDto.tilDto(person);

        if (person.getBostedsadresse().getStrukturertAdresse() instanceof StedsadresseNorge) {
            StedsadresseNorge adresseNorge = (StedsadresseNorge) person.getBostedsadresse().getStrukturertAdresse();
            assertEquals(personDto.getBostedsadresse().getPostnr(), adresseNorge.getPoststed().getValue());
            //TODO Poststed krever kodeverk
            
        }



    }



}