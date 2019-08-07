package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import no.nav.dok.melosysbrev._000067.A1;
import no.nav.dok.melosysbrev._000116.BrevdataType;
import no.nav.dok.melosysbrev._000116.Fag;
import no.nav.dok.melosysbrev._000116.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.dok.melosysbrev.felles.melosys_vedlegg.VedleggType;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.KjoennsType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklartMaritimtArbeid;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.MaritimtArbeidssted;
import org.jeasy.random.EasyRandom;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class A1MapperTest {

    private A1Mapper mapper;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private EasyRandom easyRandom;

    private Behandlingsresultat behandlingsresultat;
    private Behandling behandling;

    private BrevDataA1 brevData;

    @Before
    public void setUp() {
        mapper = new A1Mapper();
        easyRandom = EasyRandomConfigurer.randomForDokProd();

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);

        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_2);
        lovvalgsperiode.setTilleggsbestemmelse(TilleggsBestemmelser_883_2004.FO_883_2004_ART11_5);

        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now());

        behandlingsresultat = mock(Behandlingsresultat.class);
        when(behandlingsresultat.getRegistrertDato()).thenReturn(Instant.now());
        when(behandlingsresultat.getLovvalgsperioder()).thenReturn(new HashSet<>(Collections.singletonList(lovvalgsperiode)));
        when(behandlingsresultat.hentValidertLovvalgsperiode()).thenReturn(lovvalgsperiode);

        StrukturertAdresse boAdresse = new StrukturertAdresse();
        boAdresse.husnummer = "12B";
        boAdresse.gatenavn = "Bogata";
        boAdresse.postnummer = "0165";
        boAdresse.poststed = "Poststed";
        boAdresse.region = "Region";
        boAdresse.landkode = Landkoder.NO.getKode();

        behandling = mock(Behandling.class);
        when(behandling.getRegistrertDato()).thenReturn(Instant.now());
        when(behandling.getFagsak()).thenReturn(new Fagsak());

        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.husnummer = "25";
        strukturertAdresse.gatenavn = "Gatenavn";
        strukturertAdresse.postnummer = "0165";
        strukturertAdresse.poststed = "Poststed";
        strukturertAdresse.region = "Region";
        strukturertAdresse.landkode = Landkoder.NO.getKode();;

        AvklartVirksomhet virksomhet = new AvklartVirksomhet("JARLSBERG INTERNATIONAL",
                                                           "123456789",
                                                            strukturertAdresse,
                                                            Yrkesaktivitetstyper.LOENNET_ARBEID);

        AvklartVirksomhet utenlandskVirksomhet = new AvklartVirksomhet("Jarlsberg",
                                                                        "123456789",
                                                                        strukturertAdresse,
                                                                        Yrkesaktivitetstyper.LOENNET_ARBEID);

        Arbeidssted fysiskArbeidssted = new FysiskArbeidssted("JARLSBERG INTERNATIONAL", "123456789", strukturertAdresse);

        AvklartMaritimtArbeid avklartMaritimtArbeid = mock(AvklartMaritimtArbeid.class);
        when(avklartMaritimtArbeid.getMaritimtype()).thenReturn(Maritimtyper.SOKKEL);
        when(avklartMaritimtArbeid.getLand()).thenReturn(Landkoder.GB.getKode());
        when(avklartMaritimtArbeid.getNavn()).thenReturn("Dunfjæder");
        Arbeidssted maritimtArbeidssted = new MaritimtArbeidssted(avklartMaritimtArbeid);

        brevData = new BrevDataA1();
        brevData.yrkesgruppe = Yrkesgrupper.ORDINAER;
        brevData.norskeVirksomheter = new ArrayList<>(Collections.singletonList(virksomhet));   // Hovedvirksomhet
        brevData.selvstendigeForetak = new HashSet<>();
        brevData.utenlandskeVirksomheter = new ArrayList<>(Collections.singletonList(utenlandskVirksomhet));
        brevData.bostedsadresse = boAdresse;
        brevData.arbeidssteder = new ArrayList<>(Arrays.asList(fysiskArbeidssted, maritimtArbeidssted));
        brevData.person = lagPersonDokument();
        brevData.hovedvirksomhet = virksomhet;
    }

    protected static PersonDokument lagPersonDokument() {
        PersonDokument person = new PersonDokument();
        person.kjønn = new KjoennsType();
        person.kjønn.setKode("K");
        person.fornavn = "Ola";
        person.etternavn = "Nordmann";
        person.fødselsdato = LocalDate.now();
        person.statsborgerskap = new Land();
        person.statsborgerskap.setKode("NOR");
        return person;
    }

    @Test
    public void mapTilBrevXML() throws Exception {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-4");

        MelosysNAVFelles navFelles = easyRandom.nextObject(MelosysNAVFelles.class);
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        navFelles.setKontaktinformasjon(lagKontaktInformasjon());

        String xml = mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevData);

        assertThat(xml).isNotNull();
    }

    @Test
    public void mapTilBrevXML_bostedsAdresseIkkeGyldig_settBostedsadresseSinGateAdresseTom() throws Exception {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-4");

        MelosysNAVFelles navFelles = easyRandom.nextObject(MelosysNAVFelles.class);
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        navFelles.setKontaktinformasjon(lagKontaktInformasjon());

        brevData.bostedsadresse.gatenavn = null;
        A1 a1 = mapper.mapA1(behandling, behandlingsresultat, brevData);
        assertThat(a1.getPerson().getBostedsadresse().getGatenavn()).isEqualTo(" ");

        String xml = mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevData);

        assertThat(xml).isNotNull();
    }


    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevData) throws JAXBException, SAXException, TekniskException {
        final String XSD_LOCATION = "melosysbrev/melosys_000116.xsd";

        Fag fag = mapFag();
        VedleggType vedlegg = new VedleggType();
        vedlegg.setA1(mapper.mapA1(behandling, resultat, (BrevDataA1) brevData));
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag, vedlegg);

        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
    }

    private Fag mapFag() {
        Fag fag = new Fag();
        fag.setVedleggA1("true");
        return fag;
    }

    private JAXBElement<BrevdataType> mapintoBrevdataType(FellesType fellesType, MelosysNAVFelles navFelles, Fag fag, VedleggType vedlegg) {
        ObjectFactory factory = new ObjectFactory();
        BrevdataType brevdataType = factory.createBrevdataType();
        brevdataType.setFelles(fellesType);
        brevdataType.setNAVFelles(navFelles);
        brevdataType.setFag(fag);
        brevdataType.setVedlegg(vedlegg);
        return factory.createBrevdata(brevdataType);
    }
}