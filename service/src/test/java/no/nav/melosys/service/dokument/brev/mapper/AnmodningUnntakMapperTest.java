package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;
import no.nav.dok.brevdata.felles.v1.navfelles.Kontaktinformasjon;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art12_1_begrunnelser;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntak;
import org.junit.Before;
import org.junit.Test;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse;
import static no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles;
import static no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactoryTest.lagAlleVilkaarBegrunnelser;
import static org.assertj.core.api.Assertions.assertThat;

public class AnmodningUnntakMapperTest {

    private AnmodningUnntakMapper mapper;

    @Before
    public void setUp() {
        mapper = new AnmodningUnntakMapper();
    }

    @Test
    public void mapTilBrevXML() throws Exception {
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

        assertThat(xml).matches("(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\n.*");
        assertThat(":yrkesaktivitet>SELVSTENDIG</ns").isSubstringOf(xml);
        assertThat(Landkoder.AT.getBeskrivelse()).isSubstringOf(xml);
        assertThat(xml).doesNotContain(Landkoder.DK.getKode());
    }

    @Test
    public void mapTilBrevXML_validerKodeverk() throws Exception {
        Behandling behandling = lagBehandling();
        Behandlingsresultat resultat = lagBehandlingsresultat();
        Set<VilkaarBegrunnelse> begrunnelser = lagAlleVilkaarBegrunnelser(Art16_1_anmodning.class);
        for (VilkaarBegrunnelse begrunnelse : begrunnelser) {
            Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
            vilkaarsresultat.setBegrunnelser(Collections.singleton(begrunnelse));
            BrevDataAnmodningUnntak brevdata = lagBrevData(resultat);
            mapper.mapFag(behandling, resultat, brevdata);
        }
    }

    private BrevDataAnmodningUnntak lagBrevData(Behandlingsresultat resultat) {
        BrevDataAnmodningUnntak brevData = new BrevDataAnmodningUnntak("Z999999");
        Anmodningsperiode anmodningsperiode =
            new Anmodningsperiode(LocalDate.now(), LocalDate.now(),
                Landkoder.NO, null, null, Landkoder.DK,
                null, null);
        resultat.setAnmodningsperioder(Sets.newHashSet(anmodningsperiode));

        brevData.hovedvirksomhet = new AvklartVirksomhet("Test AS", null, null, Yrkesaktivitetstyper.SELVSTENDIG);
        brevData.arbeidsland = Landkoder.AT.getBeskrivelse();
        brevData.yrkesaktivitet = Yrkesaktivitetstyper.SELVSTENDIG;
        brevData.anmodningBegrunnelser = Collections.emptySet();
        brevData.anmodningUtenArt12Begrunnelser = Collections.emptySet();
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

        ArbeidUtland arbeidUtland = new ArbeidUtland();
        arbeidUtland.adresse = new StrukturertAdresse();
        arbeidUtland.adresse.landkode = "NO";

        SoeknadDokument soeknadDokument = new SoeknadDokument();
        soeknadDokument.arbeidUtland = new ArrayList<>();
        soeknadDokument.arbeidUtland.add(arbeidUtland);

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(soeknadDokument);
        saksopplysning.setType(SaksopplysningType.SØKNAD);
        behandling.setSaksopplysninger(Collections.singleton(saksopplysning));
        return behandling;
    }

    private Behandlingsresultat lagBehandlingsresultat() {
        Behandlingsresultat resultat = new Behandlingsresultat();
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now());
        resultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));

        resultat.setVilkaarsresultater(new HashSet<>());

        Vilkaarsresultat vilkaarsresultat12_1 = new Vilkaarsresultat();
        vilkaarsresultat12_1.setVilkaar(Vilkaar.FO_883_2004_ART12_1);
        VilkaarBegrunnelse begrunnelse12_1 = new VilkaarBegrunnelse();
        begrunnelse12_1.setKode(Art12_1_begrunnelser.IKKE_VESENTLIG_VIRKSOMHET.getKode());
        vilkaarsresultat12_1.setBegrunnelser(Collections.singleton(begrunnelse12_1));
        resultat.getVilkaarsresultater().add(vilkaarsresultat12_1);

        Vilkaarsresultat vilkaarsresultat12_2 = new Vilkaarsresultat();
        vilkaarsresultat12_2.setVilkaar(Vilkaar.FO_883_2004_ART12_2);
        resultat.getVilkaarsresultater().add(vilkaarsresultat12_2);
        return resultat;
    }
}