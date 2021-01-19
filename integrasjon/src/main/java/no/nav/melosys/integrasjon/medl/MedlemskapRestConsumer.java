package no.nav.melosys.integrasjon.medl;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.felles.RestConsumer;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import no.nav.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForGet;
import no.nav.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForPost;
import no.nav.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForPut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

@Component
public class MedlemskapRestConsumer implements RestConsumer {
    private static final Logger log = LoggerFactory.getLogger(MedlemskapRestConsumer.class);
    private static final String CONSUMER_ID = "srvmelosys";

    private final RestStsClient restStsClient;
    private final WebClient webClient;

    @Autowired
    public MedlemskapRestConsumer(@Value("${medlemskap.rest.url}") String url, RestStsClient restStsClient) {
        this.restStsClient = restStsClient;
        this.webClient = WebClient.builder()
            .baseUrl(url)
            .filter(headerFilter())
            .filter(errorFilter())
            .build();
    }

    public List<MedlemskapsunntakForGet> hentPeriodeListe(String fnr, LocalDate fom, LocalDate tom) {
        return asList(hentPeriodeListe(fnr, fom, tom, ""));
    }

    private MedlemskapsunntakForGet[] hentPeriodeListe(String fnr, LocalDate fom, LocalDate tom, String eksluderteKilder) {
        return requireNonNull(
            webClient.get().uri(uriBuilder ->
                uriBuilder
                    .path("")
                    .queryParam("fraOgMed", fom)
                    .queryParam("tilOgMed", tom)
                    .queryParam("inkluderSporingsinfo", true)
                    .queryParam("ekskluderKilder", eksluderteKilder)
                    .build())
                .header("Nav-Personident", fnr)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(MedlemskapsunntakForGet[].class)
                .block()
        );
    }

    public MedlemskapsunntakForGet hentPeriode(String periodeId) {
        return webClient.get().uri(uriBuilder ->
            uriBuilder
                .path("/{periodeId}")
                .queryParam("inkluderSporingsinfo", true)
                .build(periodeId))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(MedlemskapsunntakForGet.class)
            .block();
    }

    public MedlemskapsunntakForGet opprettPeriode(MedlemskapsunntakForPost request) {
        return utfør(request, HttpMethod.POST);
    }

    public MedlemskapsunntakForGet oppdaterPeriode(MedlemskapsunntakForPut request) {
        return utfør(request, HttpMethod.PUT);
    }

    private MedlemskapsunntakForGet utfør(Object request, HttpMethod method) {
        return webClient.method(method)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(MedlemskapsunntakForGet.class)
            .block();
    }

    private ExchangeFilterFunction headerFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(
            request -> {
                try {
                    return Mono.just(ClientRequest.from(request)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + restStsClient.collectToken())
                        .header("Nav-Call-Id", getCallID())
                        .header("Nav-Consumer-Id", CONSUMER_ID)
                        .build());
                } catch (MelosysException e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
        );
    }

    private ExchangeFilterFunction errorFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if(response.statusCode().isError()) {
                return response.bodyToMono(String.class)
                    .flatMap(errorBody -> {
                        log.error("Kall mot MEDL feilet. {} - {}", response.statusCode(), errorBody);
                        return Mono.error(new RuntimeException(errorBody));
                    });
            }
            return Mono.just(response);
        });
    }
}
