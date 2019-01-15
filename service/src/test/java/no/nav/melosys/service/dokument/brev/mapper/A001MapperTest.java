package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import io.github.benas.randombeans.api.EnhancedRandom;
import no.nav.dok.melosysbrev._000116.BrevdataType;
import no.nav.dok.melosysbrev._000116.Fag;
import no.nav.dok.melosysbrev._000116.ObjectFactory;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.dok.melosysbrev.felles.melosys_vedlegg.VedleggType;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.UstrukturertAdresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.KjoennsType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.TilleggsBestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.Yrkesgrupper;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA001;
import no.nav.melosys.service.dokument.brev.mapper.felles.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;
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

public class A001MapperTest {

    private A001Mapper mapper;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private EnhancedRandom enhancedRandom;

    private Behandlingsresultat behandlingsresultat;
    private Behandling behandling;

    private BrevDataA001 brevData;

    @Before
    public void setUp() {
        mapper = new A001Mapper();
        enhancedRandom = EnhancedRandomConfigurer.randomForDokProd();

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);

        lovvalgsperiode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_2);
        lovvalgsperiode.setTilleggsbestemmelse(TilleggsBestemmelser_883_2004.FO_883_2004_ART11_5);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now());
        lovvalgsperiode.setUnntakFraLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setUnntakFraBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);

        behandlingsresultat = mock(Behandlingsresultat.class);
        when(behandlingsresultat.getRegistrertDato()).thenReturn(Instant.now());
        when(behandlingsresultat.getLovvalgsperioder()).thenReturn(new HashSet<>(Arrays.asList(lovvalgsperiode)));

        Bostedsadresse boAdresse = new Bostedsadresse();
        boAdresse.getGateadresse().setGatenavn("Gatenavn");
        boAdresse.getGateadresse().setHusnummer(23);
        boAdresse.setPostnr("0165");
        boAdresse.setPoststed("Oslo");
        boAdresse.setLand(new Land(Land.NORGE));

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
        saksopplysning.setType(SaksopplysningType.PERSONOPPLYSNING);
        saksopplysning.setDokument(person);

        behandling = mock(Behandling.class);
        when(behandling.getRegistrertDato()).thenReturn(Instant.now());
        when(behandling.getSaksopplysninger()).thenReturn(new HashSet<>(Arrays.asList(saksopplysning)));
        when(behandling.getFagsak()).thenReturn(new Fagsak());

        UstrukturertAdresse adresse = new UstrukturertAdresse();
        adresse.landKode = "Land";
        adresse.adresselinjer.add("Gatenavn");
        adresse.adresselinjer.add("25");
        adresse.adresselinjer.add("Postnummer");
        adresse.adresselinjer.add("Poststed");
        adresse.adresselinjer.add("Region");

        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.husnummer = "25";
        strukturertAdresse.gatenavn = "Gatenavn";
        strukturertAdresse.postnummer = "0165";
        strukturertAdresse.poststed = "Poststed";
        strukturertAdresse.region = "Region";
        strukturertAdresse.landKode = "Land";

        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse = strukturertAdresse;
        SoeknadDokument søknad = new SoeknadDokument();
        søknad.arbeidUtland = Arrays.asList(arbeidUtland);

        OrganisasjonsDetaljer organisasjonsDetaljer = mock(OrganisasjonsDetaljer.class);
        when(organisasjonsDetaljer.hentUstrukturertForretningsadresse()).thenReturn(adresse);

        Virksomhet virksomhet = new Virksomhet("JARLSBERG AS",
                                               "123456789",
                                                adresse);

        Arbeidssted fysiskArbeidssted = new Arbeidssted("JARLSBERG INTERNATIONAL", strukturertAdresse);
        Arbeidssted maritimtArbeidssted = new Arbeidssted("Seven Kestrel", "GB", Yrkesgrupper.SOKKEL_ELLER_SKIP);

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
        brevData.lovvalgsperioder = Arrays.asList(lovvalgsperiode);
        brevData.vilkårsresultat161 = vilkår;
        brevData.utenlandskIdent = Optional.empty();
        brevData.ansettelsesperiode = Optional.empty();
    }

    @Test
    public void mapTilBrevXML() throws Exception {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-2");

        MelosysNAVFelles navFelles = enhancedRandom.nextObject(MelosysNAVFelles.class);
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        navFelles.setKontaktinformasjon(lagKontaktInformasjon());

        String xml = mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevData);
        assertThat(xml).isNotNull();
    }

    public String mapTilBrevXML(FellesType fellesType, MelosysNAVFelles navFelles, Behandling behandling, Behandlingsresultat resultat, BrevData brevData) throws JAXBException, SAXException, TekniskException {
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