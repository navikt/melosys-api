package no.nav.melosys.integrasjon.aareg.arbeidsforhold

import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.felles.WebClientConfig
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriBuilder

@Retryable
class ArbeidsforholdRestConsumer(private val webClient: WebClient) : WebClientConfig {
    fun finnArbeidsforholdPrArbeidstaker(
        fnr: String,
        arbeidsforholdQuery: ArbeidsforholdQuery
    ): ArbeidsforholdResponse = ArbeidsforholdResponse(hentArbeidsforhold(fnr, arbeidsforholdQuery))

    private fun hentArbeidsforhold(
        fnr: String,
        arbeidsforholdQuery: ArbeidsforholdQuery
    ): List<ArbeidsforholdResponse.Arbeidsforhold> = webClient.get().uri("") { uriBuilder: UriBuilder ->
        uriBuilder
            .queryParam("regelverk", arbeidsforholdQuery.regelverk)
            .queryParamIfPresent("arbeidsforholdType", arbeidsforholdQuery.arbeidsforholdType)
            .queryParamIfPresent("ansettelsesperiodeFom", arbeidsforholdQuery.ansettelsesperiodeFom)
            .queryParamIfPresent("ansettelsesperiodeTom", arbeidsforholdQuery.ansettelsesperiodeTom)
            .build()
    } // Om vi ønsker å se request med mer detaljer i grafana må vi gjøre det samme som er gjort i MedlemskapRestConsumer
        // Nå ser vi bare request på host
        .header("Nav-Personident", fnr)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono<List<ArbeidsforholdResponse.Arbeidsforhold>>()
        .block()?: throw TekniskException("ArbeidsforholdResponse er null")
}
