package no.nav.melosys.domain.dokument.person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.*;
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

    public static final String TPS_PERSON_3_0_MOCK = "person/tps_person_3.0_mock.xml";
    public static final String TPS_PERSON_3_0_XSLT = "tps/person_3.0.xslt";  // FIXME (farjam): Denne skal defineres et annet sted.
    private static final String TPS_PERSON_MED_FAMILIERELASJONER = "person/88888888886.xml";

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
        JAXBContext ctx = JAXBContext.newInstance(PersonDokument.class);
        Unmarshaller um = ctx.createUnmarshaller();
        um.setEventHandler(new DefaultValidationEventHandler());
        PersonDokument p2 = (PersonDokument) um.unmarshal(bais);

        // Verifiser...
        assertEquals(Diskresjonskode.MILI, p2.diskresjonskode);
        assertEquals(LocalDate.of(2011, 11, 11), p2.fødselsdato);
    }

    @Test
    public void testFamilierelasjoner() throws Exception {
        StreamSource xmlSource = new StreamSource(getClass().getClassLoader().getResourceAsStream(TPS_PERSON_MED_FAMILIERELASJONER));
        StreamSource streamSource = new StreamSource(getClass().getClassLoader().getResourceAsStream(TPS_PERSON_3_0_XSLT));

        Transformer transformer = TransformerFactory.newInstance().newTransformer(streamSource);
        StringWriter writer = new StringWriter();
        Result outputTarget = new StreamResult(writer);
        transformer.transform(xmlSource, outputTarget);
        writer.flush();

        // Unmarshal...
        StringReader reader = new StringReader(writer.toString());
        JAXBContext ctx = JAXBContext.newInstance(PersonDokument.class);
        Unmarshaller um = ctx.createUnmarshaller();
        um.setEventHandler(new DefaultValidationEventHandler());
        PersonDokument dokument = (PersonDokument) um.unmarshal(reader);

        // Verifiser...
        assertThat(dokument).isNotNull();
        assertThat(dokument.familierelasjoner).isNotEmpty();
    }

    @Test
    public void testPostadresse() throws Exception {
        final String kilde = "person/midlertidig_postadresse_utland.xml";
        PersonDokument dokument = getPersonDokument(kilde);

        assertThat(dokument).isNotNull();
        assertThat(dokument.postadresse).isNotNull();
        assertThat(dokument.midlertidigPostadresse).isNotNull();
    }

    @Test
    public void testMidlertidigPostadresseNorge() throws Exception {
        final String kilde = "person/midlertidig_postadresse_norge.xml";
        PersonDokument dokument = getPersonDokument(kilde);

        assertThat(dokument).isNotNull();
        assertThat(dokument.midlertidigPostadresse).isNotNull();
    }

    private PersonDokument getPersonDokument(String kilde) throws Exception {
        StreamSource xmlSource = new StreamSource(getClass().getClassLoader().getResourceAsStream(kilde));
        StreamSource streamSource = new StreamSource(getClass().getClassLoader().getResourceAsStream(TPS_PERSON_3_0_XSLT));

        Transformer transformer = TransformerFactory.newInstance().newTransformer(streamSource);
        StringWriter writer = new StringWriter();
        Result outputTarget = new StreamResult(writer);
        transformer.transform(xmlSource, outputTarget);
        writer.flush();

        // Unmarshal...
        StringReader reader = new StringReader(writer.toString());
        JAXBContext ctx = JAXBContext.newInstance(PersonDokument.class);
        Unmarshaller unmarshaller = ctx.createUnmarshaller();
        unmarshaller.setEventHandler(new DefaultValidationEventHandler());
        return (PersonDokument) unmarshaller.unmarshal(reader);
    }

}
