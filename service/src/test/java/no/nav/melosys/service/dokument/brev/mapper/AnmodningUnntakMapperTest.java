package no.nav.melosys.service.dokument.brev.mapper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

import com.google.common.collect.BiMap;
import com.google.common.collect.Sets;
import no.nav.dok.brevdata.felles.v1.navfelles.Kontaktinformasjon;
import no.nav.dok.melosysbrev._000084.BestemmelseDetSoekesUnntakFraKode;
import no.nav.dok.melosysbrev._000084.Fag;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art12_1_begrunnelser;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.service.SaksbehandlingDataFactory;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntak;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.ComparisonType;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.DifferenceEvaluators;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse;
import static no.nav.melosys.service.dokument.brev.mapper.AnmodningUnntakMapper.BESTEMMELSE_DET_SOEKES_UNNTAK_FRA_KODE_MAP;
import static no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles;
import static no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactoryTest.lagAlleVilkaarBegrunnelser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.test.util.AssertionErrors.assertFalse;

class AnmodningUnntakMapperTest {

    private AnmodningUnntakMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AnmodningUnntakMapper();
    }

    @Test
    void mapTilBrevXML() throws Exception {
        FellesType fellesType = lagFellesType();

        MelosysNAVFelles navFelles = lagMelosysNAVFelles();

        Behandling behandling = lagBehandling();

        Behandlingsresultat resultat = lagBehandlingsresultat();

        Vilkaarsresultat vilkaarsresultat16_1 = new Vilkaarsresultat();
        vilkaarsresultat16_1.setVilkaar(Vilkaar.FO_883_2004_ART16_1);
        VilkaarBegrunnelse begrunnelse_16_1 = new VilkaarBegrunnelse();
        begrunnelse_16_1.setKode(Art16_1_anmodning.UTSENDELSE_MELLOM_24_MN_OG_5_AAR.getKode());
        vilkaarsresultat16_1.setBegrunnelser(Collections.singleton(begrunnelse_16_1));

        BrevDataAnmodningUnntak brevData = lagBrevData(resultat);

        String xml = mapper.mapTilBrevXML(fellesType, navFelles, behandling, resultat, brevData);
        String expectedXml = hentBrevXmlFraFil("unntakbrev/unntakbrev.xml");


        Diff diff = createDiffIgnoreNameSpace(expectedXml, xml);


        assertFalse(diff.toString(), diff.hasDifferences());
    }

    private static Diff createDiffIgnoreNameSpace(String expectedXml, String testMapTilBrevXml) {
        return DiffBuilder.compare(Input.fromString(expectedXml))
            .withTest(Input.fromString(testMapTilBrevXml))
            .ignoreWhitespace()
            .withDifferenceEvaluator(DifferenceEvaluators.chain(
                DifferenceEvaluators.Default,
                (comparison, outcome) -> {
                    if (comparison.getType() == ComparisonType.NAMESPACE_URI) {
                        Node controlNode = comparison.getControlDetails().getTarget();
                        Node testNode = comparison.getTestDetails().getTarget();
                        if (controlNode != null && testNode != null && controlNode.getNodeType() == Node.ELEMENT_NODE && testNode.getNodeType() == Node.ELEMENT_NODE) {
                            // If both nodes are elements, ignore the namespace URI difference
                            return ComparisonResult.EQUAL;
                        }
                    }
                    // For all other comparisons, return the original outcome
                    return outcome;
                }))
            .checkForSimilar()
            .build();
    }

    private String hentBrevXmlFraFil(String filnavn) throws IOException {
        return new String(getClass().getClassLoader().getResourceAsStream(filnavn).readAllBytes());
    }

    @Test
    void mapTilBrevXML_kodeverkArt16_1_anmodning_valider() throws Exception {
        Behandling behandling = lagBehandling();
        Behandlingsresultat resultat = lagBehandlingsresultat();
        Set<VilkaarBegrunnelse> begrunnelser = lagAlleVilkaarBegrunnelser(Art16_1_anmodning.class);
        for (VilkaarBegrunnelse begrunnelse : begrunnelser) {
            BrevDataAnmodningUnntak brevdata = lagBrevData(resultat);
            brevdata.setAnmodningBegrunnelser(Set.of(begrunnelse));
            assertThatNoException().isThrownBy(() -> mapper.mapFag(behandling, resultat, brevdata));
        }
    }

    @Test
    void mapFag_direkteArt16_forventIkkeNull() {
        Behandling behandling = lagBehandling();
        Behandlingsresultat behandlingsresultat = SaksbehandlingDataFactory.lagBehandlingsresultat();
        BrevDataAnmodningUnntak brevData = lagBrevData(behandlingsresultat, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B3);

        Fag fag = mapper.mapFag(behandling, behandlingsresultat, brevData);
        assertThat(fag.getBestemmelseDetSoekesUnntakFra()).isNotNull();
    }

    @Test
    void mapFag_ikkeDirekteArt16_forventNull() {
        Behandling behandling = lagBehandling();
        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat();
        BrevDataAnmodningUnntak brevData = lagBrevData(behandlingsresultat, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B3);

        Fag fag = mapper.mapFag(behandling, behandlingsresultat, brevData);
        assertThat(fag.getBestemmelseDetSoekesUnntakFra()).isNull();
    }

    @Test
    void mapFag_alleBestemmelserDetSøkesUnntakFra_brukes() {
        final BiMap<BestemmelseDetSoekesUnntakFraKode, LovvalgBestemmelse> bestemmelseDetSoekesUnntakFraBrev =
            BESTEMMELSE_DET_SOEKES_UNNTAK_FRA_KODE_MAP.inverse();
        Arrays.stream(BestemmelseDetSoekesUnntakFraKode.values()).forEach(b -> assertThat(bestemmelseDetSoekesUnntakFraBrev.get(b))
            .describedAs("Bestemmelse %s i brev brukes ikke", b).isNotNull());
    }

    private BrevDataAnmodningUnntak lagBrevData(Behandlingsresultat resultat) {
        return lagBrevData(resultat, null);
    }

    private BrevDataAnmodningUnntak lagBrevData(Behandlingsresultat resultat, LovvalgBestemmelse unntakFraBestemmelse) {
        BrevDataAnmodningUnntak brevData = new BrevDataAnmodningUnntak("Z999999", Landkoder.AT.getBeskrivelse(), new AvklartVirksomhet("Test AS", null, null, Yrkesaktivitetstyper.SELVSTENDIG),
            Yrkesaktivitetstyper.SELVSTENDIG, Collections.emptySet(), Collections.emptySet(), null);
        Anmodningsperiode anmodningsperiode =
            new Anmodningsperiode(LocalDate.now(), LocalDate.now(),
                Land_iso2.NO, null, null, Land_iso2.DK,
                unntakFraBestemmelse, null);
        resultat.setAnmodningsperioder(Sets.newHashSet(anmodningsperiode));

        return brevData;
    }

    private FellesType lagFellesType() {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-1");
        return fellesType;
    }

    private MelosysNAVFelles lagMelosysNAVFelles() {
        MelosysNAVFelles navFelles = lagNAVFelles();
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        Kontaktinformasjon kontaktinformasjon = lagKontaktInformasjon();
        navFelles.setKontaktinformasjon(kontaktinformasjon);
        return navFelles;
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        behandling.setFagsak(fagsak);

        StrukturertAdresse strukturertAdresse = new StrukturertAdresse();
        strukturertAdresse.setLandkode("NO");
        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted(null, strukturertAdresse);

        Soeknad soeknad = new Soeknad();
        soeknad.arbeidPaaLand.setFysiskeArbeidssteder(new ArrayList<>());
        soeknad.arbeidPaaLand.getFysiskeArbeidssteder().add(fysiskArbeidssted);

        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerData(soeknad);
        behandling.setMottatteOpplysninger(mottatteOpplysninger);

        return behandling;
    }

    private Behandlingsresultat lagBehandlingsresultat() {
        Behandlingsresultat resultat = new Behandlingsresultat();
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Land_iso2.NO);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now());
        resultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));

        resultat.setVilkaarsresultater(new HashSet<>());

        Vilkaarsresultat vilkaarsresultat12_1 = new Vilkaarsresultat();
        vilkaarsresultat12_1.setVilkaar(Vilkaar.FO_883_2004_ART12_1);
        vilkaarsresultat12_1.setOppfylt(false);
        VilkaarBegrunnelse begrunnelse12_1 = new VilkaarBegrunnelse();
        begrunnelse12_1.setKode(Art12_1_begrunnelser.IKKE_VESENTLIG_VIRKSOMHET.getKode());
        vilkaarsresultat12_1.setBegrunnelser(Collections.singleton(begrunnelse12_1));
        resultat.getVilkaarsresultater().add(vilkaarsresultat12_1);

        Vilkaarsresultat vilkaarsresultat12_2 = new Vilkaarsresultat();
        vilkaarsresultat12_2.setVilkaar(Vilkaar.FO_883_2004_ART12_2);
        vilkaarsresultat12_2.setOppfylt(true);
        resultat.getVilkaarsresultater().add(vilkaarsresultat12_2);
        return resultat;
    }
}
