package no.nav.melosys.regler.service.lovvalg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import no.nav.melosys.regler.api.lovvalg.LovvalgTjeneste;
import no.nav.melosys.regler.api.lovvalg.rep.FastsettLovvalgReply;
import no.nav.melosys.regler.api.lovvalg.req.FastsettLovvalgRequest;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class LovvalgTjenesteTest {

    private LovvalgTjeneste lovvalgTjeneste;

    @Before
    public void setUp() {
        lovvalgTjeneste = new LovvalgTjenesteImpl();
    }

    @Test
    public void fastsettLovvalg() throws Exception {
        //final InputStream kilde = getClass().getClassLoader().getResourceAsStream("FJERNET.xml");
        for (String xmlFile : new String[]{"FJERNET.xml", "FJERNET.xml"}) {

            FastsettLovvalgRequest request = getFastsettLovvalgRequest(xmlFile);

            assertNotNull(request);

            FastsettLovvalgReply reply = lovvalgTjeneste.fastsettLovvalg(request);

            assertNotNull(reply);
        }
    }

    @Ignore // Funger bare med kjørende API, med mindre vi snurrer opp applikasjonen som en del av testen
    @Test
    public void sendXmlRequest() throws IOException {
        final InputStream kilde = getClass().getClassLoader().getResourceAsStream("FJERNET.xml");
        BufferedReader buffer = new BufferedReader(new InputStreamReader(kilde));
        String xml = buffer.lines().collect(Collectors.joining());

        assertNotNull(xml);

        final String regelmodulUrl = "http://localhost:8081/lovvalg/fastsettLovvalg";

        String response = ClientBuilder.newClient().target(regelmodulUrl)
                .request(LovvalgTjenesteImpl.APPLICATION_XML_UTF_8)
                .post(Entity.entity(xml, LovvalgTjenesteImpl.APPLICATION_XML_UTF_8), String.class);

        assertNotNull(response);

        ObjectMapper mapper = new XmlMapper(new JacksonXmlModule());

        FastsettLovvalgReply reply = mapper.readValue(response, FastsettLovvalgReply.class);

        assertNotNull(reply);
    }

    private FastsettLovvalgRequest getFastsettLovvalgRequest(String xmlFile) throws TransformerException, JAXBException {
        final InputStream kilde = getClass().getClassLoader().getResourceAsStream(xmlFile);
        final InputStream xslt = getClass().getClassLoader().getResourceAsStream("fastsett-lovvalg-request.xslt");

        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(new StreamSource(xslt));

        JAXBContext context = JAXBContext.newInstance(FastsettLovvalgRequest.class);
        JAXBResult result = new JAXBResult(context);

        transformer.transform(new StreamSource(kilde), result);

        return (FastsettLovvalgRequest) result.getResult();
    }
}
