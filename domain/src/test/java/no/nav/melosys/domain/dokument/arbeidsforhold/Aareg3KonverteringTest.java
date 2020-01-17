package no.nav.melosys.domain.dokument.arbeidsforhold;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.XsltTemplatesFactory;
import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import org.junit.Before;
import org.junit.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import static org.assertj.core.api.Assertions.assertThat;

public class Aareg3KonverteringTest {

    private DokumentFactory factory;

    @Before
    public void setUp() {
        Jaxb2Marshaller marshaller = JaxbConfig.jaxb2Marshaller();
        XsltTemplatesFactory xsltTemplatesFactory = new XsltTemplatesFactory();
        factory = new DokumentFactory(marshaller, xsltTemplatesFactory);
    }

    @Test
    public void transform() throws TransformerFactoryConfigurationError, TransformerException, IOException, JAXBException {
        InputStream xslt = getClass().getClassLoader().getResourceAsStream("aareg/arbeidsforhold_3.0.xslt");
        InputStream kilde = getClass().getClassLoader().getResourceAsStream("arbeidsforhold/99999999999_med_mock.xml");
        
        Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(xslt));
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        StringWriter writer = new StringWriter();
        transformer.transform(new StreamSource(kilde), new StreamResult(writer));

        //System.out.println(writer.toString());
        JAXBContext ctx = JAXBContext.newInstance(ArbeidsforholdDokument.class);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        unmarshaller.setEventHandler(new DefaultValidationEventHandler());

        ArbeidsforholdDokument dokument = (ArbeidsforholdDokument) unmarshaller.unmarshal(new StringReader(writer.toString()));
        assertThat(dokument.getArbeidsforhold()).isNotEmpty();
        assertThat(dokument.getArbeidsforhold().get(0).getArbeidsforholdIDnav()).isEqualTo(19353321);

        for (Arbeidsforhold arbeidsforhold : dokument.getArbeidsforhold()) {
            assertThat(arbeidsforhold.getArbeidsavtaler()).isNotEmpty();
            for (Arbeidsavtale arbeidsavtale : arbeidsforhold.getArbeidsavtaler()) {
                assertThat(arbeidsavtale).isNotNull();
                assertThat(arbeidsavtale.gyldighetsperiode).isNotNull();
                assertThat(arbeidsavtale.gyldighetsperiode.getFom()).isNotNull();
            }
        }
        xslt.close();
        kilde.close();
    }

    @Test
    public void maritimArbeidsavtale() throws IOException {
        final String ressurs = "arbeidsforhold/99999999999_med_mock.xml";
        final InputStream kilde = getClass().getClassLoader().getResourceAsStream(ressurs);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(kilde, StandardCharsets.UTF_8))) {
            Saksopplysning saksopplysning = new Saksopplysning();

            String xmlStr = reader.lines().collect(Collectors.joining(System.lineSeparator()));

            saksopplysning.setDokumentXml(xmlStr);
            saksopplysning.setType(SaksopplysningType.ARBFORH);
            saksopplysning.setVersjon("3.0");

            factory.lagDokument(saksopplysning);

            ArbeidsforholdDokument dokument = (ArbeidsforholdDokument) saksopplysning.getDokument();

            assertThat(dokument).isNotNull();
            assertThat(dokument.getArbeidsforhold()).isNotEmpty();

            for (Arbeidsforhold arbeidsforhold : dokument.getArbeidsforhold()) {

                assertThat(arbeidsforhold.getArbeidsgivertype()).isNotNull();
                assertThat(arbeidsforhold.getArbeidsgiverID()).isNotBlank();
                assertThat(arbeidsforhold.getOpplysningspliktigtype()).isNotNull();
                assertThat(arbeidsforhold.getOpplysningspliktigID()).isNotBlank();
                assertThat(arbeidsforhold.getArbeidsavtaler()).isNotEmpty();

                for (Arbeidsavtale arbeidsavtale : arbeidsforhold.getArbeidsavtaler()) {

                    assertThat(arbeidsavtale).isNotNull();
                    assertThat(arbeidsavtale.maritimArbeidsavtale).isNotNull();
                }
            }
        }
    }
}