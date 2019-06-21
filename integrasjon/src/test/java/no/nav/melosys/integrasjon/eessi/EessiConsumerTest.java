package no.nav.melosys.integrasjon.eessi;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import no.nav.melosys.domain.eessi.Institusjon;
import no.nav.melosys.domain.eessi.SedInformasjon;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(MockitoJUnitRunner.class)
public class EessiConsumerTest {

    @Spy
    private RestTemplate restTemplate;
    private EessiConsumer eessiConsumer;
    private MockRestServiceServer server;

    private SedDataDto sedDataDto;

    @Before
    public void setup() {
        server = MockRestServiceServer.createServer(restTemplate);
        eessiConsumer = new EessiConsumerImpl(restTemplate);
        sedDataDto = new SedDataDto();
    }

    @Test
    public void opprettOgSend_forventMap() throws Exception {
        server.expect(requestTo("/sed/createAndSend"))
            .andRespond(withSuccess("{\"rinaCaseId\":\"123132\"}", MediaType.APPLICATION_JSON));
        Map<String, String> resultat = eessiConsumer.opprettOgSendSed(sedDataDto);
        assertThat(resultat.get("rinaCaseId")).isEqualTo("123132");
    }

    @Test(expected = MelosysException.class)
    public void opprettOgSend_forventException() throws Exception {
        server.expect(requestTo("/sed/createAndSend"))
            .andRespond(withBadRequest());
        eessiConsumer.opprettOgSendSed(sedDataDto);
    }

    @Test
    public void opprettBucOgSed_forventUrl() throws MelosysException {
        server.expect(requestTo("/sed/create/LA_BUC_01"))
            .andRespond(withSuccess("{\"bucId\":\"12345\",\"rinaUrl\":\"localhost:3000\"}",
                MediaType.APPLICATION_JSON));

        String url = eessiConsumer.opprettBucOgSed(sedDataDto, "LA_BUC_01");
        assertThat(url).isEqualTo("localhost:3000");
    }

    @Test(expected = MelosysException.class)
    public void opprettBucOgSed_forventException() throws MelosysException {
        server.expect(requestTo("/sed/create/LA_BUC_01"))
            .andRespond(withBadRequest());

        eessiConsumer.opprettBucOgSed(sedDataDto, "LA_BUC_01");
    }

    @Test
    public void hentTilknyttedeSeder_forventSeder() throws MelosysException, IOException, URISyntaxException {
        URI uri = (getClass().getClassLoader().getResource("mock/eux/sederUtkast.json")).toURI();
        String json = new String(Files.readAllBytes(Paths.get(uri)));

        server.expect(requestTo("/sak/1/sed/?status=utkast"))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<SedInformasjon> sedInformasjonListe = eessiConsumer.hentTilknyttedeSeder(1L, "utkast");
        assertThat(sedInformasjonListe)
            .extracting(SedInformasjon::getSedId, SedInformasjon::getSedType, SedInformasjon::getStatus)
            .contains(
                tuple("22223333", "A008", "new"),
                tuple("11221122", "A001", "new")
            );
    }

    @Test(expected = MelosysException.class)
    public void hentTilknyttedeSeder_forventException() throws MelosysException {
        server.expect(requestTo("/sak/1/sed/?status=utkast"))
            .andRespond(withBadRequest());

        eessiConsumer.hentTilknyttedeSeder(1L, "utkast");
    }

    @Test
    public void hentMottakerinstitusjoner_forventInstitusjoner() throws MelosysException {
        server.expect(requestTo("/buc/LA_BUC_01/institusjoner"))
            .andRespond(withSuccess("[{\"id\":\"NO:NAVT002\",\"navn\":\"NAVT002\",\"landkode\":\"NO\"}]",
                MediaType.APPLICATION_JSON));

        List<Institusjon> institusjoner = eessiConsumer.hentMottakerinstitusjoner("LA_BUC_01");
        assertThat(institusjoner).extracting(Institusjon::getId, Institusjon::getNavn, Institusjon::getLandkode)
            .contains(tuple("NO:NAVT002", "NAVT002", "NO"));
    }

    @Test(expected = MelosysException.class)
    public void hentMottakerinstitusjoner_forventException() throws MelosysException {
        server.expect(requestTo("/buc/LA_BUC_01/institusjoner"))
            .andRespond(withBadRequest());

        eessiConsumer.hentMottakerinstitusjoner("LA_BUC_01");
    }
}