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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DokumentFactory {
    
    private final Jaxb2Marshaller marshaller;
    private final XsltTemplatesFactory xsltTemplatesFactory;

    public DokumentFactory(Jaxb2Marshaller marshaller, XsltTemplatesFactory xsltTemplatesFactory) {
        this.marshaller = marshaller;
        this.xsltTemplatesFactory = xsltTemplatesFactory;
    }

    public Marshaller createMarshaller() {
        return marshaller.createMarshaller();
    }

    public String lagForenkletXml(Saksopplysning saksopplysning) {
        Assert.notNull(saksopplysning, "saksopplysning må ikke være null");

        String dokumentXml = null;
        SaksopplysningDokument dokument = saksopplysning.getDokument();
        if (saksopplysning.getKilder() != null && !saksopplysning.getKilder().isEmpty()) {
            dokumentXml = saksopplysning.getKilder().iterator().next().getMottattDokument();
        }
        if (dokumentXml == null && dokument == null) {
            return null;
        }

        if (dokument != null) {
            StreamResult result = new StreamResult(new StringWriter());
            marshaller.marshal(dokument, result);
            return result.getWriter().toString();
        }

        SaksopplysningType type = saksopplysning.getType();
        String versjon = saksopplysning.getVersjon();
        return transformer(dokumentXml, type, versjon);
    }

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

        String forenkletXml = lagForenkletXml(saksopplysning);
        StringReader reader = new StringReader(forenkletXml);

        SaksopplysningDokument dokument = (SaksopplysningDokument) marshaller.unmarshal(new StreamSource(reader));
        saksopplysning.setDokument(dokument);

        return dokument;
    }
}
