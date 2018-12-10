package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import no.nav.dok.brevdata.felles.v1.navfelles.*;
import no.nav.dok.brevdata.felles.v1.simpletypes.AktoerType;
import no.nav.dok.brevdata.felles.v1.simpletypes.Spraakkode;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.avklartefakta.AvklartefaktaType;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevData;

import org.junit.Test;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class InnvilgelsesbrevMapperTest {

    private final InnvilgelsesbrevMapper instans;

    public InnvilgelsesbrevMapperTest() {
        instans = new InnvilgelsesbrevMapper();
    }

    @Test
    public void mapTilBrevXmlGirIkkeTomXmlStreng() throws Exception {
        testMapTilBrevXml();
    }

    @Test
    public void mapTilBrevXmlMedDysfunksjoneltDataTypeFactoryKasterUnntak() throws Exception {
        String dataTypeFactoryClass = null;
        Throwable unntak;
        try {
            dataTypeFactoryClass = System.setProperty(DatatypeFactory.DATATYPEFACTORY_PROPERTY, "no.datatypefactory.here.Hahahahahha");
            unntak = catchThrowable(() -> testMapTilBrevXml());
        } finally {
            if (dataTypeFactoryClass != null) {
                System.setProperty(DatatypeFactory.DATATYPEFACTORY_PROPERTY, dataTypeFactoryClass);
            } else {
                System.getProperties().remove(DatatypeFactory.DATATYPEFACTORY_PROPERTY);
            }
        }
        assertThat(unntak).isInstanceOf(IllegalStateException.class)
            .hasCauseInstanceOf(DatatypeConfigurationException.class)
            .hasMessageContaining("Kan ikke lage DatatypeConverterFactory");
    }

    @Test
    public final void mapTilBrevXmlMedFlereLovvalgsperioderFeiler() throws Exception {
        Set<Lovvalgsperiode> perioder = new HashSet<>(Arrays.asList(lagLovvalgsperiode(LocalDate.MIN), lagLovvalgsperiode(LocalDate.now())));
        Throwable unntak = catchThrowable(() -> testMapTilBrevXml(lagBehandlingsresultat(perioder)));
        assertThat(unntak).isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("perioder ulik 1 støttes ikke");
    }

    @Test
    public void mapArbeidslandFraSøknadsTilBrevXmlGirIkkeTomXmlStreng() throws Exception {
        testMapTilBrevXml(lagBehandlingsresultat(Collections.singleton(lagLovvalgsperiode()),
                Collections.singleton(lagAvklarteFakta(AvklartefaktaType.AVKLARTE_ARBEIDSGIVER, "123456789"))));
    }

    @Test
    public void mapTilBrevXmlUtenArbeidslandISøknadGirUnntak() throws Exception {
        Behandling behandlingUtenSaksopplysninger = lagBehandling(lagFagsak(), Collections.emptySet());
        Behandlingsresultat behandlingsresultatUtenAvklartArbeidsland = lagBehandlingsresultat(Collections.singleton(lagLovvalgsperiode()),
                Collections.singleton(lagAvklarteFakta(AvklartefaktaType.AVKLARTE_ARBEIDSGIVER, "123456789")));
        Throwable unntak = catchThrowable(() -> testMapTilBrevXml(behandlingUtenSaksopplysninger,
                behandlingsresultatUtenAvklartArbeidsland));
        assertThat(unntak).isInstanceOf(IllegalStateException.class)
            .hasCauseInstanceOf(TekniskException.class)
            .hasMessageContaining("Finner ikke søknaddokument");
    }

    @Test
    public final void mapTilBrevXmlUtenAvklartArbeidsgiverKasterUnntak() throws Exception {
        Behandlingsresultat resultatUtenAvklartArbeidsgiver = lagBehandlingsresultat(Collections.singleton(lagLovvalgsperiode()),
                Collections.emptySet());
        Throwable unntak = catchThrowable(() -> testMapTilBrevXml(resultatUtenAvklartArbeidsgiver));
        assertThat(unntak).isInstanceOf(TekniskException.class)
            .hasNoCause()
            .hasMessageContaining("mangler faktumet AVKLART");
    }

    @Test
    public final void mapTilBrevXmlMedUavklartArbeidsgiverFaktumKasterUnntak() throws Exception {
        Avklartefakta uavklartArbeidsgiverFaktum = lagAvklarteFakta(AvklartefaktaType.AVKLARTE_ARBEIDSGIVER, "987654321", "VET IKKE");
        Behandlingsresultat resultatUtenAvklartArbeidsgiver = lagBehandlingsresultat(Collections.singleton(lagLovvalgsperiode()),
                Collections.singleton(uavklartArbeidsgiverFaktum));
        Throwable unntak = catchThrowable(() -> testMapTilBrevXml(resultatUtenAvklartArbeidsgiver));
        assertThat(unntak).isInstanceOf(TekniskException.class)
            .hasNoCause()
            .hasMessageContaining("mangler faktumet AVKLART");
    }

    public void testMapTilBrevXml() throws Exception {
        testMapTilBrevXml(lagBehandlingsresultat());
    }

    public void testMapTilBrevXml(Behandlingsresultat behandlingsresultat) throws Exception {
        testMapTilBrevXml(lagBehandling(lagFagsak()), behandlingsresultat);
    }

    public void testMapTilBrevXml(Behandling behandling, Behandlingsresultat behandlingsresultat) throws Exception {
        FellesType fellesType = lagFellesType();
        MelosysNAVFelles navFelles = LagMelosysNAVFelles();
        BrevData brevdata = new BrevData();
        String resultat = instans.mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevdata);
        // TODO: Vurder å bruke XMLUnit e.l. til å sammenlikne XML-strengen
        // grundig mot forventninger.
        assertThat(resultat).matches("(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\n.*");
    }

    private static Behandlingsresultat lagBehandlingsresultat() {
        Set<Avklartefakta> fakta = new HashSet<>(Arrays.asList(
                lagAvklarteFakta(AvklartefaktaType.AVKLARTE_ARBEIDSGIVER, "123456789"),
                lagAvklarteFakta(AvklartefaktaType.AG_FORRETNINGSLAND, "SE")));
        return lagBehandlingsresultat(Collections.singleton(lagLovvalgsperiode()), fakta);
    }

    private static Behandlingsresultat lagBehandlingsresultat(Set<Lovvalgsperiode> perioder) {
        Set<Avklartefakta> fakta = new HashSet<>(Arrays.asList(
                lagAvklarteFakta(AvklartefaktaType.AVKLARTE_ARBEIDSGIVER, "123456789"),
                lagAvklarteFakta(AvklartefaktaType.AG_FORRETNINGSLAND, "SE")));
        return lagBehandlingsresultat(perioder, fakta);
    }

    private static Behandlingsresultat lagBehandlingsresultat(Set<Lovvalgsperiode> perioder, Set<Avklartefakta> fakta) {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setAvklartefakta(fakta);
        behandlingsresultat.setLovvalgsperioder(perioder);
        return behandlingsresultat;
    }

    private static Lovvalgsperiode lagLovvalgsperiode() {
        return lagLovvalgsperiode(LocalDate.now());
    }

    private static Lovvalgsperiode lagLovvalgsperiode(LocalDate fom) {
        Lovvalgsperiode periode = new Lovvalgsperiode();
        periode.setBestemmelse(LovvalgBestemmelse_883_2004.FO_883_2004_ART12_1);
        // periode.setFom(LocalDate.now());
        periode.setFom(fom);
        periode.setTom(LocalDate.now());
        return periode;
    }

    private static Avklartefakta lagAvklarteFakta(AvklartefaktaType type, String verdi) {
        return lagAvklarteFakta(type, verdi, "TRUE");
    }

    private static Avklartefakta lagAvklarteFakta(AvklartefaktaType type, String verdi, String status) {
        Avklartefakta faktum = new Avklartefakta();
        faktum.setType(type);
        faktum.setFakta(status);
        faktum.setSubjekt(verdi);
        return faktum;
    }

    private static Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Fagsakstype.EU_EØS);
        return fagsak;
    }

    private static Behandling lagBehandling(Fagsak fagsak) {
        Saksopplysning søknad = new Saksopplysning();
        søknad.setType(SaksopplysningType.SØKNAD);
        SoeknadDokument dokument = new SoeknadDokument();
        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse = new StrukturertAdresse();
        arbeidUtland.adresse.landKode = Landkoder.AT.getKode();
        dokument.arbeidUtland = Arrays.asList(arbeidUtland);
        søknad.setDokument(dokument);
        return lagBehandling(fagsak, Collections.singleton(søknad));
    }

    private static Behandling lagBehandling(Fagsak fagsak, Set<Saksopplysning> saksopplysninger) {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstype.KLAGE);
        behandling.setFagsak(fagsak);
        behandling.setSaksopplysninger(saksopplysninger);
        return behandling;
    }

    private static FellesType lagFellesType() {
        FellesType fellesType = FellesType.builder()
            .withFagsaksnummer("Sak 1")
            .build();
        return fellesType;
    }

    private static MelosysNAVFelles LagMelosysNAVFelles() {
        NavEnhet navEnhet = NavEnhet.builder()
            .withEnhetsId("Enhetsid")
            .withEnhetsNavn("Behandlende Enhet")
            .build();
        Saksbehandler saksbehandler = lagSaksbehandler(navEnhet);
        Person person = lagPerson();
        MelosysNAVFelles navFelles = MelosysNAVFelles.builder()
            .withSakspart(Sakspart.builder()
                .withId("123")
                .withTypeKode(AktoerType.INSTITUSJON)
                .withNavn("Institutten Tei")
                .build())
            .withMottaker(person)
            .withBehandlendeEnhet(navEnhet)
            .withSignerendeSaksbehandler(saksbehandler)
            .withSignerendeBeslutter(saksbehandler)
            .build();
        navFelles.setKontaktinformasjon(lagKontaktInformasjon());
        return navFelles;
    }

    private static Saksbehandler lagSaksbehandler(NavEnhet navEnhet) {
        Saksbehandler saksbehandler = Saksbehandler.builder()
            .withNavAnsatt(NavAnsatt.builder()
                .withAnsattId("Saksbehandler 1")
                .withNavn("Saksbehandler En")
                .build())
            .withNavEnhet(navEnhet)
            .build();
        return saksbehandler;
    }

    private static Person lagPerson() {
        Person person = Person.builder()
            .withId("2")
            .withTypeKode(AktoerType.PERSON)
            .withNavn("Nevn Navnet")
            .withKortNavn("NN")
            .withSpraakkode(Spraakkode.NB)
            .withMottakeradresse(NorskPostadresse.builder()
                .withAdresselinje1("Gate 1")
                .withPostnummer("1234")
                .withPoststed("Poststed")
                .build())
            .build();
        return person;
    }

}
