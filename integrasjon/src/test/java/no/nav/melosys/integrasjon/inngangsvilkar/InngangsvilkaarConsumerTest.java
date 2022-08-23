package no.nav.melosys.integrasjon.inngangsvilkar;

import java.time.LocalDate;
import java.util.Set;

import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.inngangsvilkar.InngangsvilkarResponse;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingInterceptor;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class InngangsvilkaarConsumerTest {
    private MockRestServiceServer server;
    private final String url = "http://melosys-inngangsvilkar/api";
    private InngangsvilkaarConsumerImpl inngangsvilkaarConsumer;

    @BeforeEach
    void setup() {
        RestTemplate restTemplate = new InngangsvilkarConfig().inngangsVilkaarRestTemplate(url, new RestTemplateBuilder(), new CorrelationIdOutgoingInterceptor());
        server = MockRestServiceServer.createServer(restTemplate);
        inngangsvilkaarConsumer = new InngangsvilkaarConsumerImpl(restTemplate);
    }

    @Test
    void vurderInngangsvilkår() {
        final Set<Land> statsborgerskap = Set.of(Land.av(Land.NORGE));
        final Set<String> arbeidsland = Set.of(Land.SVERIGE);
        final boolean erUkjenteEllerAlleEosLand = false;
        final LocalDate nå = LocalDate.now();

        server.expect(requestTo(url + "/inngangsvilkaar"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(header(HttpHeaders.ACCEPT, StringContains.containsString(MediaType.APPLICATION_JSON_VALUE)))
            .andRespond(withSuccess("{\"kvalifisererForEf883_2004\": true,\"feilmeldinger\": []}", MediaType.APPLICATION_JSON));

        var response = inngangsvilkaarConsumer.vurderInngangsvilkår(statsborgerskap, arbeidsland, erUkjenteEllerAlleEosLand, new Periode(nå, nå));
        assertThat(response).isNotNull().extracting(InngangsvilkarResponse::getKvalifisererForEf883_2004).isEqualTo(Boolean.TRUE);
    }
}
