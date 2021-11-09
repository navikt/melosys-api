package no.nav.melosys.integrasjonstest.saksflyt;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.melosys.exception.IkkeFunnetException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import java.util.Comparator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class TestDataForTrygdeavtale {

    String body = """
        {
          "antall": 1,
          "tilordnetRessurs": "Z123456"
        }""";

    public JsonNode lagOppgave() {
        WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:8083/testdata/jfr-oppgave")
            .defaultHeaders(this::defaultHeaders)
            .build();

        return webClient.post().uri("", UriBuilder::build)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
    }

    public JsonNode hentOppgave(String id) {
        JsonNode jsonNode = hentOppgaver();
        return asStream(jsonNode)
            .filter(node -> node.get("id").asText().equals(id))
            .findFirst()
            .orElseThrow(() -> new IkkeFunnetException("Oppgave med id " + id + " ikke funnet"));
    }

    public JsonNode hentNyesteOppgave() {
        JsonNode jsonNode = hentOppgaver();
        Comparator<JsonNode> idComparator = Comparator.comparing(o -> o.get("id").asInt());
        return asStream(jsonNode)
            .max(idComparator).orElseThrow(() -> new IkkeFunnetException("Ingen oppgaver funnet"));
    }

    public JsonNode hentFørsteOppgave() {
        JsonNode jsonNode = hentOppgaver();

        Comparator<JsonNode> idComparator = Comparator.comparing(o -> o.get("id").asInt());
        return asStream(jsonNode)
            .min(idComparator).orElseThrow(() -> new IkkeFunnetException("Ingen oppgaver funnet"));
    }

    private Stream<JsonNode> asStream(JsonNode node) {
        return StreamSupport.stream(node.get("oppgaver").spliterator(), false);
    }

    private JsonNode hentOppgaver() {
        WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:8083/api/v1/oppgaver")
            .build();

        return webClient.get().uri("", UriBuilder::build)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
    }

    private void defaultHeaders(HttpHeaders httpHeaders) {
        httpHeaders.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }
}
