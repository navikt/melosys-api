package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import java.util.*;
import javax.xml.bind.JAXBException;

import no.nav.dok.brevdata.felles.v1.navfelles.Kontaktinformasjon;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.soeknad.ArbeidUtland;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art12_1_begrunnelser;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_anmodning;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art16_1_avslag;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagAnmodningsperiodeSvarAvslag;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse;
import static no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles;
import static org.assertj.core.api.Assertions.assertThat;

public class AvslagYrkesaktivMapperTest {

    private FellesType fellesType;
    private Behandling behandling;
    private MelosysNAVFelles navFelles;

    @Before
    public void setUp() {
        fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-1");

        navFelles = lagNAVFelles();
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        Kontaktinformasjon kontaktinformasjon = lagKontaktInformasjon();
        navFelles.setKontaktinformasjon(kontaktinformasjon);

        behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setType(Sakstyper.EU_EOS);
        behandling.setFagsak(fagsak);
    }

    @Test
    public void mapTilBrevXML() throws JAXBException, SAXException, TekniskException {

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

        Vilkaarsresultat vilkaarsresultat12_1 = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART12_1, false, Art12_1_begrunnelser.IKKE_VESENTLIG_VIRKSOMHET);
        resultat.getVilkaarsresultater().add(vilkaarsresultat12_1);

        Vilkaarsresultat vilkaarsresultat12_2 = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART12_2, false);
        resultat.getVilkaarsresultater().add(vilkaarsresultat12_2);

        Vilkaarsresultat vilkaarsresultat16_1 = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART16_1, false,
            Art16_1_avslag.OVER_5_AAR,
            Art16_1_avslag.SOEKT_FOR_SENT,
            Art16_1_avslag.SAERLIG_AVSLAGSGRUNN);
        vilkaarsresultat16_1.setBegrunnelseFritekst("Fritekst");

        BrevDataAnmodningUnntakOgAvslag brevData = new BrevDataAnmodningUnntakOgAvslag("Z999999");
        brevData.arbeidsland = Landkoder.AT.getBeskrivelse();
        brevData.hovedvirksomhet = new AvklartVirksomhet("Test AS", null, null, Yrkesaktivitetstyper.LOENNET_ARBEID);
        brevData.anmodningsperiodeSvar = Optional.empty();
        brevData.yrkesaktivitet = Yrkesaktivitetstyper.LOENNET_ARBEID;
        brevData.art16Vilkaar = Optional.of(vilkaarsresultat16_1);

        AvslagYrkesaktivMapper spy = Mockito.spy(new AvslagYrkesaktivMapper());
        String xml = spy.mapTilBrevXML(fellesType, navFelles, behandling, resultat, brevData);

        assertThat(xml).matches("(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\n.*");
    }

    @Test
    public void mapTilBrevXML_medOppfyltArt16OgAnmodningsperiode_brukerAnmodningsperiode() throws JAXBException, SAXException, TekniskException {
        AvslagYrkesaktivMapper spy = Mockito.spy(new AvslagYrkesaktivMapper());

        BrevDataAnmodningUnntakOgAvslag brevData = new BrevDataAnmodningUnntakOgAvslag("Z999999");
        brevData.arbeidsland = Landkoder.ES.getBeskrivelse();
        brevData.hovedvirksomhet = new AvklartVirksomhet("Test AS", null, null, Yrkesaktivitetstyper.LOENNET_ARBEID);
        brevData.anmodningsperiodeSvar = Optional.of(lagAnmodningsperiodeSvarAvslag());
        brevData.yrkesaktivitet = Yrkesaktivitetstyper.LOENNET_ARBEID;

        Vilkaarsresultat vilkår16_1_oppfylt = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART16_1, true, Art16_1_anmodning.ERSTATTER_EN_ANNEN_UNDER_5_AAR);
        brevData.art16Vilkaar = Optional.of(vilkår16_1_oppfylt);

        Behandlingsresultat resultat = lagBehandlingsresultat();
        Vilkaarsresultat vilkår12_1_avslått = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART12_1, false, Art12_1_begrunnelser.IKKE_VESENTLIG_VIRKSOMHET);
        resultat.getVilkaarsresultater().add(vilkår12_1_avslått);

        String xml = spy.mapTilBrevXML(fellesType, navFelles, behandling, resultat, brevData);
        assertThat(xml).matches("(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\n.*");
    }

    private Vilkaarsresultat lagVilkaarsresultat(Vilkaar vilkaar, boolean oppfylt, Kodeverk... vilkårbegrunnelser) {
        Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
        vilkaarsresultat.setOppfylt(oppfylt);
        vilkaarsresultat.setVilkaar(vilkaar);
        vilkaarsresultat.setBegrunnelser(new HashSet<>());
        for (Kodeverk begrunnelseKode : vilkårbegrunnelser) {
            VilkaarBegrunnelse begrunnelse = new VilkaarBegrunnelse();
            begrunnelse.setKode(begrunnelseKode.getKode());
            vilkaarsresultat.getBegrunnelser().add(begrunnelse);
        }
        return vilkaarsresultat;
    }

    private Behandlingsresultat lagBehandlingsresultat() {
        Behandlingsresultat resultat = new Behandlingsresultat();

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now());
        resultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));
        resultat.setVilkaarsresultater(new HashSet<>());
        return resultat;
    }

}