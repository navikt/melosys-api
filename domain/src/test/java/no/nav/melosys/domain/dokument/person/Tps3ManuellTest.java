package no.nav.melosys.domain.dokument.person;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.Ignore;
import org.junit.Test;

import static no.nav.melosys.domain.dokument.person.Tps3PersonKonverteringTest.TPS_PERSON_3_0_MOCK;
import static no.nav.melosys.domain.dokument.person.Tps3PersonKonverteringTest.TPS_PERSON_3_0_XSLT;
import static org.junit.Assert.assertTrue;


public class Tps3ManuellTest {
    
    // Bruk denne testen for å se hva xslt gjør
    @Ignore
    @Test
    public void testTpsTilXml() throws Exception {
        File inputXml = new File(getClass().getClassLoader().getResource(TPS_PERSON_3_0_MOCK).getFile());
        File xslt = new File(getClass().getClassLoader().getResource(TPS_PERSON_3_0_XSLT).getFile());
        Transformer t = TransformerFactory.newInstance().newTransformer(new StreamSource(xslt));
        StreamSource xmlSource = new StreamSource(inputXml);
        t.transform(xmlSource, new StreamResult(System.out));
        assertTrue(false); // Manuell test. Ikke sjekk inn med @Ignore utkommentert.
    }
    
    @Ignore
    @Test
    public void testTpsTilDomene() throws Exception {
        File inputXml = new File(getClass().getClassLoader().getResource(TPS_PERSON_3_0_MOCK).getFile());
        File xslt = new File(getClass().getClassLoader().getResource(TPS_PERSON_3_0_XSLT).getFile());

        // Transform...
        Transformer t = TransformerFactory.newInstance().newTransformer(new StreamSource(xslt));
        StreamSource xmlSource = new StreamSource(inputXml);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(99999);
        Result outputTarget = new StreamResult(baos);
        t.transform(xmlSource, outputTarget);
        baos.flush();
        byte[] xmlBytes = baos.toByteArray();
        
        // Unmarshal...
        ByteArrayInputStream bais = new ByteArrayInputStream(xmlBytes);
        JAXBContext ctx = JAXBContext.newInstance(PersonDokument.class);
        Unmarshaller um = ctx.createUnmarshaller();
        PersonDokument p2 = (PersonDokument) um.unmarshal(bais);

        // Sett breakpoint her for å inspisere p2
        assertTrue(p2.diskresjonskode.getKode() == "MILI");
        
        assertTrue(false); // Manuell test. Ikke sjekk inn med @Ignore utkommentert.
    }
    
}
