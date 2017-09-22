package no.nav.melosys.domain.dokument.person;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.time.LocalDate;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;

public class Tps3PersonKonverteringTest {

    public static final String TPS_PERSON_3_0_MOCK = "mock/tps_person_3.0_mock.xml";
    public static final String TPS_PERSON_3_0_XSLT = "tps/person_3.0.xslt";  // FIXME (farjam): Denne skal defineres et annet sted.

    @Test
    public void testTpsTilDomene() throws Exception {
        File inputXml = new File(getClass().getClassLoader().getResource(TPS_PERSON_3_0_MOCK).getFile());
        File xslt = new File(getClass().getClassLoader().getResource(TPS_PERSON_3_0_XSLT).getFile());

        // Transformer...
        Transformer t = TransformerFactory.newInstance().newTransformer(new StreamSource(xslt));
        StreamSource xmlSource = new StreamSource(inputXml);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(99999);
        Result outputTarget = new StreamResult(baos);
        t.transform(xmlSource, outputTarget);
        baos.flush();
        byte[] xmlBytes = baos.toByteArray();
        
        // Unmarshal...
        ByteArrayInputStream bais = new ByteArrayInputStream(xmlBytes);
        JAXBContext ctx = JAXBContext.newInstance(PersonopplysningDokument.class);
        Unmarshaller um = ctx.createUnmarshaller();
        um.setEventHandler(new DefaultValidationEventHandler());
        PersonopplysningDokument p2 = (PersonopplysningDokument) um.unmarshal(bais);

        // Verifiser...
        assertEquals(Diskresjonskode.MILI, p2.diskresjonskode);
        assertEquals(LocalDate.of(2011, 11, 11), p2.fødselsdato);
    }
    

}
