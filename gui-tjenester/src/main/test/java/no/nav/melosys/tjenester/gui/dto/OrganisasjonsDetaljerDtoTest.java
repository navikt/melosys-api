package no.nav.melosys.tjenester.gui.dto;

import static org.junit.Assert.assertEquals;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;

import no.nav.tjeneste.virksomhet.organisasjon.v4.HentOrganisasjonResponse;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.GeografiskAdresse;

public class OrganisasjonsDetaljerDtoTest {


    @Test
    public void tilDto() throws Exception {
        String orgXml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns2:hentOrganisasjonResponse xmlns:ns2=\"http://nav.no/tjeneste/virksomhet/organisasjon/v4\">" +
                "<response><organisasjon xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:ns4=\"http://nav.no/tjeneste/virksomhet/organisasjon/v4/informasjon\" xsi:type=\"ns4:Virksomhet\"><orgnummer>974600951</orgnummer><navn xsi:type=\"ns4:UstrukturertNavn\">" +
                "<navnelinje>VANN- OG AVLØPSETATEN</navnelinje><navnelinje></navnelinje><navnelinje></navnelinje><navnelinje></navnelinje><navnelinje></navnelinje></navn>" +
                "<organisasjonDetaljer><registreringsDato>1995-08-10+02:00</registreringsDato><datoSistEndret>2013-12-18+01:00</datoSistEndret>" +
                "<telefaks fomGyldighetsperiode=\"2013-12-18T00:00:00.000+01:00\" fomBruksperiode=\"2014-05-21T20:19:49+02:00\"><identifikator>55 56 65 99</identifikator></telefaks>" +
                "<telefon fomGyldighetsperiode=\"2013-12-18T00:00:00.000+01:00\" fomBruksperiode=\"2014-05-21T20:19:49+02:00\"><identifikator>55 56 60 00</identifikator></telefon>" +
                "<forretningsadresse xsi:type=\"ns4:SemistrukturertAdresse\" fomGyldighetsperiode=\"2006-08-16T00:00:00.000+02:00\" fomBruksperiode=\"2015-02-23T10:38:34.403+01:00\">" +
                "<landkode kodeRef=\"NO\"></landkode><adresseledd><noekkel kodeRef=\"adresselinje1\"></noekkel>" +
                "<verdi>Fjøsangerveien 68</verdi></adresseledd><adresseledd><noekkel kodeRef=\"postnr\"></noekkel><verdi>5068</verdi></adresseledd><adresseledd><noekkel kodeRef=\"kommunenr\"></noekkel><verdi>1201</verdi></adresseledd></forretningsadresse>" +
                "<postadresse xsi:type=\"ns4:SemistrukturertAdresse\" fomGyldighetsperiode=\"2006-09-08T00:00:00.000+02:00\" fomBruksperiode=\"2015-02-23T10:38:34.403+01:00\"><landkode kodeRef=\"NO\"></landkode><adresseledd><noekkel kodeRef=\"adresselinje1\"></noekkel><verdi>Postboks 7700</verdi></adresseledd><adresseledd>" +
                "<noekkel kodeRef=\"postnr\"></noekkel><verdi>5020</verdi></adresseledd><adresseledd><noekkel kodeRef=\"kommunenr\"></noekkel><verdi>1201</verdi></adresseledd></postadresse><navSpesifikkInformasjon fomGyldighetsperiode=\"2014-10-13T13:58:44+02:00\" fomBruksperiode=\"2014-10-13T13:58:44+02:00\"><erIA>true</erIA>" +
                "</navSpesifikkInformasjon><internettadresse fomGyldighetsperiode=\"2013-12-18T00:00:00.000+01:00\" fomBruksperiode=\"2014-05-21T20:19:49+02:00\"><identifikator>www.bergenvann.no</identifikator></internettadresse>" +
                "<epostadresse fomGyldighetsperiode=\"2013-12-18T00:00:00.000+01:00\" fomBruksperiode=\"2014-05-21T20:19:49+02:00\">" +
                "<identifikator>Va-kundeservice@bergen.kommune.no</identifikator></epostadresse><naering fomBruksperiode=\"2014-05-22T00:58:08+02:00\" " +
                "fomGyldighetsperiode=\"2000-11-14T00:00:00.000+01:00\"><naeringskode kodeRef=\"36.000\"></naeringskode><hjelpeenhet>false</hjelpeenhet></naering>" +
                "<navn fomGyldighetsperiode=\"2010-08-17T00:00:00.000+02:00\" fomBruksperiode=\"2015-02-23T08:04:53.200+01:00\"><navn xsi:type=\"ns4:UstrukturertNavn\">" +
                "<navnelinje>VANN- OG AVLØPSETATEN</navnelinje><navnelinje></navnelinje><navnelinje></navnelinje><navnelinje></navnelinje><navnelinje></navnelinje></navn>" +
                "<redigertNavn>VANN- OG AVLØPSETATEN</redigertNavn></navn></organisasjonDetaljer><virksomhetDetaljer><eierskiftedato>2004-01-01+01:00</eierskiftedato>" +
                "<enhetstype kodeRef=\"BEDR\"></enhetstype></virksomhetDetaljer></organisasjon></response></ns2:hentOrganisasjonResponse>";

        JAXBContext jaxbContext = JAXBContext.newInstance(no.nav.tjeneste.virksomhet.organisasjon.v4.HentOrganisasjonResponse.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        Reader reader = new StringReader(orgXml);
        Object xmlBean = unmarshaller.unmarshal(reader);
        HentOrganisasjonResponse response = (HentOrganisasjonResponse) xmlBean;

        OrganisasjonsDetaljerDto organisasjonsDetaljerDto = OrganisasjonsDetaljerDto.toDto(response.getResponse().getOrganisasjon());

        // test Forretningsadresse
        List<GeografiskAdresse> forretningsadresser = response.getResponse().getOrganisasjon().getOrganisasjonDetaljer().getForretningsadresse();
        BostedsadresseDto bostedsadresseDto = new BostedsadresseDto();
        GateadresseDto gateadresse= new GateadresseDto();
        gateadresse.setGatenavn("Fjøsangerveien 68 ");

        bostedsadresseDto.setGateadresse(gateadresse);
        bostedsadresseDto.setPostnr("5068"); //TODO Kodeverk poststed

        bostedsadresseDto.setLand("NO");

        bostedsadresseDto.equals(organisasjonsDetaljerDto.getForretningsadresse());
        assertEquals(bostedsadresseDto, organisasjonsDetaljerDto.getForretningsadresse());

        // test Postadresse
        assertEquals("Postboks 7700 5020 NO", organisasjonsDetaljerDto.getPostadresse());



    }

}