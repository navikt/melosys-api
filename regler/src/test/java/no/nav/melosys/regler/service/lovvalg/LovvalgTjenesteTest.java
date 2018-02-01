package no.nav.melosys.regler.service.lovvalg;

import java.io.InputStream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.regler.api.lovvalg.LovvalgTjeneste;
import no.nav.melosys.regler.api.lovvalg.rep.FastsettLovvalgReply;
import no.nav.melosys.regler.api.lovvalg.req.FastsettLovvalgRequest;
import org.junit.Before;
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
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // FIXME: Sørg for at json kan mappes uten problemer
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("88888888882.json");

        FastsettLovvalgRequest request = mapper.readValue(inputStream, FastsettLovvalgRequest.class);
        assertNotNull(request);

        FastsettLovvalgReply reply = lovvalgTjeneste.fastsettLovvalg(request);
        assertNotNull(reply);
    }
}
