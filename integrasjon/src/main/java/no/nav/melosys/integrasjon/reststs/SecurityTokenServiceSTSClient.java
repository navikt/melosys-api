package no.nav.melosys.integrasjon.reststs;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.apache.cxf.Bus;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * SOAP-klient som injecter SAML-token fra Security Token Service
 */
@Deprecated
public class SecurityTokenServiceSTSClient extends STSClient {

    private final RestSTSService restSTSService;

    public SecurityTokenServiceSTSClient(Bus bus, RestSTSService restSTSService) {
        super(bus);
        this.restSTSService = restSTSService;
    }

    @Override
    public SecurityToken requestSecurityToken(String appliesTo,
                                              String action,
                                              String requestType,
                                              String binaryExchange) throws Exception {
        SecurityToken token = new SecurityToken();
        token.setToken(lagSamlTokenElement());
        return token;
    }

    private Element lagSamlTokenElement() throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        DocumentBuilder builder;
        Document document;

        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            builder = factory.newDocumentBuilder();
            document = builder.parse(new InputSource(new StringReader(getDecodedSamlToken())));
        } catch (ParserConfigurationException | SAXException e) {
            throw new IllegalStateException("Kunne ikke parse SAML-token", e);
        }

        return document.getDocumentElement();
    }

    private String getDecodedSamlToken() {
        String encodedSamlToken = restSTSService.samlToken();

        return new String(Base64.decodeBase64(encodedSamlToken), StandardCharsets.UTF_8);
    }
}
