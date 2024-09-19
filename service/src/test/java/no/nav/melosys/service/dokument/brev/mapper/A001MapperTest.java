package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;

import no.nav.dok.melosysbrev._000115.BostedsadresseTypeKode;
import no.nav.dok.melosysbrev._000116.BrevdataType;
import no.nav.dok.melosysbrev._000116.Fag;
import no.nav.dok.melosysbrev._000116.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.dok.melosysbrev.felles.melosys_vedlegg.VedleggType;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.person.KjoennsType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_konv_efta_storbritannia;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA001;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import static no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser.UTSENDELSE_MELLOM_24_MN_OG_5_AAR;
import static no.nav.melosys.domain.kodeverk.begrunnelser.Direkte_til_anmodning_begrunnelser.SJOEMANNSKIRKEN;
import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.*;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class A001MapperTest {

    private A001Mapper mapper;

    private EasyRandom easyRandom;

    private BrevDataA001 brevData;

    @BeforeEach
    public void setUp() {
        mapper = new A001Mapper();
        easyRandom = EasyRandomConfigurer.randomForDokProd();

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now(), Land_iso2.NO,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2, Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5,
            Land_iso2.NO, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, Trygdedekninger.FULL_DEKNING_EOSFO);

        Behandlingsresultat behandlingsresultat = mock(Behandlingsresultat.class);
        when(behandlingsresultat.getRegistrertDato()).thenReturn(Instant.now());

        StrukturertAdresse boAdresse = new StrukturertAdresse();
        boAdresse.setGatenavn("Gatenavn");
        boAdresse.setHusnummerEtasjeLeilighet("23A");
        boAdresse.setPostnummer("0165");
        boAdresse.setPoststed("Oslo");
        boAdresse.setLandkode(Landkoder.NO.getKode());

        PersonDokument person = new PersonDokument();
        person.setKjønn(new KjoennsType("K"));
        person.setFornavn("Ola");
        person.setEtternavn("Nordmann");
        person.setFødselsdato(LocalDate.now());
        person.setFnr("123456789");
        person.setStatsborgerskap(new Land(Land.NORGE));

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        saksopplysning.setDokument(person);

        Behandling behandling = mock(Behandling.class);
        when(behandling.getRegistrertDato()).thenReturn(Instant.now());
        when(behandling.getSaksopplysninger()).thenReturn(new HashSet<>(List.of(saksopplysning)));
        when(behandling.getFagsak()).thenReturn(FagsakTestFactory.lagFagsak());

        StrukturertAdresse strukturertAdresse = lagStrukturertAdresse();

        FysiskArbeidssted arbeidssted = new FysiskArbeidssted(null, strukturertAdresse);
        Soeknad søknad = new Soeknad();
        søknad.arbeidPaaLand.setFysiskeArbeidssteder(List.of(arbeidssted));

        AvklartVirksomhet virksomhet = new AvklartVirksomhet("JARLSBERG AS",
            "123456789",
            strukturertAdresse, Yrkesaktivitetstyper.LOENNET_ARBEID);

        no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted fysiskArbeidssted = new no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted("JARLSBERG INTERNATIONAL", "123456789", strukturertAdresse);

        no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted maritimtArbeidssted = lagMaritimtArbeidssted();

        UtenlandskMyndighet myndighet = new UtenlandskMyndighet();
        myndighet.setNavn("SAV");
        myndighet.setInstitusjonskode("23");
        myndighet.setGateadresse1("Adresse");
        myndighet.setPostnummer("0165");
        myndighet.setPoststed("Stockholm");
        myndighet.setLandkode(Land_iso2.SK);

        Vilkaarsresultat vilkår16 = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART16_1, true, UTSENDELSE_MELLOM_24_MN_OG_5_AAR);
        Vilkaarsresultat vilkår16Uten12 = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART16_1, true, SJOEMANNSKIRKEN);

        brevData = new BrevDataA001();
        brevData.setArbeidsgivendeVirksomheter(new ArrayList<>(List.of(virksomhet)));   // Hovedvirksomhet
        brevData.setSelvstendigeVirksomheter(new ArrayList<>());
        brevData.setArbeidssteder(new ArrayList<>(Arrays.asList(fysiskArbeidssted, maritimtArbeidssted)));
        brevData.setPersondata(person);
        brevData.setBostedsadresse(boAdresse);
        brevData.setUtenlandskMyndighet(myndighet);
        brevData.setAnmodningsperioder(List.of(anmodningsperiode));
        brevData.setAnmodningBegrunnelser(vilkår16.getBegrunnelser());
        brevData.setAnmodningUtenArt12Begrunnelser(vilkår16Uten12.getBegrunnelser());
        brevData.setUtenlandskIdent(null);
        brevData.setAnsettelsesperiode(null);
    }

    @Test
    void mapTilBrevXMLArt16Begrunnelser() throws Exception {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-2");

        MelosysNAVFelles navFelles = easyRandom.nextObject(MelosysNAVFelles.class);
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        navFelles.setKontaktinformasjon(lagKontaktInformasjon());

        brevData.getAnmodningUtenArt12Begrunnelser().clear();

        String xml = mapTilBrevXML(fellesType, navFelles, brevData);
        assertThat(xml).isNotNull();
    }

    @Test
    void mapTilBrevXMLArt16UtenArt12Begrunnelser() throws Exception {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-2");

        MelosysNAVFelles navFelles = easyRandom.nextObject(MelosysNAVFelles.class);
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        navFelles.setKontaktinformasjon(lagKontaktInformasjon());

        brevData.getAnmodningBegrunnelser().clear();

        String xml = mapTilBrevXML(fellesType, navFelles, brevData);
        assertThat(xml).isNotNull();
    }

    @Test
    void mapTilBrevXMLUtenSelvstendigVirksomhet() throws Exception {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-2");

        MelosysNAVFelles navFelles = easyRandom.nextObject(MelosysNAVFelles.class);
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        navFelles.setKontaktinformasjon(lagKontaktInformasjon());

        String xml = mapTilBrevXML(fellesType, navFelles, brevData);
        assertThat(xml).isNotNull();
    }

    @Test
    void mapSEDA001_kontaktAdresseType_korrektAdresseType() {
        brevData.setBostedsadresseTypeKode(BostedsadresseTypeKode.KONTAKTADRESSE);

        var SEDA001 = mapper.mapSEDA001(brevData);

        assertThat(SEDA001.getPerson().getBostedsadresse().getAdresseType()).isEqualTo(BostedsadresseTypeKode.KONTAKTADRESSE);
    }

    @Test
    void mapSEDA001_ingenAdresseType_korrektAdresseType() {
        brevData.setBostedsadresseTypeKode(null);

        var SEDA001 = mapper.mapSEDA001(brevData);

        assertThat(SEDA001.getPerson().getBostedsadresse().getAdresseType()).isEqualTo(BostedsadresseTypeKode.BOSTEDSLAND);
    }

    @Test
    void mapSEDA001_storbritannia_blirMappetKorrekt() {
        brevData.getAnmodningsperioder().forEach(periode -> {
            periode.setBestemmelse(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART18_1);
            periode.setUnntakFraBestemmelse(Lovvalgbestemmelser_konv_efta_storbritannia.KONV_EFTA_STORBRITANNIA_ART13_3D);
        });
        brevData.setYtterligereInformasjon("Fritekst fra saksbehandler.");

        var SEDA001 = mapper.mapSEDA001(brevData);

        assertThat(SEDA001.getLovvalgsbestemmelse().value()).isEqualTo(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_3E.getKode());
        assertThat(SEDA001.getYtterligereInformasjon()).isEqualTo("Issued under the EEA EFTA Convention. Fritekst fra saksbehandler.");
    }


    private String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, BrevData brevData) throws JAXBException, SAXException {
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
