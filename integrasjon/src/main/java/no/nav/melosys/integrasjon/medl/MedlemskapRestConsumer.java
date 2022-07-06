package no.nav.melosys.integrasjon.medl;

import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.integrasjon.felles.RestConsumer;
import no.nav.melosys.ekstern.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForGet;
import no.nav.melosys.ekstern.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForPost;
import no.nav.melosys.ekstern.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForPut;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.reactive.function.client.WebClient;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

@Retryable
public class MedlemskapRestConsumer implements RestConsumer {
    private final WebClient webClient;

    public MedlemskapRestConsumer(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<MedlemskapsunntakForGet> hentPeriodeListe(String fnr, LocalDate fom, LocalDate tom) {
        return asList(hentPeriodeListe(fnr, fom, tom, ""));
    }

    private MedlemskapsunntakForGet[] hentPeriodeListe(String fnr, LocalDate fom, LocalDate tom, String eksluderteKilder) {
        return requireNonNull(
            webClient.get().uri("", uriBuilder ->
                uriBuilder
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
        return webClient.get()
            .uri("/{periodeId}?inkluderSporingsinfo={inkluderSporingsinfo}", periodeId, true)
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
}
