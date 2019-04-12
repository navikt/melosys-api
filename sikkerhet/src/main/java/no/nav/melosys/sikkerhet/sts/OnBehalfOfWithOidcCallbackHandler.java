package no.nav.melosys.sikkerhet.sts;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.apache.cxf.ws.security.trust.delegation.DelegationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Inserts the OIDC token string into the xml message under wsse:BinarySecurityToken
 */
public class OnBehalfOfWithOidcCallbackHandler implements CallbackHandler {

    private static final Logger logger = LoggerFactory.getLogger(OnBehalfOfWithOidcCallbackHandler.class);

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof DelegationCallback) {
                DelegationCallback delegationCallback = (DelegationCallback) callback;
                delegationCallback.setToken(getElement());
            } else {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }

    private static Element getElement() throws IOException {
        return lagOnBehalfOfElement();
    }

    private static Element lagOnBehalfOfElement() throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        DocumentBuilder builder;
        Document document;

        try {
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            builder = factory.newDocumentBuilder();
            document = builder.parse(new InputSource(new StringReader(getOnBehalfOfString())));
        } catch (ParserConfigurationException e) {
            logger.error("Exception while getting builder, aborting", e);
            throw new IllegalStateException(e);
        } catch (SAXException e) {
            logger.error("Exception while getting OnBehalfOf element, aborting", e);
            throw new IllegalStateException(e);
        }

        return document.getDocumentElement();
    }

    private static String getOnBehalfOfString() {
        SubjectHandler subjectHandler = SubjectHandler.getInstance();
        String jwt = subjectHandler.getOidcTokenString();
        String base64encodedJTW = Base64.getEncoder().encodeToString(jwt.getBytes(StandardCharsets.UTF_8));
        return "<wsse:BinarySecurityToken" +
                " EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\"" +
                " ValueType=\"urn:ietf:params:oauth:token-type:jwt\"" +
                " xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"" +
                ">"
                + base64encodedJTW
                + "</wsse:BinarySecurityToken>";
    }
}
