package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import java.util.*;

import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.MaritimtArbeid;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.BrevDataInnvilgelse;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import org.junit.Test;

import static no.nav.melosys.service.dokument.brev.mapper.A1MapperTest.lagPersonDokument;
import static no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagFellesType;
import static no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles;
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
    public void mapArbeidslandFraSøknadsTilBrevXmlGirIkkeTomXmlStreng() throws Exception {
        testMapTilBrevXml(lagBehandlingsresultat(Collections.singleton(lagLovvalgsperiode()),
                Collections.singleton(lagAvklarteFakta(Avklartefaktatype.VIRKSOMHET, "123456789"))));
    }

    @Test
    public void mapTilBrevXmlUtenArbeidslandISøknadGirUnntak() {
        Behandling behandlingUtenSaksopplysninger = lagBehandling(lagFagsak(), Collections.emptySet());
        Behandlingsresultat behandlingsresultatUtenAvklartArbeidsland = lagBehandlingsresultat(Collections.singleton(lagLovvalgsperiode()),
                Collections.singleton(lagAvklarteFakta(Avklartefaktatype.VIRKSOMHET, "123456789")));
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
        MelosysNAVFelles navFelles = lagNAVFelles();
        BrevDataA1 brevdataA1 = new BrevDataA1();
        AvklartVirksomhet virksomhet = new AvklartVirksomhet("Virker ikke", "123456789", lagStrukturertAdresse(), Yrkesaktivitetstyper.LOENNET_ARBEID);
        brevdataA1.norskeVirksomheter = new ArrayList<>(Arrays.asList(virksomhet, virksomhet));
        brevdataA1.bostedsadresse = lagStrukturertAdresse();
        brevdataA1.yrkesgruppe = Yrkesgrupper.FLYENDE_PERSONELL;
        brevdataA1.selvstendigeForetak = Collections.emptySet();
        brevdataA1.utenlandskeVirksomheter = Collections.emptyList();
        brevdataA1.person = lagPersonDokument();
        brevdataA1.hovedvirksomhet = virksomhet;
        brevdataA1.arbeidssteder = new ArrayList<>();
        BrevDataInnvilgelse brevdataInnvilgelse = new BrevDataInnvilgelse(new BrevbestillingDto(),"SAKSBEHANDLER");
        brevdataInnvilgelse.vedleggA1 = brevdataA1;
        brevdataInnvilgelse.lovvalgsperiode = lagLovvalgsperiode();
        brevdataInnvilgelse.avklartMaritimType = Maritimtyper.SKIP;
        brevdataInnvilgelse.norskeVirksomheter = brevdataA1.norskeVirksomheter;
        brevdataInnvilgelse.arbeidsland = "Sverige";
        brevdataInnvilgelse.trygdemyndighetsland = "Sverige";

        String resultat = instans.mapTilBrevXML(fellesType, navFelles, behandling, behandlingsresultat, brevdataInnvilgelse);
        // TODO: Vurder å bruke XMLUnit e.l. til å sammenlikne XML-strengen
        // grundig mot forventninger.
        assertThat(resultat).matches("(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\n.*");
    }

    private static StrukturertAdresse lagStrukturertAdresse() {
        StrukturertAdresse vadr = new StrukturertAdresse();
        vadr.gatenavn = "Gate";
        vadr.husnummer = "12B";
        vadr.poststed = "Sted";
        vadr.postnummer = "4321";
        vadr.landkode = Landkoder.BG.getKode();
        return vadr;
    }

    private static Behandlingsresultat lagBehandlingsresultat() {
        Set<Avklartefakta> fakta = new HashSet<>(Arrays.asList(
                lagAvklarteFakta(Avklartefaktatype.VIRKSOMHET, "123456789"),
                lagAvklarteFakta(Avklartefaktatype.ARBEIDSLAND, "SE")));
        return lagBehandlingsresultat(Collections.singleton(lagLovvalgsperiode()), fakta);
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
        periode.setTilleggsbestemmelse(TilleggsBestemmelser_883_2004.FO_883_2004_ART11_4_1);
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
        Saksopplysning søknad = lagSoeknadssaksopplysning();
        return lagBehandling(fagsak, Collections.singleton(søknad));
    }

    private static Saksopplysning lagSoeknadssaksopplysning() {
        return lagSaksopplysning(SaksopplysningType.SØKNAD, lagSoeknadDokument());
    }

    private static SoeknadDokument lagSoeknadDokument() {
        SoeknadDokument dokument = new SoeknadDokument();
        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse = new StrukturertAdresse();
        arbeidUtland.adresse.landkode = Landkoder.AT.getKode();
        dokument.arbeidUtland = Collections.singletonList(arbeidUtland);
        dokument.maritimtArbeid.add(lagMaritimtArbeidUtenFartsområde());
        return dokument;
    }

    private static MaritimtArbeid lagMaritimtArbeidUtenFartsområde() {
        MaritimtArbeid maritimtArbeid = new MaritimtArbeid();
        maritimtArbeid.navn = "Dunfjæder";
        maritimtArbeid.installasjonsLandkode = "NO";
        return maritimtArbeid;
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
}
