package no.nav.melosys.domain.dokument;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.bind.Marshaller;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * SaksopplysningDokumentFactory konverterer et xml-resultat fra en ekstern tjeneste til et internt xml-dokument ved hejlp av XSLT med JAXP.
 * Xml-dokumentet blir deretter transformert med JAXB til et objekttre som tilhører et sentralt domene.
 * Klassen er ikke trådsikker.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DokumentFactory {
    // Spring JAXB 2 marshaller og unmarshaller
    private final Jaxb2Marshaller marshaller;
    private final XsltTemplatesFactory xsltTemplatesFactory;

    @Autowired
    public DokumentFactory(Jaxb2Marshaller marshaller, XsltTemplatesFactory xsltTemplatesFactory) {
        this.marshaller = marshaller;
        this.xsltTemplatesFactory = xsltTemplatesFactory;
    }

    public Marshaller createMarshaller() {
        return marshaller.createMarshaller();
    }

    /**
     * Setter {@code internXml} på en {@link Saksopplysning} ut fra en predefinert xslt anvendt på feltet {@code dokumentXml}
     * eller fra feltet {@code dokument} hvis feltet er ikke null.
     * Det resulterende xml returneres.
     */
    public String lagInternXml(Saksopplysning saksopplysning) {
        Assert.notNull(saksopplysning, "saksopplysning må ikke være null");

        String dokumentXml = null;
        SaksopplysningDokument dokument = saksopplysning.getDokument();
        if (saksopplysning.getKilder() != null && !saksopplysning.getKilder().isEmpty()) {
            dokumentXml = saksopplysning.getKilder().iterator().next().getMottattDokument();
        }
        if (dokumentXml == null && dokument == null) {
            return null;
        }

        // Hvis saksopplysning.getDokument() eksisterer kan man serialisere direkte for å få intern xml.
        if (dokument != null) {
            StreamResult result = new StreamResult(new StringWriter());
            marshaller.marshal(dokument, result);
            return result.getWriter().toString();
        }

        SaksopplysningType type = saksopplysning.getType();
        String versjon = saksopplysning.getVersjon();
        return transformer(dokumentXml, type, versjon);
    }

    // {@code dokumentXml} transformeres med en JAXP Transformer
    private String transformer(String dokumentXml, SaksopplysningType type, String versjon) {
        StreamResult outputTarget = new StreamResult(new StringWriter());
        try {
            Templates templates = xsltTemplatesFactory.getXsltTemplates(type, versjon);
            Transformer transformer = templates.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());

            StreamSource xmlSource = new StreamSource(new StringReader(dokumentXml));
            transformer.transform(xmlSource, outputTarget);
        } catch (TransformerException e) {
            throw new IllegalStateException(e);
        }

        return outputTarget.getWriter().toString();
    }

    /**
     * Setter et {@link SaksopplysningDokument} og {@code internXml} på en {@link Saksopplysning} ut fra feltet {@code dokumentXml}.
     * SaksopplysningDokumentet returneres.
     */
    public SaksopplysningDokument lagDokument(Saksopplysning saksopplysning) {
        Assert.notNull(saksopplysning, "saksopplysning må ikke være null");

        String dokumentXml = null;
        if (saksopplysning.getKilder() != null && !saksopplysning.getKilder().isEmpty()) {
            dokumentXml = saksopplysning.getKilder().iterator().next().getMottattDokument();
        }
        if (dokumentXml == null) {
            saksopplysning.setDokument(null);
            return null;
        }

        String internXml = hentInternXml(saksopplysning);
        StringReader reader = new StringReader(internXml);

        // JAXB brukes til å opprette et SaksopplysningDokument
        SaksopplysningDokument dokument = (SaksopplysningDokument) marshaller.unmarshal(new StreamSource(reader));
        saksopplysning.setDokument(dokument);

        return dokument;
    }

    /**
     *  InternXml lages hvis det ikke eksisterer fra før av. (eksterne saksopplysninger)
     *  Interne saksopplysninger (saksopplysninger som lages av melosys) må generere internXml
     *  hver gang de lastes fordi de er redigerbare og versjonerte
     */
    private String hentInternXml(Saksopplysning saksopplysning) {
        return lagInternXml(saksopplysning);
    }
}