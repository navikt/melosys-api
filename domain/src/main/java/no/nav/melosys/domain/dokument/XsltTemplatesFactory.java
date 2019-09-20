package no.nav.melosys.domain.dokument;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import no.nav.melosys.domain.SaksopplysningType;
import org.springframework.stereotype.Component;

/**
 * Klassen er en cache for å gjenbruke Templates, "kompilert" xslt.
 */
@Component
public class XsltTemplatesFactory {
    private final TransformerFactory transformerFactory;
    private final Map<String, Templates> cache;

    public XsltTemplatesFactory() {
        this.cache = new HashMap<>();
        this.transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    }

    /**
     * Returnerer bearbeidet xslt ({@code Templates}) for en angitt type saksopplysning og en angitt tjenesteversjon.
     * F.eks. getXsltTemplates med parametere {@code SaksopplysningType.PERSOPL} og versjon {@code 3.0}
     * returnerer xslt for TPS tjenesten Person_v3.0.
     *
     * @param type Den typen saksopplysning
     * @param versjon Tjenesteversjonen
     */
    public Templates getXsltTemplates(SaksopplysningType type, String versjon) throws TransformerConfigurationException {
        String xsltPath = XsltConfig.getXsltPath(type, versjon);

        Templates templates = cache.get(xsltPath);

        if (templates == null) {
            synchronized (this) {
                InputStream is = getClass().getClassLoader().getResourceAsStream(xsltPath);
                templates = transformerFactory.newTemplates(new StreamSource(is));
                cache.put(xsltPath, templates);
            }
        }

        return templates;
    }

}
