package no.nav.melosys.domain.dokument;

import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public final class XmlFormaterer {

    private XmlFormaterer() {}

    private static final Logger log = LoggerFactory.getLogger(XmlFormaterer.class);

    private static DocumentBuilder documentBuilder;

    public static String formaterXml(String xmlString) {
        StringWriter stringWriter = new StringWriter();
        try {
            DocumentBuilder documentBuilder = getDocumentBuilder();
            Document document = documentBuilder.parse(new InputSource(new StringReader(xmlString)));
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            Result result = new StreamResult(stringWriter);
            Source source = new DOMSource(document);
            transformer.transform(source, result);
        } catch (Exception e) {
            log.warn("Kunne ikke formatere xml", e);
            return xmlString;
        }

        return stringWriter.toString();
    }

    @SuppressWarnings("squid:S2755")
    private static DocumentBuilder getDocumentBuilder() {
        if (documentBuilder == null) {
            try {
                documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new IllegalStateException("Kan ikke instansiere type DocumentBuilder", e);
            }
        }
        return documentBuilder;
    }
}
