package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
import no.nav.melosys.domain.dokument.person.KjoennsType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import org.junit.Test;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static org.assertj.core.api.Assertions.assertThat;

public class InnvilgelseArbeidsgiverBrevMapperTest {

    private final InnvilgelseArbeidsgiverMapper instans;

    public InnvilgelseArbeidsgiverBrevMapperTest() {
        instans = new InnvilgelseArbeidsgiverMapper();
    }

    @Test
    public void mapArbeidsLandSammensattNavnLovvalgsperiodeFraSøkandTilBrevXmlGirIkkeTomXmlStreng() throws Exception {
        testMapTilBrevXml(lagBehandlingsresultat(Collections.singleton(lagLovvalgsperiode()),
                Collections.singleton(lagAvklarteFakta())));
    }

    private void testMapTilBrevXml(Behandlingsresultat behandlingsresultat) throws Exception {
        testMapTilBrevXml(lagBehandling(lagFagsak()), behandlingsresultat);
    }

    private void testMapTilBrevXml(Behandling behandling, Behandlingsresultat behandlingsresultat) throws Exception {
        FellesType fellesType = lagFellesType();
        MelosysNAVFelles navFelles = LagMelosysNAVFelles();
        BrevDataInnvilgelse brevDataInnvilgelse = new BrevDataInnvilgelse("Z123456", new BrevbestillingDto());
        brevDataInnvilgelse.arbeidsland = "Sverige";
        brevDataInnvilgelse.lovvalgsperiode = lagLovvalgsperiode();
        String resultat = instans.mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevDataInnvilgelse);
        // TODO: Vurder å bruke XMLUnit e.l. til å sammenlikne XML-strengen
        // grundig mot forventninger.
        assertThat(resultat).matches("(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\n.*");
        assertThat("<ns3:navn>For Etter</ns3:navn>").isSubstringOf(resultat);
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
        periode.setFom(fom);
        periode.setTom(LocalDate.now());
        periode.setLovvalgsland(Landkoder.AT);
        return periode;
    }

    private static Avklartefakta lagAvklarteFakta() {
        Avklartefakta faktum = new Avklartefakta();
        faktum.setType(Avklartefaktatype.AVKLARTE_ARBEIDSGIVER);
        faktum.setFakta("TRUE");
        faktum.setSubjekt("123456789");
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
        pdok.sammensattNavn = "For Etter";
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
