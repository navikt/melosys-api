package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import java.util.*;
import jakarta.xml.bind.JAXBException;

import no.nav.dok.brevdata.felles.v1.navfelles.Kontaktinformasjon;
import no.nav.dok.melosysbrev._000081.Fag;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.begrunnelser.Utsendt_arbeidstaker_begrunnelser;
import no.nav.melosys.domain.kodeverk.begrunnelser.Anmodning_begrunnelser;
import no.nav.melosys.domain.kodeverk.begrunnelser.Avslag_anmodning_begrunnelser;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger;
import no.nav.melosys.domain.mottatteopplysninger.Soeknad;
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted;
import no.nav.melosys.service.dokument.brev.BrevDataAvslagYrkesaktiv;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.BrevDataTestUtils.lagAnmodningsperiodeSvarAvslag;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse;
import static no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles;
import static no.nav.melosys.service.dokument.brev.mapper.felles.VilkaarbegrunnelseFactoryTest.lagAlleVilkaarBegrunnelser;
import static org.assertj.core.api.Assertions.assertThat;

class AvslagYrkesaktivMapperTest {

    private FellesType fellesType;
    private Behandling behandling;
    private MelosysNAVFelles navFelles;

    @BeforeEach
    public void setUp() {
        fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-1");

        navFelles = lagNAVFelles();
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        Kontaktinformasjon kontaktinformasjon = lagKontaktInformasjon();
        navFelles.setKontaktinformasjon(kontaktinformasjon);

        behandling = BehandlingTestFactory.builderWithDefaults().build();
        Fagsak fagsak = FagsakTestFactory.lagFagsak();
        behandling.setFagsak(fagsak);
    }

    @Test
    void mapTilBrevXML() throws JAXBException, SAXException {

        FysiskArbeidssted fysiskArbeidssted = new FysiskArbeidssted("NO", new StrukturertAdresse());

        Soeknad soeknad = new Soeknad();
        soeknad.arbeidPaaLand.setFysiskeArbeidssteder(new ArrayList<>());
        soeknad.arbeidPaaLand.getFysiskeArbeidssteder().add(fysiskArbeidssted);

        MottatteOpplysninger mottatteOpplysninger = new MottatteOpplysninger();
        mottatteOpplysninger.setMottatteOpplysningerData(soeknad);
        behandling.setMottatteOpplysninger(mottatteOpplysninger);

        Behandlingsresultat resultat = new Behandlingsresultat();

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Land_iso2.NO);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now());
        resultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));

        resultat.setVilkaarsresultater(new HashSet<>());

        Vilkaarsresultat vilkaarsresultat12_1 = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART12_1, false, Utsendt_arbeidstaker_begrunnelser.IKKE_VESENTLIG_VIRKSOMHET);
        resultat.getVilkaarsresultater().add(vilkaarsresultat12_1);

        Vilkaarsresultat vilkaarsresultat12_2 = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART12_2, false);
        resultat.getVilkaarsresultater().add(vilkaarsresultat12_2);

        Vilkaarsresultat vilkaarsresultat16_1 = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART16_1, false,
            Avslag_anmodning_begrunnelser.OVER_5_AAR,
            Avslag_anmodning_begrunnelser.SOEKT_FOR_SENT,
            Avslag_anmodning_begrunnelser.SAERLIG_AVSLAGSGRUNN);
        vilkaarsresultat16_1.setBegrunnelseFritekst("Fritekst");

        BrevDataAvslagYrkesaktiv brevData = new BrevDataAvslagYrkesaktiv(new BrevbestillingDto(), "Z999999");
        brevData.setArbeidsland(Landkoder.AT.getBeskrivelse());
        brevData.setHovedvirksomhet(new AvklartVirksomhet("Test AS", null, null, Yrkesaktivitetstyper.LOENNET_ARBEID));
        brevData.setAnmodningsperiodeSvar(new AnmodningsperiodeSvar());
        brevData.setYrkesaktivitet(Yrkesaktivitetstyper.LOENNET_ARBEID);
        brevData.setArt16Vilkaar(vilkaarsresultat16_1);

        AvslagYrkesaktivMapper spy = Mockito.spy(new AvslagYrkesaktivMapper());
        String xml = spy.mapTilBrevXML(fellesType, navFelles, behandling, resultat, brevData);

        assertThat(xml).matches("(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\n.*");
    }

    @Test
    void mapTilBrevXML_medOppfyltArt16OgAnmodningsperiode_brukerAnmodningsperiode() {
        AvslagYrkesaktivMapper spy = Mockito.spy(new AvslagYrkesaktivMapper());

        BrevDataAvslagYrkesaktiv brevData = new BrevDataAvslagYrkesaktiv(new BrevbestillingDto(), "Z999999");
        brevData.setArbeidsland(Landkoder.ES.getBeskrivelse());
        brevData.setHovedvirksomhet(new AvklartVirksomhet("Test AS", null, null, Yrkesaktivitetstyper.LOENNET_ARBEID));
        brevData.setAnmodningsperiodeSvar(lagAnmodningsperiodeSvarAvslag());
        brevData.setYrkesaktivitet(Yrkesaktivitetstyper.LOENNET_ARBEID);

        Vilkaarsresultat vilkår16_1_oppfylt = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART16_1, true, Anmodning_begrunnelser.ERSTATTER_EN_ANNEN_UNDER_5_AAR);
        brevData.setArt16Vilkaar(vilkår16_1_oppfylt);

        Behandlingsresultat resultat = lagBehandlingsresultat();
        Vilkaarsresultat vilkår12_1_avslått = lagVilkaarsresultat(Vilkaar.FO_883_2004_ART12_1, false, Utsendt_arbeidstaker_begrunnelser.IKKE_VESENTLIG_VIRKSOMHET);
        resultat.getVilkaarsresultater().add(vilkår12_1_avslått);

        String xml = spy.mapTilBrevXML(fellesType, navFelles, behandling, resultat, brevData);
        assertThat(xml).matches("(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\n.*");
    }

    @Test
    void mapTilBrevXml_kanMappeAlleKodeverksverdierForArt16_1_avslag() throws Exception {
        AvslagYrkesaktivMapper spy = Mockito.spy(new AvslagYrkesaktivMapper());
        BrevDataAvslagYrkesaktiv brevdata = new BrevDataAvslagYrkesaktiv(new BrevbestillingDto(), "");
        Set<VilkaarBegrunnelse> begrunnelser = lagAlleVilkaarBegrunnelser(Avslag_anmodning_begrunnelser.class);
        for (VilkaarBegrunnelse begrunnelse : begrunnelser) {
            Vilkaarsresultat vilkaarsresultat = new Vilkaarsresultat();
            vilkaarsresultat.setBegrunnelser(Collections.singleton(begrunnelse));
            vilkaarsresultat.setBegrunnelseFritekst("Fritekst");
            brevdata.setArt16Vilkaar(vilkaarsresultat);
            spy.mapArt161Avslag(new Fag(), brevdata);
        }
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
        lovvalgsperiode.setLovvalgsland(Land_iso2.NO);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now());
        resultat.setLovvalgsperioder(Collections.singleton(lovvalgsperiode));
        resultat.setVilkaarsresultater(new HashSet<>());
        return resultat;
    }

}
