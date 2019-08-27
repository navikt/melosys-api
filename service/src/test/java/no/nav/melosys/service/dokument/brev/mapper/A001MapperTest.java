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
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.KjoennsType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA001;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted;
import org.jeasy.random.EasyRandom;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagMaritimtArbeidssted;
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

    private Behandlingsresultat behandlingsresultat;
    private Behandling behandling;

    private BrevDataA001 brevData;

    @Before
    public void setUp() {
        mapper = new A001Mapper();
        easyRandom = EasyRandomConfigurer.randomForDokProd();

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode(LocalDate.now(), LocalDate.now(), Landkoder.NO,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2, Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5,
            Landkoder.NO, Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, Trygdedekninger.FULL_DEKNING_EOSFO);

        behandlingsresultat = mock(Behandlingsresultat.class);
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

        behandling = mock(Behandling.class);
        when(behandling.getRegistrertDato()).thenReturn(Instant.now());
        when(behandling.getSaksopplysninger()).thenReturn(new HashSet<>(Arrays.asList(saksopplysning)));
        when(behandling.getFagsak()).thenReturn(new Fagsak());

        StrukturertAdresse strukturertAdresse = lagStrukturertAdresse();

        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse = strukturertAdresse;
        SoeknadDokument søknad = new SoeknadDokument();
        søknad.arbeidUtland = Arrays.asList(arbeidUtland);

        AvklartVirksomhet virksomhet = new AvklartVirksomhet("JARLSBERG AS",
                                               "123456789",
                                                strukturertAdresse, Yrkesaktivitetstyper.LOENNET_ARBEID);

        Arbeidssted fysiskArbeidssted = new FysiskArbeidssted("JARLSBERG INTERNATIONAL", "123456789", strukturertAdresse);

        Arbeidssted maritimtArbeidssted = lagMaritimtArbeidssted();

        UtenlandskMyndighet myndighet = new UtenlandskMyndighet();
        myndighet.navn = "SAV";
        myndighet.institusjonskode = "23";
        myndighet.gateadresse = "Adresse";
        myndighet.postnummer = "0165";
        myndighet.poststed ="Stockholm";
        myndighet.landkode = Landkoder.SK;

        Vilkaarsresultat vilkår = new Vilkaarsresultat();
        vilkår.setBegrunnelseFritekst("Fritekst");
        vilkår.setOppfylt(true);
        VilkaarBegrunnelse begrunnelse = new VilkaarBegrunnelse();
        begrunnelse.setKode("UTSENDELSE_MELLOM_24_MN_OG_5_AAR");
        vilkår.getBegrunnelser().add(begrunnelse);

        brevData = new BrevDataA001();
        brevData.arbeidsgivendeVirkomsheter = new ArrayList<>(Arrays.asList(virksomhet));   // Hovedvirksomhet
        brevData.selvstendigeVirksomheter = new ArrayList<>();
        brevData.arbeidssteder = new ArrayList<>(Arrays.asList(fysiskArbeidssted, maritimtArbeidssted));
        brevData.personDokument = person;
        brevData.bostedsadresse = boAdresse;
        brevData.utenlandskMyndighet = myndighet;
        brevData.anmodningsperioder = Arrays.asList(anmodningsperiode);
        brevData.vilkårsresultat161 = vilkår;
        brevData.utenlandskIdent = Optional.empty();
        brevData.ansettelsesperiode = Optional.empty();
    }

    @Test
    public void mapTilBrevXMLUtenSelvstendigVirksomhet() throws Exception {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-2");

        MelosysNAVFelles navFelles = easyRandom.nextObject(MelosysNAVFelles.class);
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        navFelles.setKontaktinformasjon(lagKontaktInformasjon());

        String xml = mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevData);
        assertThat(xml).isNotNull();
    }

    @Test
    public void mapTilBrevXML_MedSelvstendigVirksomhet_tarIkkeMedArbeidsgivendeVirkomsheter() throws Exception {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-2");

        MelosysNAVFelles navFelles = easyRandom.nextObject(MelosysNAVFelles.class);
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        navFelles.setKontaktinformasjon(lagKontaktInformasjon());

        AvklartVirksomhet avklartVirksomhet = new AvklartVirksomhet("Ranselbygg AS",
            "998877665",
            lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID);
        brevData.selvstendigeVirksomheter = Collections.singletonList(avklartVirksomhet);

        String xml = mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevData);
        assertThat(xml).doesNotContain("JARLSBERG AS");
        assertThat(xml).contains("Ranselbygg AS");
    }

    private StrukturertAdresse lagStrukturertAdresse() {
        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.husnummer = "25";
        strukturertAdresse.gatenavn = "Gatenavn";
        strukturertAdresse.postnummer = "0165";
        strukturertAdresse.poststed = "Poststed";
        strukturertAdresse.region = "Region";
        strukturertAdresse.landkode = Landkoder.NO.getKode();
        return strukturertAdresse;
    }

    private String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevData) throws JAXBException, SAXException, TekniskException {
        final String XSD_LOCATION = "melosysbrev/melosys_000116.xsd";

        Fag fag = mapFag();
        VedleggType vedlegg = new VedleggType();
        vedlegg.setSEDA001(mapper.mapSEDA001((BrevDataA001) brevData));
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag, vedlegg);

        return JaxbHelper.marshalAndValidateJaxb(BrevdataType.class, brevdataTypeJAXBElement, XSD_LOCATION);
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