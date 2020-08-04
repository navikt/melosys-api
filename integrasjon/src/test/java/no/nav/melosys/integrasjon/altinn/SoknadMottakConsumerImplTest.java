package no.nav.melosys.integrasjon.altinn;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.soknad_altinn.Innhold;
import no.nav.melosys.soknad_altinn.MedlemskapArbeidEOSM;
import no.nav.melosys.soknad_altinn.MidlertidigUtsendt;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class SoknadMottakConsumerImplTest {

    private final RestTemplate restTemplate = new RestTemplate();
    private SoknadMottakConsumer soknadMottakConsumer;
    private MockRestServiceServer server;

    private final String soknadID = "grj304iht";

    @Before
    public void setup() {
        server = MockRestServiceServer.createServer(restTemplate);
        soknadMottakConsumer = new SoknadMottakConsumerImpl(restTemplate);
    }

    @Test
    public void hentSøknad_mottakerSoknadIXml_soknadBlirMappetTilStruktur() throws Exception {

        URI søknadURI = (getClass().getClassLoader().getResource("soknad_altinn.xml")).toURI();
        String xmlResponse = new String(Files.readAllBytes(Paths.get(søknadURI)));

        server.expect(requestTo("/soknader/" + soknadID))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess().body(xmlResponse).contentType(MediaType.APPLICATION_XML));

        MedlemskapArbeidEOSM res = soknadMottakConsumer.hentSøknad(soknadID);

        assertThat(res).isNotNull()
            .extracting(MedlemskapArbeidEOSM::getInnhold).isNotNull()
            .extracting(Innhold::getMidlertidigUtsendt).isNotNull()
            .extracting(MidlertidigUtsendt::getArbeidsland).isEqualTo(Landkoder.BG.getKode());
    }
}