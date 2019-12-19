package no.nav.melosys.service.dokument.brev.mapper;

import java.io.StringWriter;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import no.nav.melosys.domain.dokument.jaxb.JaxbConfig;
import org.xml.sax.SAXException;

public final class JaxbHelper {
    private static final Map<String, Schema> SCHEMAS = new ConcurrentHashMap<>();

    private JaxbHelper() {
        // Utility class
    }

    public static String marshalAndValidate(Object jaxbObject, String xsdLocation) throws JAXBException, SAXException {
        Marshaller marshaller = JaxbConfig.jaxb2Marshaller().createMarshaller();

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