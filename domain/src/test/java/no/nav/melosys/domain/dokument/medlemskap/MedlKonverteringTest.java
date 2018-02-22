package no.nav.melosys.domain.dokument.medlemskap;

import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

public class MedlKonverteringTest {

    @Test
    @SuppressWarnings("Duplicates")
    public void transform() throws TransformerFactoryConfigurationError, TransformerException, IOException, JAXBException {
        InputStream xslt = getClass().getClassLoader().getResourceAsStream("medl/medlemskap_2.0.xslt");
        InputStream kilde = getClass().getClassLoader().getResourceAsStream("medlemskap/66666666661.xml");

        Transformer transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(xslt));
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        StringWriter writer = new StringWriter();
        transformer.transform(new StreamSource(kilde), new StreamResult(writer));

        JAXBContext ctx = JAXBContext.newInstance(MedlemskapDokument.class);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        unmarshaller.setEventHandler(new DefaultValidationEventHandler());

        MedlemskapDokument dokument = (MedlemskapDokument) unmarshaller.unmarshal(new StringReader(writer.toString()));
        assertThat(dokument.getMedlemsperiode()).isNotEmpty();

        for (Medlemsperiode medlemsperiode : dokument.getMedlemsperiode()) {
            assertNotNull(medlemsperiode.getType());
            assertNotNull(medlemsperiode.getStatus());
            assertNotNull(medlemsperiode.getLovvalg());
            assertNotNull(medlemsperiode.getKilde());
            assertNotNull(medlemsperiode.getGrunnlagstype());
        }
        kilde.close();
        xslt.close();
    }
}
