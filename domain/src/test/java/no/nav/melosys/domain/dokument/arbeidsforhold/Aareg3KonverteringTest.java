package no.nav.melosys.domain.dokument.arbeidsforhold;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;

public class Aareg3KonverteringTest {

    @Test
    public void transform() throws TransformerFactoryConfigurationError, TransformerException, IOException, JAXBException {
        InputStream xslt = getClass().getClassLoader().getResourceAsStream("aareg/arbeidsforhold_3.0.xslt");
        InputStream kilde = getClass().getClassLoader().getResourceAsStream("arbeidsforhold/99999999999.xml");
        
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
    }
}