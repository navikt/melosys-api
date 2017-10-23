package no.nav.melosys.domain.dokument;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;

/**
 * SaksopplysningDokumentFactory konverterer et xml-resultat fra en ekstern tjeneste til et internt xml-dokument ved hejlp av XSLT med JAXP.
 * Xml-dokumentet blir deretter transformert med JAXB til et objekttre som tilhører et sentralt domene.
 * Klassen er ikke trådsikker.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DokumentFactory {

    private static final Logger log = LoggerFactory.getLogger(DokumentFactory.class);

    // Spring JAXB 2 marshaller og unmarshaller
    private Jaxb2Marshaller marshaller;

    private XsltTemplatesFactory xsltTemplatesFactory;

    @Autowired
    public DokumentFactory(Jaxb2Marshaller marshaller, XsltTemplatesFactory xsltTemplatesFactory) {
        this.marshaller = marshaller;
        this.xsltTemplatesFactory = xsltTemplatesFactory;
    }

    /**
     * lagDokument setter et {@link SaksopplysningDokument} på en {@link Saksopplysning} ut fra feltet {@code dokumentXml}.
     * SaksopplysningDokumentet returneres.
     */
    public SaksopplysningDokument lagDokument(Saksopplysning saksopplysning) {
        Assert.notNull(saksopplysning, "saksopplysning må ikke være null");

        String dokumentXml = saksopplysning.getDokumentXml();
        if (dokumentXml == null) {
            saksopplysning.setDokument(null);
            return null;
        }

        SaksopplysningType type = saksopplysning.getType();
        String versjon = saksopplysning.getVersjon();

        // {@code dokumentXml} transformeres med en JAXP Transformer
        StreamResult outputTarget = new StreamResult(new StringWriter());
        try {
            Templates templates = xsltTemplatesFactory.getXsltTemplates(type, versjon);
            Transformer transformer = templates.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());

            StreamSource xmlSource = new StreamSource(new StringReader(dokumentXml));
            transformer.transform(xmlSource, outputTarget);
        } catch (TransformerException e) {
            log.error("XSLT transformasjon feilet for type {} og versjon {} ", type , versjon);
            log.error("Exception: ", e);
            throw new RuntimeException(e);
        }

        // JAXB brukes til å opprette et SaksopplysningDokument
        StringReader reader = new StringReader(outputTarget.getWriter().toString());
        SaksopplysningDokument dokument = (SaksopplysningDokument) marshaller.unmarshal(new StreamSource(reader));

        saksopplysning.setDokument(dokument);
        return dokument;
    }

}

