package no.nav.melosys.service.dokument.brev.mapper;

import java.time.LocalDate;
import java.util.Collections;
import javax.xml.bind.JAXBException;

import no.nav.dok.brevdata.felles.v1.navfelles.Kontaktinformasjon;
import no.nav.dok.melosysbrev.felles.melosys_felles.FellesType;
import no.nav.dok.melosysbrev.felles.melosys_felles.MelosysNAVFelles;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.VilkaarBegrunnelse;
import no.nav.melosys.domain.Vilkaarsresultat;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vilkaar;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art12_1_begrunnelser;
import no.nav.melosys.domain.kodeverk.begrunnelser.Art12_1_vesentlig_virksomhet;
import no.nav.melosys.domain.kodeverk.yrker.Yrkesaktivitetstyper;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.brev.BrevDataAvslagArbeidsgiver;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagKontaktInformasjon;
import static no.nav.melosys.service.dokument.brev.BrevDataUtils.lagNorskPostadresse;
import static no.nav.melosys.service.dokument.brev.mapper.BrevMappingTestUtils.lagNAVFelles;
import static org.assertj.core.api.Assertions.assertThat;

public class AvslagArbeidsgiverMapperTest {

    @Test
    public void mapTilBrevXML() throws JAXBException, SAXException, TekniskException {
        FellesType fellesType = new FellesType();
        fellesType.setFagsaksnummer("MELTEST-2");

        MelosysNAVFelles navFelles = lagNAVFelles();
        navFelles.getMottaker().setMottakeradresse(lagNorskPostadresse());
        Kontaktinformasjon kontaktinformasjon = lagKontaktInformasjon();
        navFelles.setKontaktinformasjon(kontaktinformasjon);

        BrevDataAvslagArbeidsgiver brevData = new BrevDataAvslagArbeidsgiver("Z12345");

        PersonDokument person = new PersonDokument();
        person.sammensattNavn = "Gunnar Granskau";
        brevData.person = person;

        brevData.arbeidsland = "Danmark";

        brevData.hovedvirksomhet = new AvklartVirksomhet("Test AS", "123456789", null, Yrkesaktivitetstyper.SELVSTENDIG);

        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setLovvalgsland(Landkoder.DE);
        lovvalgsperiode.setFom(LocalDate.now());
        lovvalgsperiode.setTom(LocalDate.now());
        brevData.lovvalgsperiode = lovvalgsperiode;

        Vilkaarsresultat vilkaarsresultat12_1 = new Vilkaarsresultat();
        vilkaarsresultat12_1.setVilkaar(Vilkaar.FO_883_2004_ART12_1);
        VilkaarBegrunnelse begrunnelse12_1 = new VilkaarBegrunnelse();
        begrunnelse12_1.setKode(Art12_1_begrunnelser.IKKE_VESENTLIG_VIRKSOMHET.getKode());
        vilkaarsresultat12_1.setBegrunnelser(Collections.singleton(begrunnelse12_1));
        brevData.vilkårbegrunnelser121 = vilkaarsresultat12_1.getBegrunnelser();

        VilkaarBegrunnelse vesentligVirksomhetBegrunnelse = new VilkaarBegrunnelse();
        vesentligVirksomhetBegrunnelse.setKode(Art12_1_vesentlig_virksomhet.FOR_LITE_KONTRAKTER_NORGE.getKode());
        Vilkaarsresultat vesentligVirksomhet = new Vilkaarsresultat();
        vesentligVirksomhet.setVilkaar(Vilkaar.ART12_1_VESENTLIG_VIRKSOMHET);
        vesentligVirksomhet.setBegrunnelser(Collections.singleton(vesentligVirksomhetBegrunnelse));
        brevData.vilkårbegrunnelser121VesentligVirksomhet = vesentligVirksomhet.getBegrunnelser();


        AvslagArbeidsgiverMapper spy = Mockito.spy(new AvslagArbeidsgiverMapper());
        String xml = spy.mapTilBrevXML(fellesType, navFelles, null, null, brevData);

        assertThat(xml).matches("(?s)\\<\\?xml version=\"\\d\\.\\d+\" .*>\n.*");
    }
}
