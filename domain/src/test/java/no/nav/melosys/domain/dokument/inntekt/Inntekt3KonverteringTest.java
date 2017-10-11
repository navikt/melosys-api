package no.nav.melosys.domain.dokument.inntekt;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;

public class Inntekt3KonverteringTest {

    public static final String INNTEKT_3_2_MOCK = "inntekt/99999999992.xml";

    DokumentFactory factory;

    @Before
    public void setUp() {
        Jaxb2Marshaller marshaller = new JaxbConfig().jaxb2Marshaller();
        XsltTemplatesFactory xsltTemplatesFactory = new XsltTemplatesFactory();
        factory = new DokumentFactory(marshaller, xsltTemplatesFactory);
    }

    @Test
    public void testKonvertering() throws Exception {
        Saksopplysning test = new Saksopplysning();

        InputStream kilde = getClass().getClassLoader().getResourceAsStream(INNTEKT_3_2_MOCK);
        StringBuilder stringBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (kilde, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                stringBuilder.append((char) c);
            }
        }
        test.setDokumentXml(stringBuilder.toString());
        test.setType(SaksopplysningType.INNTEKT);
        test.setVersjon("3.2");

        factory.lagDokument(test);

        InntektDokument dokument = (InntektDokument) test.getDokument();
        assertThat(dokument).isNotNull();
    }


}