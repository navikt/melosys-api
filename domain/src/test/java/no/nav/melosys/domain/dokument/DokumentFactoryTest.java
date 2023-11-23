package no.nav.melosys.domain.dokument;

import java.time.LocalDate;
import java.util.Collections;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import no.nav.melosys.domain.dokument.medlemskap.Periode;
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import static org.assertj.core.api.Assertions.assertThat;

class DokumentFactoryTest {

    DokumentFactory factory;

    @BeforeEach
    void setUp() {
        Jaxb2Marshaller marshaller = JaxbConfig.getJaxb2Marshaller();

        XsltTemplatesFactory xsltTemplatesFactory = new XsltTemplatesFactory();
        factory = new DokumentFactory(marshaller, xsltTemplatesFactory);
    }

    @Test
    void lagSedDokument_xmlBlirProdusert() {
        SedDokument sedDokument = new SedDokument();
        sedDokument.setStatsborgerskapKoder(Collections.singletonList("NO"));
        sedDokument.setLovvalgsperiode(new Periode(LocalDate.now(), LocalDate.now()));
        sedDokument.setLovvalgBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        sedDokument.setErEndring(false);
        sedDokument.setRinaDokumentID("123");
        sedDokument.setRinaSaksnummer("saksnummer123");
        sedDokument.setFnr("333");
        sedDokument.setLovvalgslandKode(Landkoder.DE);

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(sedDokument);

        String xml = factory.lagForenkletXml(saksopplysning);

        saksopplysning.leggTilKildesystemOgMottattDokument(null, xml);
        SaksopplysningDokument saksopplysningDokument = factory.lagDokument(saksopplysning);

        assertThat(xml).isNotNull();
        assertThat(saksopplysningDokument).isNotNull();


    }

}
