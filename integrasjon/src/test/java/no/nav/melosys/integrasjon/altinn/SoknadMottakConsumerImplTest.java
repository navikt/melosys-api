package no.nav.melosys.integrasjon.altinn;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.msm.AltinnDokument;
import no.nav.melosys.soknad_altinn.Innhold;
import no.nav.melosys.soknad_altinn.MedlemskapArbeidEOSM;
import no.nav.melosys.soknad_altinn.MidlertidigUtsendt;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class SoknadMottakConsumerImplTest {

    private final RestTemplate restTemplate = new RestTemplateBuilder().rootUri("http://melosys-soknad-mottak").build();
    private SoknadMottakConsumer soknadMottakConsumer;
    private MockRestServiceServer server;

    private final String søknadID = "grj304iht";

    @Before
    public void setup() {
        server = MockRestServiceServer.createServer(restTemplate);
        soknadMottakConsumer = new SoknadMottakConsumerImpl(restTemplate);
    }

    @Test
    public void hentSøknad_mottarSoknadIXml_soknadBlirMappetTilStruktur() throws Exception {

        URI søknadURI = (getClass().getClassLoader().getResource("soknad_altinn.xml")).toURI();
        String xmlResponse = new String(Files.readAllBytes(Paths.get(søknadURI)));

        server.expect(requestTo("http://melosys-soknad-mottak/soknader/" + søknadID))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess().body(xmlResponse).contentType(MediaType.APPLICATION_XML));

        assertThat(soknadMottakConsumer.hentSøknad(søknadID)).isNotNull()
            .extracting(MedlemskapArbeidEOSM::getInnhold).isNotNull()
            .extracting(Innhold::getMidlertidigUtsendt).isNotNull()
            .extracting(MidlertidigUtsendt::getArbeidsland).isEqualTo(Landkoder.BG.getKode());
    }

    @Test
    public void hentDokumenter_mottarListeAvDokumenter_blirMappet() throws JsonProcessingException {
        AltinnDokument altinnDokument = new AltinnDokument(
            søknadID, "dokID123", "tittel", "Fullmakt", "Base64EncodedPdf");

        String json = new ObjectMapper().writeValueAsString(Collections.singleton(altinnDokument));

        server.expect(requestTo("http://melosys-soknad-mottak/soknader/" + søknadID + "/dokumenter"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess().body(json).contentType(MediaType.APPLICATION_JSON));

        assertThat(soknadMottakConsumer.hentDokumenter(søknadID))
            .isNotNull()
            .hasSize(1)
            .extracting(AltinnDokument::getTittel)
            .containsExactly(altinnDokument.getTittel());
    }
}