package no.nav.melosys.tjenester.gui;

import java.io.Reader;
import java.io.StringReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import no.nav.tjeneste.virksomhet.person.v3.HentPersonResponse;

public class MockTjenesterSvarTps {

    public static no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse SVAR = null;

    MockTjenesterSvarTps() throws JAXBException {

        JAXBContext jaxbContext = JAXBContext.newInstance(HentPersonResponse.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        Reader reader = new StringReader(xml);
        Object xmlBean = unmarshaller.unmarshal(reader);
        HentPersonResponse response = (HentPersonResponse) xmlBean;
        SVAR = response.getResponse();

    }

    public static String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<ns2:hentPersonResponse xmlns:ns2=\"http://nav.no/tjeneste/virksomhet/person/v3\" xmlns:ns3=\"http://nav.no/tjeneste/virksomhet/person/v3/informasjon\">\n" +
            "    <response>\n" +
            "        <person xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"ns3:Bruker\">\n" +
            "            <bostedsadresse>\n" +
            "                <strukturertAdresse xsi:type=\"ns3:Gateadresse\">\n" +
            "                    <landkode>NOR</landkode>\n" +
            "                    <tilleggsadresseType>Offisiell adresse</tilleggsadresseType>\n" +
            "                    <poststed>5141</poststed>\n" +
            "                    <kommunenummer>1201</kommunenummer>\n" +
            "                    <gatenavn>EINERHAUGEN</gatenavn>\n" +
            "                    <husnummer>7</husnummer>\n" +
            "                    <husbokstav>A</husbokstav>\n" +
            "                </strukturertAdresse>\n" +
            "            </bostedsadresse>\n" +
            "            <sivilstand fomGyldighetsperiode=\"2010-08-09T00:00:00.000+02:00\">\n" +
            "                <sivilstand>UGIF</sivilstand>\n" +
            "            </sivilstand>\n" +
            "            <statsborgerskap>\n" +
            "                <land>SWE</land>\n" +
            "            </statsborgerskap>\n" +
            "            <aktoer xsi:type=\"ns3:PersonIdent\">\n" +
            "                <ident>\n" +
            "                    <ident>88888888884</ident>\n" +
            "                    <type>FNR</type>\n" +
            "                </ident>\n" +
            "            </aktoer>\n" +
            "            <kjoenn>\n" +
            "                <kjoenn>K</kjoenn>\n" +
            "            </kjoenn>\n" +
            "            <personnavn>\n" +
            "                <etternavn>NAVNESEN</etternavn>\n" +
            "                <fornavn>HANNA FORNAVN MADELEINE</fornavn>\n" +
            "                <sammensattNavn>NAVNESEN HANNA FORNAVN M</sammensattNavn>\n" +
            "            </personnavn>\n" +
            "            <personstatus>\n" +
            "                <personstatus>BOSA</personstatus>\n" +
            "            </personstatus>\n" +
            "            <foedselsdato>\n" +
            "                <foedselsdato>1983-04-10+02:00</foedselsdato>\n" +
            "            </foedselsdato>\n" +
            "            <gjeldendePostadressetype>BOSTEDSADRESSE</gjeldendePostadressetype>\n" +
            "            <geografiskTilknytning xsi:type=\"ns3:Bydel\">\n" +
            "                <geografiskTilknytning>120104</geografiskTilknytning>\n" +
            "            </geografiskTilknytning>\n" +
            "        </person>\n" +
            "    </response>\n" +
            "</ns2:hentPersonResponse>";

}
