package no.nav.melosys.integrasjon.tps.mapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import no.nav.melosys.domain.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.person.adresse.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.adresse.UstrukturertAdresse;
import no.nav.melosys.domain.person.Master;
import no.nav.melosys.domain.person.adresse.Kontaktadresse;
import no.nav.melosys.integrasjon.kodeverk.KodeOppslag;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PersonMapperTest {
    @Mock
    KodeOppslag kodeOppslag;

    @Test
    void mapTilPerson_medDokumentFraXml_girForventetMapping() {
        final String kilde = "mock/person/tps_person_3.0_mock.xml";
        HentPersonResponse response = lagHentPersonResponseFraXml(kilde);

        PersonDokument personDokument = PersonMapper.mapTilPerson(response.getPerson(), kodeOppslag);
        assertThat(personDokument).isNotNull();
        assertThat(personDokument.hentFolkeregisterident()).isEqualTo("11111111111");
        assertThat(personDokument.getSivilstand().getKode()).isEqualTo("UGIF");
        assertThat(personDokument.getSivilstandGyldighetsperiodeFom()).isEqualTo(LocalDate.parse("2010-08-09"));
        assertThat(personDokument.hentAlleStatsborgerskap()).isEqualTo(Set.of(Land.av(Land.SVERIGE)));
        assertThat(personDokument.getKjønn().getKode()).isEqualTo("K");
        assertThat(personDokument.getFornavn()).isEqualTo("MAGICA");
        assertThat(personDokument.getMellomnavn()).isEqualTo("FRA");
        assertThat(personDokument.getEtternavn()).isEqualTo("TRYLL");
        assertThat(personDokument.getSammensattNavn()).isEqualTo("MAGICA FRA TRYLL");
        assertThat(personDokument.getFødselsdato()).isEqualTo(LocalDate.parse("2011-11-11"));
        assertThat(personDokument.getDødsdato()).isNull();
        assertThat(personDokument.getDiskresjonskode().getKode()).isEqualTo("SPFO");
        assertThat(personDokument.getPersonstatus().getKode()).isEqualTo("BOSA");
        assertThat(personDokument.getBostedsadresse().getPostnr()).isEqualTo("5141");
        assertThat(personDokument.getBostedsadresse().getLand()).isEqualTo(Land.av(Land.NORGE));
        assertThat(personDokument.getBostedsadresse().getGateadresse())
            .extracting("gatenavn", "husnummer", "husbokstav")
            .isEqualTo(List.of("XXXXXX", 7, "A"));
    }

    @Test
    void mapTilPerson_testFamilierelasjoner_verifiserAtdokumentIkkeErTomt() {
        final String kilde = "mock/person/familierelasjoner.xml";
        HentPersonResponse response = lagHentPersonResponseFraXml(kilde);

        PersonDokument dokument = PersonMapper.mapTilPerson(response.getPerson(), kodeOppslag);
        // Verifiser...
        Assertions.assertThat(dokument).isNotNull();
        Assertions.assertThat(dokument.getFamiliemedlemmer()).isNotEmpty();
    }

    @Test
    void mapTilPerson_testMidlertidigPostadresseUtland_mapGjeldendePostadresse() {
        final String kilde = "mock/person/midlertidig_postadresse_utland.xml";
        HentPersonResponse response = lagHentPersonResponseFraXml(kilde);

        PersonDokument dokument = PersonMapper.mapTilPerson(response.getPerson(), kodeOppslag);

        Assertions.assertThat(dokument).isNotNull();
        Assertions.assertThat(dokument.getPostadresse()).isNotNull();
        Assertions.assertThat(dokument.getMidlertidigPostadresse()).isNotNull();
        Assertions.assertThat(dokument.getMidlertidigPostadresse().land.getKode()).isEqualTo("GBR");
    }

    @Test
    void mapTilPerson_testMidlertidigPostadresseNorge_mapGjeldendePostadresse() {
        final String kilde = "mock/person/midlertidig_postadresse_norge.xml";
        HentPersonResponse response = lagHentPersonResponseFraXml(kilde);

        PersonDokument dokument = PersonMapper.mapTilPerson(response.getPerson(), kodeOppslag);

        Assertions.assertThat(dokument).isNotNull();
        Assertions.assertThat(dokument.getMidlertidigPostadresse()).isNotNull();

        MidlertidigPostadresseNorge midlertidigPostadresseNorge = (MidlertidigPostadresseNorge) dokument.getMidlertidigPostadresse();
        Assertions.assertThat(midlertidigPostadresseNorge.gateadresse.getGatenummer()).isEqualTo(29);
        Assertions.assertThat(midlertidigPostadresseNorge.gateadresse.getHusnummer()).isEqualTo(7);
    }

    @Test
    void mapTilPerson_testMidlertidigPostadresseNorgeMedMatrikkeladresse_mapGjeldendePostadresse() {
        final String kilde = "mock/person/midlertidig_postadresse_norge_matrikkel.xml";
        HentPersonResponse response = lagHentPersonResponseFraXml(kilde);

        PersonDokument dokument = PersonMapper.mapTilPerson(response.getPerson(), kodeOppslag);

        MidlertidigPostadresseNorge midlertidigPostadresseNorge = (MidlertidigPostadresseNorge) dokument.getMidlertidigPostadresse();
        Assertions.assertThat(midlertidigPostadresseNorge.gateadresse.getGatenavn()).isEqualTo("Bugstadveien 101");
    }

    @Test
    void mapTilPerson_gittBostedsadresseGjeldende_mapGjeldendePostadresse() {
        final String kilde = "mock/person/bostedsadresse.xml";
        HentPersonResponse response = lagHentPersonResponseFraXml(kilde);

        PersonDokument dokument = PersonMapper.mapTilPerson(response.getPerson(), kodeOppslag);

        assertNotNull(dokument);

        Bostedsadresse bostedsadresse = dokument.getBostedsadresse();
        UstrukturertAdresse gjeldendePostadresse = dokument.getGjeldendePostadresse();

        assertNotNull(bostedsadresse);
        assertNotNull(gjeldendePostadresse);
        assertNotNull(dokument.getPostadresse());

        assertEquals(bostedsadresse.getLand(), gjeldendePostadresse.land);
        assertEquals(bostedsadresse.getPostnr(), gjeldendePostadresse.postnr);
    }

    @Test
    void mapTilPerson_gittMidlertidigadresseGjeldende_mapGjeldendePostadresse() {
        final String kilde = "mock/person/midlertidig_co_adresse.xml";
        HentPersonResponse response = lagHentPersonResponseFraXml(kilde);

        PersonDokument dokument = PersonMapper.mapTilPerson(response.getPerson(), kodeOppslag);

        assertNotNull(dokument);

        Bostedsadresse bostedsadresse = dokument.getBostedsadresse();
        UstrukturertAdresse gjeldendePostadresse = dokument.getGjeldendePostadresse();
        MidlertidigPostadresseNorge midlertidigPostadresseNorge = (MidlertidigPostadresseNorge) dokument.getMidlertidigPostadresse();

        assertNotNull(bostedsadresse);
        assertNotNull(gjeldendePostadresse);
        assertNotNull(midlertidigPostadresseNorge);

        assertEquals(midlertidigPostadresseNorge.land, gjeldendePostadresse.land);
        assertEquals(midlertidigPostadresseNorge.poststed, gjeldendePostadresse.postnr);
        assertTrue(gjeldendePostadresse.adresselinje1.startsWith("C/O"));
    }

    @Test
    void mapTilPerson_medDokumentUtenBostedsadresseFraXml_forventKontaktadresseUtfylt(){
        final String kilde = "mock/person/postadresse_utland.xml";
        HentPersonResponse response = lagHentPersonResponseFraXml(kilde);


        PersonDokument dokument = PersonMapper.mapTilPerson(response.getPerson(), kodeOppslag);

        assertNotNull(dokument);

        Optional<Kontaktadresse> kontaktadresse = dokument.finnKontaktadresse();
        assertThat(kontaktadresse.isPresent()).isTrue();

        assertThat(kontaktadresse.get().master()).isEqualTo(Master.TPS.name());
        assertThat(kontaktadresse.get().gyldigFraOgMed()).isNull();
        assertThat(kontaktadresse.get().gyldigTilOgMed()).isNull();
        assertThat(kontaktadresse.get().kilde()).isEqualTo(Master.TPS.name());
        assertThat(kontaktadresse.get().coAdressenavn()).isNull();
        assertThat(kontaktadresse.get().registrertDato()).isNull();
        assertThat(kontaktadresse.get().erHistorisk()).isFalse();

        SemistrukturertAdresse semistrukturertAdresse = kontaktadresse.get().semistrukturertAdresse();
        assertThat(semistrukturertAdresse.adresselinje1()).isEqualTo("42 Mock Road");
        assertThat(semistrukturertAdresse.adresselinje2()).isEqualTo("Mock City");
        assertThat(semistrukturertAdresse.adresselinje3()).isEqualTo("United Kingdom");
        assertThat(semistrukturertAdresse.landkode()).isEqualTo(Land.STORBRITANNIA);
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
