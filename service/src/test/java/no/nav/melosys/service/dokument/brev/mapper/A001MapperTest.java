package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import no.nav.dok.melosysbrev._000116.BrevdataType;
import no.nav.dok.melosysbrev._000116.Fag;
import no.nav.dok.melosysbrev._000116.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.dok.melosysbrev.felles.melosys_vedlegg.VedleggType;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.KjoennsType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.behandlingsgrunnlag.data.FysiskArbeidssted;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA001;
import org.jeasy.random.EasyRandom;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xml.sax.SAXException;

import static no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning.UTSENDELSE_MELLOM_24_MN_OG_5_AAR;
import static no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning_uten_art12.SJOEMANNSKIRKEN;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.*;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class A001MapperTest {

    private A001Mapper mapper;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private EasyRandom easyRandom;

    private BrevDataA001 brevData;

    @Before
    public void setUp() {
        mapper = new A001Mapper();
        easyRandom = EasyRandomConfigurer.randomForDokProd();

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now(), Landkoder.NO,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2, Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5,
            Landkoder.NO, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, Trygdedekninger.FULL_DEKNING_EOSFO);

        Behandlingsresultat behandlingsresultat = mock(Behandlingsresultat.class);
        when(behandlingsresultat.getRegistrertDato()).thenReturn(Instant.now());

        StrukturertAdresse boAdresse = new StrukturertAdresse();
        boAdresse.gatenavn = "Gatenavn";
        boAdresse.husnummer = "23A";
        boAdresse.postnummer = "0165";
        boAdresse.poststed = "Oslo";
        boAdresse.landkode = Landkoder.NO.getKode();

        PersonDokument person = new PersonDokument();
        person.kjønn = new KjoennsType();
        person.kjønn.setKode("K");
        person.fornavn = "Ola";
        person.etternavn = "Nordmann";
        person.fødselsdato = LocalDate.now();
        person.fnr = "123456789";
        person.statsborgerskap = new Land();
        person.statsborgerskap.setKode("NO");

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        saksopplysning.setDokument(person);

        Behandling behandling = mock(Behandling.class);
        when(behandling.getRegistrertDato()).thenReturn(Instant.now());
        when(behandling.getSaksopplysninger()).thenReturn(new HashSet<>(Arrays.asList(saksopplysning)));
        when(behandling.getFagsak()).thenReturn(new Fagsak());

        StrukturertAdresse strukturertAdresse = lagStrukturertAdresse();

        FysiskArbeidssted arbeidssted = new FysiskArbeidssted();
        arbeidssted.adresse = strukturertAdresse;
        Soeknad søknad = new Soeknad();
        søknad.arbeidPaaLand.fysiskeArbeidssteder = Arrays.asList(arbeidssted);

        AvklartVirksomhet virksomhet = new AvklartVirksomhet("JARLSBERG AS",
                                               "123456789",
                                                strukturertAdresse, Yrkesaktivitetstyper.LOENNET_ARBEID);

        no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted fysiskArbeidssted = new no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted("JARLSBERG INTERNATIONAL", "123456789", strukturertAdresse);

        no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted maritimtArbeidssted = lagMaritimtArbeidssted();

        UtenlandskMyndighet myndighet = new UtenlandskMyndighet();
        myndighet.navn = "SAV";
        myndighet.institusjonskode = "23";
        myndighet.gateadresse = "Adresse";
        myndighet.postnummer = "0165";
        myndighet.poststed ="Stockholm";
        myndighet.landkode = Landkoder.SK;

        Vilkaarsresultat vilkår16 = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART16_1, true, UTSENDELSE_MELLOM_24_MN_OG_5_AAR);
        Vilkaarsresultat vilkår16Uten12 = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART16_1, true, SJOEMANNSKIRKEN);

        brevData = new BrevDataA001();
        brevData.arbeidsgivendeVirksomheter = new ArrayList<>(Arrays.asList(virksomhet));   // Hovedvirksomhet
        brevData.selvstendigeVirksomheter = new ArrayList<>();
        brevData.arbeidssteder = new ArrayList<>(Arrays.asList(fysiskArbeidssted, maritimtArbeidssted));
        brevData.personDokument = person;
        brevData.bostedsadresse = boAdresse;
        brevData.utenlandskMyndighet = myndighet;
        brevData.anmodningsperioder = Arrays.asList(anmodningsperiode);
        brevData.anmodningBegrunnelser = vilkår16.getBegrunnelser();
        brevData.anmodningUtenArt12Begrunnelser = vilkår16Uten12.getBegrunnelser();
        brevData.utenlandskIdent = Optional.empty();
        brevData.ansettelsesperiode = Optional.empty();
    }

    @Test
    public void mapTilBrevXMLArt16Begrunnelser() throws Exception {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-2");

        MelosysNAVFelles navFelles = easyRandom.nextObject(MelosysNAVFelles.class);
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        navFelles.setKontaktinformasjon(lagKontaktInformasjon());

        brevData.anmodningUtenArt12Begrunnelser.clear();

        String xml = mapTilBrevXML(fellesType, navFelles, brevData);
        assertThat(xml).isNotNull();
    }

    @Test
    public void mapTilBrevXMLArt16UtenArt12Begrunnelser() throws Exception {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-2");

        MelosysNAVFelles navFelles = easyRandom.nextObject(MelosysNAVFelles.class);
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        navFelles.setKontaktinformasjon(lagKontaktInformasjon());

        brevData.anmodningBegrunnelser.clear();

        String xml = mapTilBrevXML(fellesType, navFelles, brevData);
        assertThat(xml).isNotNull();
    }

    @Test
    public void mapTilBrevXMLUtenSelvstendigVirksomhet() throws Exception {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-2");

        MelosysNAVFelles navFelles = easyRandom.nextObject(MelosysNAVFelles.class);
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        navFelles.setKontaktinformasjon(lagKontaktInformasjon());

        String xml = mapTilBrevXML(fellesType, navFelles, brevData);
        assertThat(xml).isNotNull();
    }


    private String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, BrevData brevData) throws JAXBException, SAXException, TekniskException {
        final String XSD_LOCATION = "melosysbrev/melosys_000116.xsd";

        Fag fag = mapFag();
        VedleggType vedlegg = new VedleggType();
        vedlegg.setSEDA001(mapper.mapSEDA001((BrevDataA001) brevData));
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag, vedlegg);

        return JaxbHelper.marshalAndValidate(brevdataTypeJAXBElement, XSD_LOCATION);
    }

    public Fag mapFag() {
        Fag fag = new Fag();
        fag.setVedleggSEDA001("true");
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