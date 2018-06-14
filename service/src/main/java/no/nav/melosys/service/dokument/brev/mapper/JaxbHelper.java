package no.nav.melosys.service.dokument.brev.mapper;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

/**
 * Klassen er tatt fra Foreldrepenger:
 * http://stash.devillo.no/projects/VEDFP/repos/vl-felles-integrasjon/browse/webservice/src/main/java/no/nav/vedtak/felles/integrasjon/felles/ws/JaxbHelper.java
 */
public final class JaxbHelper {
    private static final Map<Class<?>, JAXBContext> CONTEXTS = new ConcurrentHashMap<>(); // NOSONAR
    private static final Map<String, Schema> SCHEMAS = new ConcurrentHashMap<>(); // NOSONAR

    private JaxbHelper() {

    }

    public static String marshalAndValidateJaxb(Object jaxbObject, Map.Entry<Class<?>, Schema> schemaAndClass) throws JAXBException {
        return marshalAndValidateJaxb(jaxbObject, schemaAndClass, true);
    }

    public static String marshalAndValidateJaxb(Object jaxbObject, Map.Entry<Class<?>, Schema> schemaAndClass, boolean formatted)
            throws JAXBException {
        if (!CONTEXTS.containsKey(schemaAndClass.getKey())) {
            CONTEXTS.put(schemaAndClass.getKey(), JAXBContext.newInstance(schemaAndClass.getKey()));
        }
        Marshaller marshaller = CONTEXTS.get(schemaAndClass.getKey()).createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formatted);
        marshaller.setSchema(schemaAndClass.getValue());

        StringWriter writer = new StringWriter();
        marshaller.marshal(jaxbObject, writer);
        return writer.toString();
    }

    public static String marshalAndValidateJaxb(Class<?> clazz, Object jaxbObject, String xsdLocation) throws JAXBException, SAXException {
        if (!CONTEXTS.containsKey(clazz)) {
            CONTEXTS.put(clazz, JAXBContext.newInstance(clazz));
        }
        Marshaller marshaller = CONTEXTS.get(clazz).createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        Schema schema = getSchema(xsdLocation);
        marshaller.setSchema(schema);

        StringWriter writer = new StringWriter();
        marshaller.marshal(jaxbObject, writer);
        return writer.toString();
    }

    public static String marshalAndValidateJaxbWithSchema(Class<?> clazz, Object jaxbObject, Schema schema) throws JAXBException {
        if (!CONTEXTS.containsKey(clazz)) {
            CONTEXTS.put(clazz, JAXBContext.newInstance(clazz));
        }
        Marshaller marshaller = CONTEXTS.get(clazz).createMarshaller();

        if (schema != null) {
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setSchema(schema);
        }

        StringWriter writer = new StringWriter();
        marshaller.marshal(jaxbObject, writer);
        return writer.toString();
    }

    public static String marshalJaxb(Class<?> clazz, Object jaxbObject) throws JAXBException {
        return marshalAndValidateJaxbWithSchema(clazz, jaxbObject, null);
    }

    public static <T> T unmarshalXMLWithStAX(Class<T> clazz, String xml) throws JAXBException, XMLStreamException, SAXException {
        return unmarshalAndValidateXMLWithStAX(clazz, xml, (String) null);
    }

    public static <T> T unmarshalAndValidateXMLWithStAX(Class<T> clazz, String xml, String xsdLocation)
            throws JAXBException, XMLStreamException, SAXException {
        if (!CONTEXTS.containsKey(clazz)) {
            CONTEXTS.put(clazz, JAXBContext.newInstance(clazz));
        }

        Schema schema = null;
        if (xsdLocation != null) {
            schema = getSchema(xsdLocation);
        }

        return unmarshalAndValidateXMLWithStAXProvidingSchema(clazz, xml, schema);
    }

    public static <T> T unmarshalAndValidateXMLWithStAX(Class<T> clazz, String xml, Schema schema)
            throws JAXBException, XMLStreamException {
        if (!CONTEXTS.containsKey(clazz)) {
            CONTEXTS.put(clazz, JAXBContext.newInstance(clazz));
        }

        return unmarshalAndValidateXMLWithStAXProvidingSchema(clazz, xml, schema);
    }

    public static <T> T unmarshalAndValidateXMLWithStAXProvidingSchema(Class<T> clazz, String xml, String xsdLocation) throws SAXException, JAXBException, XMLStreamException {
        Schema schema = getSchema(xsdLocation);
        return unmarshalAndValidateXMLWithStAXProvidingSchema(clazz, xml, schema);
    }

    public static <T> T unmarshalAndValidateXMLWithStAXProvidingSchema(Class<T> clazz, String xml, Schema schema)
            throws JAXBException, XMLStreamException {
        if (!CONTEXTS.containsKey(clazz)) {
            CONTEXTS.put(clazz, JAXBContext.newInstance(clazz));
        }
        Unmarshaller unmarshaller = CONTEXTS.get(clazz).createUnmarshaller();

        if (schema != null) {
            unmarshaller.setSchema(schema);
        }

        Source source = new StreamSource(new StringReader(xml));

        XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(source);

        JAXBElement<T> root = unmarshaller.unmarshal(xmlStreamReader, clazz);

        return root.getValue();
    }

    private static Schema getSchema(String xsdLocation) throws SAXException {
        if (!SCHEMAS.containsKey(xsdLocation)) {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            final String systemId = JaxbHelper.class.getClassLoader().getResource(xsdLocation).toExternalForm();
            final StreamSource source = new StreamSource(systemId);
            SCHEMAS.putIfAbsent(xsdLocation, schemaFactory.newSchema(source));
        }
        return SCHEMAS.get(xsdLocation);
    }

    public static void clear() {
        CONTEXTS.clear();
        SCHEMAS.clear();
    }
}