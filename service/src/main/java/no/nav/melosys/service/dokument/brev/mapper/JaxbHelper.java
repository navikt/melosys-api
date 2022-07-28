package no.nav.melosys.service.dokument.brev.mapper;

import java.io.StringWriter;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.xml.sax.SAXException;

public final class JaxbHelper {

    private static final Logger log = LoggerFactory.getLogger(JaxbHelper.class);
    private static final Map<String, Schema> SCHEMAS = new ConcurrentHashMap<>();

    private JaxbHelper() {
        // Utility class
    }

    private static class Singleton {
        private static final Jaxb2Marshaller INSTANCE = createMarshaller();

        private static Jaxb2Marshaller createMarshaller() {
            Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
            marshaller.setPackagesToScan("no.nav.melosys.domain.dokument", "no.nav.dok.melosysbrev");
            marshaller.setValidationEventHandler(new DefaultValidationEventHandler());
            try {
                marshaller.afterPropertiesSet();
            } catch (Exception e) {
                log.error("Initialsering av Jaxb2Marshaller feilet: ", e);
            }
            return marshaller;
        }
    }

    public static String marshalAndValidate(Object jaxbObject, String xsdLocation) throws JAXBException, SAXException {
        Marshaller marshaller = Singleton.INSTANCE.createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        Schema schema = getSchema(xsdLocation);
        marshaller.setSchema(schema);

        StringWriter writer = new StringWriter();
        marshaller.marshal(jaxbObject, writer);
        return writer.toString();
    }

    private static Schema getSchema(String xsdLocation) throws SAXException {
        if (!SCHEMAS.containsKey(xsdLocation)) {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final URL xsdURL = JaxbHelper.class.getClassLoader().getResource(xsdLocation);
            final StreamSource source = new StreamSource(Objects.requireNonNull(xsdURL).toExternalForm());
            SCHEMAS.putIfAbsent(xsdLocation, schemaFactory.newSchema(source));
        }
        return SCHEMAS.get(xsdLocation);
    }
}
