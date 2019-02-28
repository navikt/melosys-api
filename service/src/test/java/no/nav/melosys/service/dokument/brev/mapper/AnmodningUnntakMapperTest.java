package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;

import no.nav.dok.brevdata.felles.v1.navfelles.Kontaktinformasjon;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;
import no.nav.melosys.service.dokument.felles.AvklartVirksomhet;
import org.junit.Before;
import org.junit.Test;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse;
import static no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles;
import static org.assertj.core.api.Assertions.assertThat;

public class AnmodningUnntakMapperTest {

    private AnmodningUnntakMapper mapper;

    @Before
    public void setUp() {
        mapper = new AnmodningUnntakMapper();
    }

    @Test
    public void mapTilBrevXML() throws Exception {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-1");

        MelosysNAVFelles navFelles = lagNAVFelles();
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        Kontaktinformasjon kontaktinformasjon = lagKontaktInformasjon();
        navFelles.setKontaktinformasjon(kontaktinformasjon);

        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        behandling.setFagsak(fagsak);

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(new SoeknadDokument());
        saksopplysning.setType(SaksopplysningType.SØKNAD);
        behandling.setSaksopplysninger(Collections.singleton(saksopplysning));

        Behandlingsresultat resultat = new Behandlingsresultat();

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setUnntakFraLovvalgsland(Landkoder.DE);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now());
        resultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));

        resultat.setVilkaarsresultater(new HashSet<>());

        Vilkaarsresultat vilkaarsresultat12_1 = new Vilkaarsresultat();
        vilkaarsresultat12_1.setVilkaar(Vilkaar.FO_883_2004_ART12_1);
        VilkaarBegrunnelse begrunnelse12_1 = new VilkaarBegrunnelse();
        begrunnelse12_1.setKode(Art12_1_Begrunnelser.IKKE_VESENTLIG_VIRKSOMHET.getKode());
        vilkaarsresultat12_1.setBegrunnelser(Collections.singleton(begrunnelse12_1));
        resultat.getVilkaarsresultater().add(vilkaarsresultat12_1);

        Vilkaarsresultat vilkaarsresultat12_2 = new Vilkaarsresultat();
        vilkaarsresultat12_2.setVilkaar(Vilkaar.FO_883_2004_ART12_2);
        resultat.getVilkaarsresultater().add(vilkaarsresultat12_2);

        Vilkaarsresultat vilkaarsresultat16_1 = new Vilkaarsresultat();
        vilkaarsresultat16_1.setVilkaar(Vilkaar.FO_883_2004_ART16_1);
        VilkaarBegrunnelse begrunnelse_16_1 = new VilkaarBegrunnelse();
        begrunnelse_16_1.setKode(Art16_1_Anmodning_Begrunnelser.UTSENDELSE_MELLOM_24_MN_OG_5_AAR.getKode());
        vilkaarsresultat16_1.setBegrunnelser(Collections.singleton(begrunnelse_16_1));
        resultat.getVilkaarsresultater().add(vilkaarsresultat16_1);

        BrevDataAnmodningUnntakOgAvslag brevData = new BrevDataAnmodningUnntakOgAvslag("Z999999");
        brevData.hovedvirksomhet = new AvklartVirksomhet("Test AS", null, null, Yrkesaktivitetstyper.SELVSTENDIG);

        String xml = mapper.mapTilBrevXML(fellesType, navFelles, behandling, resultat, brevData);

        assertThat(xml).matches("(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\n.*");
        assertThat("<ns3:yrkesaktivitet>SELVSTENDIG</ns3:yrkesaktivitet>").isSubstringOf(xml);
    }
}