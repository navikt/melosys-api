package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import java.util.*;

import no.nav.dok.brevdata.felles.v1.navfelles.*;
import no.nav.dok.brevdata.felles.v1.simpletypes.AktoerType;
import no.nav.dok.brevdata.felles.v1.simpletypes.Spraakkode;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.KjoennKode;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.person.Bostedsadresse;
import no.nav.melosys.domain.dokument.person.Gateadresse;
import no.nav.melosys.domain.dokument.person.KjoennsType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataVedlegg;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import org.junit.Test;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static no.nav.melosys.service.dokument.brev.mapper.A1MapperTest.lagPersonDokument;
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
    public final void mapTilBrevXmlMedFlereLovvalgsperioderFeiler() {
        Set<Lovvalgsperiode> perioder = new HashSet<>(Arrays.asList(lagLovvalgsperiode(LocalDate.MIN), lagLovvalgsperiode(LocalDate.now())));
        Throwable unntak = catchThrowable(() -> testMapTilBrevXml(lagBehandlingsresultat(perioder)));
        assertThat(unntak).isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("perioder (2) ulik 1 støttes ikke");
    }

    @Test
    public void mapArbeidslandFraSøknadsTilBrevXmlGirIkkeTomXmlStreng() throws Exception {
        testMapTilBrevXml(lagBehandlingsresultat(Collections.singleton(lagLovvalgsperiode()),
                Collections.singleton(lagAvklarteFakta(Avklartefaktatype.AVKLARTE_ARBEIDSGIVER, "123456789"))));
    }

    @Test
    public void mapTilBrevXmlUtenArbeidslandISøknadGirUnntak() {
        Behandling behandlingUtenSaksopplysninger = lagBehandling(lagFagsak(), Collections.emptySet());
        Behandlingsresultat behandlingsresultatUtenAvklartArbeidsland = lagBehandlingsresultat(Collections.singleton(lagLovvalgsperiode()),
                Collections.singleton(lagAvklarteFakta(Avklartefaktatype.AVKLARTE_ARBEIDSGIVER, "123456789")));
        Throwable unntak = catchThrowable(() -> testMapTilBrevXml(behandlingUtenSaksopplysninger,
                behandlingsresultatUtenAvklartArbeidsland));
        assertThat(unntak).isInstanceOf(TekniskException.class)
            .hasMessageContaining("Finner ikke søknaddokument");
    }

    private void testMapTilBrevXml() throws Exception {
        testMapTilBrevXml(lagBehandlingsresultat());
    }

    private void testMapTilBrevXml(Behandlingsresultat behandlingsresultat) throws Exception {
        testMapTilBrevXml(lagBehandling(lagFagsak()), behandlingsresultat);
    }

    private void testMapTilBrevXml(Behandling behandling, Behandlingsresultat behandlingsresultat) throws Exception {
        FellesType fellesType = lagFellesType();
        MelosysNAVFelles navFelles = LagMelosysNAVFelles();
        BrevDataA1 brevdataA1 = new BrevDataA1();
        AvklartVirksomhet virksomhet = new AvklartVirksomhet("Virker ikke", "123456789", lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID);
        brevdataA1.norskeVirksomheter = new ArrayList<>(Arrays.asList(virksomhet, virksomhet));
        brevdataA1.bostedsadresse = lagBostedsadresse();
        brevdataA1.yrkesgruppe = Yrkesgrupper.FLYENDE_PERSONELL;
        brevdataA1.selvstendigeForetak = Collections.emptySet();
        brevdataA1.utenlandskeVirksomheter = Collections.emptyList();
        brevdataA1.person = lagPersonDokument();
        brevdataA1.hovedvirksomhet = virksomhet;
        brevdataA1.arbeidssteder = new ArrayList<>();
        BrevDataVedlegg brevVedlegg = new BrevDataVedlegg("SAKSBEHANDLER");
        brevVedlegg.brevDataA1 = brevdataA1;

        String resultat = instans.mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevVedlegg);
        // TODO: Vurder å bruke XMLUnit e.l. til å sammenlikne XML-strengen
        // grundig mot forventninger.
        assertThat(resultat).matches("(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\n.*");
    }

    private static Bostedsadresse lagBostedsadresse() {
        Bostedsadresse adresse = new Bostedsadresse();
        Gateadresse gateadresse = new Gateadresse();
        gateadresse.setGatenavn("Gate");
        adresse.setGateadresse(gateadresse);
        adresse.setPostnr("1234");
        adresse.setPoststed("Sted");
        adresse.setLand(new Land(Land.BULGARIA));
        return adresse;
    }

    private static StrukturertAdresse lagStrukturertAdresse() {
        StrukturertAdresse vadr = new StrukturertAdresse();
        vadr.gatenavn = "Gate";
        vadr.landKode = Land.BULGARIA;
        vadr.poststed = "Sted";
        vadr.postnummer = "4321";
        return vadr;
    }

    private static Behandlingsresultat lagBehandlingsresultat() {
        Set<Avklartefakta> fakta = new HashSet<>(Arrays.asList(
                lagAvklarteFakta(Avklartefaktatype.AVKLARTE_ARBEIDSGIVER, "123456789"),
                lagAvklarteFakta(Avklartefaktatype.ARBEIDSLAND, "SE")));
        return lagBehandlingsresultat(Collections.singleton(lagLovvalgsperiode()), fakta);
    }

    private static Behandlingsresultat lagBehandlingsresultat(Set<Lovvalgsperiode> perioder) {
        Set<Avklartefakta> fakta = new HashSet<>(Arrays.asList(
                lagAvklarteFakta(Avklartefaktatype.AVKLARTE_ARBEIDSGIVER, "123456789"),
                lagAvklarteFakta(Avklartefaktatype.ARBEIDSLAND, "SE")));
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
        periode.setBestemmelse(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1);
        // periode.setFom(LocalDate.now());
        periode.setFom(fom);
        periode.setTom(LocalDate.now());
        periode.setLovvalgsland(Landkoder.AT);
        return periode;
    }

    private static Avklartefakta lagAvklarteFakta(Avklartefaktatype type, String verdi) {
        Avklartefakta faktum = new Avklartefakta();
        faktum.setType(type);
        faktum.setFakta("TRUE");
        faktum.setSubjekt(verdi);
        return faktum;
    }

    private static Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        return fagsak;
    }

    private static Behandling lagBehandling(Fagsak fagsak) {
        PersonDokument pdok = new PersonDokument();
        pdok.kjønn = new KjoennsType();
        pdok.kjønn.setKode(KjoennKode.U.name());
        pdok.fornavn = "For";
        pdok.etternavn = "Etter";
        pdok.statsborgerskap = new Land(Land.BELGIA);
        pdok.fødselsdato = LocalDate.ofYearDay(1900, 1);
        Saksopplysning søknad = lagSoeknadssaksopplysning();
        return lagBehandling(fagsak, new HashSet<>(Arrays.asList(søknad, lagSaksopplysning(SaksopplysningType.PERSONOPPLYSNING, pdok))));
    }

    private static Saksopplysning lagSoeknadssaksopplysning() {
        return lagSaksopplysning(SaksopplysningType.SØKNAD, lagSoeknadDokument());
    }

    private static SoeknadDokument lagSoeknadDokument() {
        SoeknadDokument dokument = new SoeknadDokument();
        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse = new StrukturertAdresse();
        arbeidUtland.adresse.landKode = Landkoder.AT.getKode();
        dokument.arbeidUtland = Collections.singletonList(arbeidUtland);
        return dokument;
    }

    private static Saksopplysning lagSaksopplysning(SaksopplysningType type, SaksopplysningDokument dokument) {
        Saksopplysning søknad = new Saksopplysning();
        søknad.setType(type);
        søknad.setDokument(dokument);
        return søknad;
    }

    private static Behandling lagBehandling(Fagsak fagsak, Set<Saksopplysning> saksopplysninger) {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.KLAGE);
        behandling.setFagsak(fagsak);
        behandling.setSaksopplysninger(saksopplysninger);
        return behandling;
    }

    private static FellesType lagFellesType() {
        return FellesType.builder()
            .withFagsaksnummer("Sak 1")
            .build();
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
        return Saksbehandler.builder()
            .withNavAnsatt(NavAnsatt.builder()
                .withAnsattId("Saksbehandler 1")
                .withNavn("Saksbehandler En")
                .build())
            .withNavEnhet(navEnhet)
            .build();
    }

    private static Person lagPerson() {
        return Person.builder()
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
    }
}
