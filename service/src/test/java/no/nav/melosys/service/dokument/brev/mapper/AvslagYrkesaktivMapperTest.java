package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.JAXBException;

import com.google.common.collect.Sets;
import no.nav.dok.brevdata.felles.v1.navfelles.Kontaktinformasjon;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;
import org.junit.Test;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse;
import static no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles;
import static org.assertj.core.api.Assertions.assertThat;

public class AvslagYrkesaktivMapperTest {

    @Test
    public void mapTilBrevXML() throws JAXBException, SAXException, TekniskException {
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

        Behandlingsresultat resultat = new Behandlingsresultat();

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now());
        resultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));

        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setUnntakFraLovvalgsland(Landkoder.DE);

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
        vilkaarsresultat16_1.setBegrunnelseFritekst("Fritekst");
        VilkaarBegrunnelse a_begrunnelse_16_1 = new VilkaarBegrunnelse();
        a_begrunnelse_16_1.setKode(Art16_1_Avslag__Begrunnelser.OVER_5_AAR.getKode());
        VilkaarBegrunnelse b_begrunnelse_16_1 = new VilkaarBegrunnelse();
        b_begrunnelse_16_1.setKode(Art16_1_Avslag__Begrunnelser.SOEKT_FOR_SENT.getKode());
        VilkaarBegrunnelse c_begrunnelse_16_1 = new VilkaarBegrunnelse();
        c_begrunnelse_16_1.setKode(Art16_1_Avslag__Begrunnelser.SAERLIG_AVSLAGSGRUNN.getKode());

        Set<VilkaarBegrunnelse> begrunnelser_16_1 = new HashSet<>();
        begrunnelser_16_1.add(a_begrunnelse_16_1);
        begrunnelser_16_1.add(b_begrunnelse_16_1);
        begrunnelser_16_1.add(c_begrunnelse_16_1);
        vilkaarsresultat16_1.setBegrunnelser(begrunnelser_16_1);
        resultat.getVilkaarsresultater().add(vilkaarsresultat16_1);

        BrevDataAnmodningUnntakOgAvslag brevData = new BrevDataAnmodningUnntakOgAvslag("Z999999");
        brevData.arbeidsland = Landkoder.AT.getBeskrivelse();
        resultat.setAnmodningsperioder(Sets.newHashSet(anmodningsperiode));
        brevData.hovedvirksomhet = new AvklartVirksomhet("Test AS", null, null, Yrkesaktivitetstyper.LOENNET_ARBEID);

        AvslagYrkesaktivMapper spy = Mockito.spy(new AvslagYrkesaktivMapper());
        String xml = spy.mapTilBrevXML(fellesType, navFelles, behandling, resultat, brevData);

        assertThat(xml).matches("(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\n.*");
    }
}