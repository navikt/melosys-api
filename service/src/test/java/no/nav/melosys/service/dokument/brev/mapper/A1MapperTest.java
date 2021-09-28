package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import no.nav.dok.melosysbrev._000067.A1;
import no.nav.dok.melosysbrev._000067.AdresseType;
import no.nav.dok.melosysbrev._000116.BrevdataType;
import no.nav.dok.melosysbrev._000116.Fag;
import no.nav.dok.melosysbrev._000116.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.BostedsadresseType;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.dok.melosysbrev.felles.melosys_vedlegg.VedleggType;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.behandlingsgrunnlag.data.arbeidssteder.LuftfartBase;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Maritimtyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesgrupper;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FlyvendeArbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.MaritimtArbeidssted;
import org.apache.commons.lang3.StringUtils;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.*;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse;
import static no.nav.melosys.service.dokument.brev.mapper.A1Mapper.*;
import static no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class A1MapperTest {

    private A1Mapper mapper;

    private EasyRandom easyRandom;

    private Behandlingsresultat behandlingsresultat;
    private Behandling behandling;

    private BrevDataA1 brevData;
    private FellesType fellesType;
    private MelosysNAVFelles navFelles;

    @BeforeAll
    void felleSetup() {
        mapper = new A1Mapper();
        easyRandom = EasyRandomConfigurer.randomForDokProd();

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);

        lovvalgsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_2);
        lovvalgsperiode.setTilleggsbestemmelse(Tilleggsbestemmelser_883_2004.FO_883_2004_ART11_5);

        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now());

        behandlingsresultat = mock(Behandlingsresultat.class);
        when(behandlingsresultat.getRegistrertDato()).thenReturn(Instant.now());
        when(behandlingsresultat.getLovvalgsperioder()).thenReturn(new HashSet<>(Collections.singletonList(lovvalgsperiode)));
        when(behandlingsresultat.hentValidertLovvalgsperiode()).thenReturn(lovvalgsperiode);

        behandling = mock(Behandling.class);
        when(behandling.getRegistrertDato()).thenReturn(Instant.now());
        when(behandling.getFagsak()).thenReturn(new Fagsak());
    }

    @BeforeEach
    public void setUp() {
        StrukturertAdresse boAdresse = new StrukturertAdresse();
        boAdresse.setHusnummerEtasjeLeilighet("12B");
        boAdresse.setGatenavn("Bogata");
        boAdresse.setPostnummer("0165");
        boAdresse.setPoststed("Poststed");
        boAdresse.setRegion("Region");
        boAdresse.setLandkode(Landkoder.NO.getKode());

        StrukturertAdresse strukturertAdresse = lagStrukturertAdresse();

        AvklartVirksomhet virksomhet = new AvklartVirksomhet("Jarlsberg",
            "123456789",
            strukturertAdresse,
            Yrkesaktivitetstyper.LOENNET_ARBEID);

        AvklartVirksomhet utenlandskVirksomhet = new AvklartVirksomhet("JARLSBERG INTERNATIONAL",
            "123456789",
            strukturertAdresse,
            Yrkesaktivitetstyper.LOENNET_ARBEID);

        Arbeidssted fysiskArbeidssted = new FysiskArbeidssted("JARLSBERG INTERNATIONAL", "123456789", strukturertAdresse);

        Arbeidssted maritimtArbeidsstedSkip = lagMaritimtArbeidssted(Maritimtyper.SKIP);
        MaritimtArbeidssted maritimtArbeidsstedSokkel = (MaritimtArbeidssted) lagMaritimtArbeidssted(Maritimtyper.SOKKEL);
        brevData = new BrevDataA1();
        brevData.yrkesgruppe = Yrkesgrupper.ORDINAER;
        brevData.bostedsadresse = boAdresse;
        brevData.arbeidssteder = new ArrayList<>(Arrays.asList(fysiskArbeidssted, maritimtArbeidsstedSkip, maritimtArbeidsstedSokkel));
        brevData.arbeidsland = List.of(Landkoder.SE);
        brevData.person = lagPersonopplysninger();
        brevData.hovedvirksomhet = virksomhet;
        brevData.bivirksomheter = new ArrayList<>(Collections.singletonList(utenlandskVirksomhet));

        fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-4");

        navFelles = easyRandom.nextObject(MelosysNAVFelles.class);
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        navFelles.setKontaktinformasjon(lagKontaktInformasjon());

    }


    @Test
    void mapTilBrevXML() throws Exception {
        String xml = mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevData);

        assertThat(xml).isNotNull();
    }

    @Test
    void mapTilBrevXML_hovedVirksomhetUtenOrgnr_fyll4_2MedMellomrom() throws Exception {
        ForetakUtland utenlandskForetak = lagForetakUtland(false);
        utenlandskForetak.orgnr = null;
        brevData.hovedvirksomhet = new AvklartVirksomhet(utenlandskForetak);
        brevData.arbeidsland = List.of(Landkoder.values());// List.of(Landkoder.GB, Landkoder.SE);

        mapper.mapA1(behandling, behandlingsresultat, brevData);

        String xml = mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevData);
        assertThat(xml).isNotNull();
    }

    @Test
    void mapTilBrevXML_bostedsAdresseIkkeGyldig_settBostedsadresseSinGateAdresseTom() throws Exception {
        brevData.bostedsadresse.setGatenavn(null);
        A1 a1 = mapper.mapA1(behandling, behandlingsresultat, brevData);
        assertThat(a1.getPerson().getBostedsadresse().getGatenavn()).isEqualTo("gatenavnFraBostedsadresse");

        String xml = mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevData);

        assertThat(xml).isNotNull();
    }

    @Test
    void mapBrevTilXML_arbeidslandUtenFysiskArbeidssted_fyllerPåMedArbeidsland() throws JAXBException, SAXException {
        brevData.arbeidsland = List.of(Landkoder.SE, Landkoder.DK, Landkoder.GB);
        A1 a1 = mapper.mapA1(behandling, behandlingsresultat, brevData);

        assertThat(a1.getFysiskArbeidsstedAdresseListe().getAdresse())
            .extracting("adresselinje1")
            .contains("Danmark, Sverige");

        String xml = mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevData);

        assertThat(xml).isNotNull();
    }

    @Test
    void mapBrevTilXML_harFlyvendeArbeidssted_fyllerUtHjemmebaseNavnOgLand() {
        Landkoder landkode = Landkoder.FI;
        LuftfartBase luftfartBase = new LuftfartBase();
        luftfartBase.hjemmebaseNavn = "hjemmebaseNavn";
        luftfartBase.hjemmebaseLand = landkode.getKode();

        brevData.arbeidssteder = List.of(new FlyvendeArbeidssted(luftfartBase));
        A1 a1 = mapper.mapA1(behandling, behandlingsresultat, brevData);

        assertThat(a1.getBivirksomhetListe().getBivirksomhet())
            .extracting("navn")
            .contains(luftfartBase.hjemmebaseNavn);
        assertThat(a1.getFysiskArbeidsstedAdresseListe().getAdresse())
            .extracting("adresselinje1")
            .contains(landkode.getBeskrivelse());
    }

    @Test
    void mapTilBrevXML_harKortAdressePåArbeidssted_brekkerIkkeAdresseOverFlereLinjer() {
        StrukturertAdresse adresse = lagStrukturertAdresse();
        Arbeidssted fysiskArbeidssted = new FysiskArbeidssted("", "", adresse);

        assertThat(fysiskArbeidssted.lagAdresselinje().length()).isLessThan(MAKS_ANTALL_TEGN_PER_LINJE_5_2);

        brevData.arbeidssteder = List.of(fysiskArbeidssted);
        brevData.arbeidsland = Collections.emptyList();

        A1 a1 = mapper.mapA1(behandling, behandlingsresultat, brevData);
        List<String> utfylteAdresselinjer = a1.getFysiskArbeidsstedAdresseListe().getAdresse().stream()
            .map(AdresseType::getAdresselinje1)
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toList());

        assertThat(utfylteAdresselinjer.size()).isEqualTo(1);
    }

    @Test
    void mapTilBrevXML_harLangAdressePåArbeidssted_brekkerAdresseOverFlereLinjer() {
        StrukturertAdresse adresse = lagStrukturertAdresse();
        adresse.setGatenavn(
            "Lorem ipsumdolorsitamet consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua-veien");
        adresse.setHusnummerEtasjeLeilighet("47");
        Arbeidssted fysiskArbeidssted = new FysiskArbeidssted("", "", adresse);

        assertThat(fysiskArbeidssted.lagAdresselinje().length()).isGreaterThan(MAKS_ANTALL_TEGN_PER_LINJE_5_2);

        brevData.arbeidssteder = List.of(fysiskArbeidssted);
        brevData.arbeidsland = Collections.emptyList();

        A1 a1 = mapper.mapA1(behandling, behandlingsresultat, brevData);
        List<String> utfylteAdresselinjer = a1.getFysiskArbeidsstedAdresseListe().getAdresse().stream()
            .map(AdresseType::getAdresselinje1)
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toList());

        assertThat(utfylteAdresselinjer.size()).isGreaterThan(1);
    }

    @Test
    void mapTilBrevXML_harUkjentEllerIkkeOppgittArbeidsted_brekkerAdresseOverFlereLinjer() {
        brevData.erUkjenteEllerAlleEosLand = true;
        brevData.arbeidssteder = Collections.emptyList();
        brevData.arbeidsland = Collections.emptyList();

        A1 a1 = mapper.mapA1(behandling, behandlingsresultat, brevData);
        List<String> utfylteAdresselinjer = a1.getFysiskArbeidsstedAdresseListe().getAdresse().stream()
            .map(AdresseType::getAdresselinje1)
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.toList());

        assertThat(utfylteAdresselinjer).containsExactly(FLERE_UKJENTE_ELLER_IKKE_OPPGITT_LAND);
    }

    @Test
    void mapTilBrevXML_brukerHarFlereStatsborgerskap_forventNorskSvenskOgDanskStatsborgerskapIAlfabetiskRekkefølge() {
        final A1 a1 = mapper.mapA1(behandling, behandlingsresultat, brevData);
        final Collection<String> statsborgerskap = Arrays.stream(a1.getPerson().getStatsborgerskap().split(",")).toList();
        assertThat(statsborgerskap).isEqualTo(List.of("DK", "NO", "SE"));
    }

    @Test
    void mapTilBrevXML_brukerErStatsløsFraPDL_forventStatløsTekst() {
        brevData.person = lagPersonopplysningerStatløs();
        A1 a1 = mapper.mapA1(behandling, behandlingsresultat, brevData);
        assertThat(a1.getPerson().getStatsborgerskap()).isEqualTo(STATSLØS_TEKST);
    }


    @Test
    void mapTilBrevXML_bostedsadresserFraRegisterPDL_forventBostedsadresse() {
        brevData.person = lagPersonopplysninger();
        A1 a1 = mapper.mapA1(behandling, behandlingsresultat, brevData);
        assertThat(a1.getPerson().getBostedsadresse().getGatenavn()).isEqualTo("gatenavnFraBostedsadresse");
        assertThat(a1.getPerson().getBostedsadresse().getHusnummer()).isEqualTo("3");
        assertThat(a1.getPerson().getBostedsadresse().getPostnr()).isEqualTo("1234");
        assertThat(a1.getPerson().getBostedsadresse().getPoststed()).isEqualTo("Oslo");
        assertThat(a1.getPerson().getBostedsadresse().getRegion()).isEqualTo("Norge");
        assertThat(a1.getPerson().getBostedsadresse().getLandkode()).isEqualTo("NO");
    }

    @Test
    void mapTilBrevXML_harFlereAdresserRegistrertFraPDL_forventUtfylltMidlertidigAdresseMedNyesteRegistrerteDato() {
        brevData.person = lagPersonopplysninger();
        A1 a1 = mapper.mapA1(behandling, behandlingsresultat, brevData);
        assertThat(a1.getPerson().getMidlertidigOppholdsadresse().getGatenavn()).isEqualTo("gatenavnOppholdsadresseFreg");
    }

    @Test
    void mapTilBrevXML_harIngenAdresserRegistrertFraPDL_kastIkkeFunnetException() {
        brevData.person = lagPersonopplysningerUtenAdresser();
        A1 a1 = mapper.mapA1(behandling, behandlingsresultat, brevData);
        assertThat(a1.getPerson().getBostedsadresse().getGatenavn()).isNull();
        assertThat(a1.getPerson().getMidlertidigOppholdsadresse().getGatenavn()).isNull();

    }

    @Test
    void mapTilBrevXML_harIkkeBostedsAdresseFraPDL_bostedsAdresseErTom() {
        brevData.person = lagPersonopplysningerUtenBostedsadresse();
        A1 a1 = mapper.mapA1(behandling, behandlingsresultat, brevData);
        assertThat(a1.getPerson().getBostedsadresse()).isEqualTo(new BostedsadresseType());
    }

    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevData) throws JAXBException, SAXException {
        final String XSD_LOCATION = "melosysbrev/melosys_000116.xsd";

        Fag fag = mapFag();
        VedleggType vedlegg = new VedleggType();
        vedlegg.setA1(mapper.mapA1(behandling, resultat, (BrevDataA1) brevData));
        JAXBElement<BrevdataType> brevdataTypeJAXBElement = mapintoBrevdataType(fellesType, navFelles, fag, vedlegg);

        return JaxbHelper.marshalAndValidate(brevdataTypeJAXBElement, XSD_LOCATION);
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
