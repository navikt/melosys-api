package no.nav.melosys.integrasjon.tps.mapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.adresse.UstrukturertAdresse;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PersonMapperTest {

    @Test
    void mapTilPerson_medDokumentFraXml_girForventetMapping() {
        final String kilde = "mock/person/tps_person_3.0_mock.xml";
        HentPersonResponse response = lagHentPersonResponseFraXml(kilde);

        PersonDokument personDokument = PersonMapper.mapTilPerson(response.getPerson());
        assertThat(personDokument).isNotNull();
        assertThat(personDokument.fnr).isEqualTo("11111111111");
        assertThat(personDokument.sivilstand.getKode()).isEqualTo("UGIF");
        assertThat(personDokument.sivilstandGyldighetsperiodeFom).isEqualTo(LocalDate.parse("2010-08-09"));
        assertThat(personDokument.statsborgerskap.getKode()).isEqualTo(Land.SVERIGE);
        assertThat(personDokument.kjønn.getKode()).isEqualTo("K");
        assertThat(personDokument.fornavn).isEqualTo("MAGICA");
        assertThat(personDokument.mellomnavn).isEqualTo("FRA");
        assertThat(personDokument.etternavn).isEqualTo("TRYLL");
        assertThat(personDokument.sammensattNavn).isEqualTo("MAGICA FRA TRYLL");
        assertThat(personDokument.fødselsdato).isEqualTo(LocalDate.parse("2011-11-11"));
        assertThat(personDokument.dødsdato).isNull();
        assertThat(personDokument.diskresjonskode.getKode()).isEqualTo("SPFO");
        assertThat(personDokument.personstatus.getKode()).isEqualTo("BOSA");
        assertThat(personDokument.bostedsadresse.getPostnr()).isEqualTo("5141");
        assertThat(personDokument.bostedsadresse.getLand()).isEqualTo(Land.av(Land.NORGE));
        assertThat(personDokument.bostedsadresse.getGateadresse())
            .extracting("gatenavn", "husnummer", "husbokstav")
            .isEqualTo(List.of("XXXXXX", 7, "A"));
    }

    @Test
    void testFamilierelasjoner() {
        final String kilde = "mock/person/familierelasjoner.xml";
        HentPersonResponse response = lagHentPersonResponseFraXml(kilde);

        PersonDokument dokument = PersonMapper.mapTilPerson(response.getPerson());
        // Verifiser...
        Assertions.assertThat(dokument).isNotNull();
        Assertions.assertThat(dokument.familiemedlemmer).isNotEmpty();
    }

    @Test
    void testMidlertidigPostadresseUtland() {
        final String kilde = "mock/person/midlertidig_postadresse_utland.xml";
        HentPersonResponse response = lagHentPersonResponseFraXml(kilde);

        PersonDokument dokument = PersonMapper.mapTilPerson(response.getPerson());

        Assertions.assertThat(dokument).isNotNull();
        Assertions.assertThat(dokument.postadresse).isNotNull();
        Assertions.assertThat(dokument.midlertidigPostadresse).isNotNull();
        Assertions.assertThat(dokument.midlertidigPostadresse.land.getKode()).isEqualTo("GBR");
    }

    @Test
    void testMidlertidigPostadresseNorge() {
        final String kilde = "mock/person/midlertidig_postadresse_norge.xml";
        HentPersonResponse response = lagHentPersonResponseFraXml(kilde);

        PersonDokument dokument = PersonMapper.mapTilPerson(response.getPerson());

        Assertions.assertThat(dokument).isNotNull();
        Assertions.assertThat(dokument.midlertidigPostadresse).isNotNull();

        MidlertidigPostadresseNorge midlertidigPostadresseNorge = (MidlertidigPostadresseNorge) dokument.midlertidigPostadresse;
        Assertions.assertThat(midlertidigPostadresseNorge.gateadresse.getGatenummer()).isEqualTo(29);
        Assertions.assertThat(midlertidigPostadresseNorge.gateadresse.getHusnummer()).isEqualTo(7);
    }

    @Test
    void testMidlertidigPostadresseNorgeMedMatrikkeladresse() {
        final String kilde = "mock/person/midlertidig_postadresse_norge_matrikkel.xml";
        HentPersonResponse response = lagHentPersonResponseFraXml(kilde);

        PersonDokument dokument = PersonMapper.mapTilPerson(response.getPerson());

        MidlertidigPostadresseNorge midlertidigPostadresseNorge = (MidlertidigPostadresseNorge) dokument.midlertidigPostadresse;
        Assertions.assertThat(midlertidigPostadresseNorge.gateadresse.getGatenavn()).isEqualTo("Bugstadveien 101");
    }

    @Test
    void gittBostedsadresseGjeldendeMapGjeldendePostadresse() {
        final String kilde = "mock/person/bostedsadresse.xml";
        HentPersonResponse response = lagHentPersonResponseFraXml(kilde);

        PersonDokument dokument = PersonMapper.mapTilPerson(response.getPerson());

        assertNotNull(dokument);

        Bostedsadresse bostedsadresse = dokument.bostedsadresse;
        UstrukturertAdresse gjeldendePostadresse = dokument.gjeldendePostadresse;

        assertNotNull(bostedsadresse);
        assertNotNull(gjeldendePostadresse);
        assertNotNull(dokument.postadresse);

        assertEquals(bostedsadresse.getLand(), gjeldendePostadresse.land);
        assertEquals(bostedsadresse.getPostnr(), gjeldendePostadresse.postnr);
    }

    @Test
    void gittMidlertidigadresseGjeldendeMapGjeldendePostadresse() {
        final String kilde = "mock/person/midlertidig_co_adresse.xml";
        HentPersonResponse response = lagHentPersonResponseFraXml(kilde);

        PersonDokument dokument = PersonMapper.mapTilPerson(response.getPerson());

        assertNotNull(dokument);

        Bostedsadresse bostedsadresse = dokument.bostedsadresse;
        UstrukturertAdresse gjeldendePostadresse = dokument.gjeldendePostadresse;
        MidlertidigPostadresseNorge midlertidigPostadresseNorge = (MidlertidigPostadresseNorge) dokument.midlertidigPostadresse;

        assertNotNull(bostedsadresse);
        assertNotNull(gjeldendePostadresse);
        assertNotNull(midlertidigPostadresseNorge);

        assertEquals(midlertidigPostadresseNorge.land, gjeldendePostadresse.land);
        assertEquals(midlertidigPostadresseNorge.poststed, gjeldendePostadresse.postnr);
        assertTrue(gjeldendePostadresse.adresselinje1.startsWith("C/O"));
    }

    private HentPersonResponse lagHentPersonResponseFraXml(String ressurs) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(ressurs)) {
            JAXBContext jaxbContext = JAXBContext.newInstance(no.nav.tjeneste.virksomhet.person.v3.HentPersonResponse.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return ((no.nav.tjeneste.virksomhet.person.v3.HentPersonResponse) unmarshaller.unmarshal(inputStream)).getResponse();
        } catch (JAXBException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
