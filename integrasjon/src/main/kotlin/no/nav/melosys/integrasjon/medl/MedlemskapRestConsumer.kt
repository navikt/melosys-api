package no.nav.melosys.integrasjon.medl

import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForGet
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForPost
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForPut
import no.nav.melosys.integrasjon.felles.RestConsumer
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import java.time.LocalDate

@Retryable
class MedlemskapRestConsumer(private val webClient: WebClient) : RestConsumer {
    fun hentPeriodeListe(fnr: String, fom: LocalDate, tom: LocalDate): List<MedlemskapsunntakForGet> {
        return hentMedlemskapsunntakForPeriode(fnr, fom, tom)!!.toList()
    }

    private fun hentMedlemskapsunntakForPeriode(
        fnr: String,
        fom: LocalDate,
        tom: LocalDate
    ): Array<MedlemskapsunntakForGet>? {
        return webClient.get().uri("") { uriBuilder: UriBuilder ->
            uriBuilder
                .queryParam("fraOgMed", fom)
                .queryParam("tilOgMed", tom)
                .queryParam("inkluderSporingsinfo", true)
                .queryParam("ekskluderKilder", "")
                .build()
        }
            .header("Nav-Personident", fnr)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(Array<MedlemskapsunntakForGet>::class.java)
            .block()!!
    }

    fun hentPeriode(periodeId: String?) = webClient.get()
        .uri("/{periodeId}?inkluderSporingsinfo={inkluderSporingsinfo}", periodeId, true)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(MedlemskapsunntakForGet::class.java)
        .block()!!

    fun opprettPeriode(request: MedlemskapsunntakForPost) = utfør(request, HttpMethod.POST)

    fun oppdaterPeriode(request: MedlemskapsunntakForPut) = utfør(request, HttpMethod.PUT)

    private fun utfør(request: Any, method: HttpMethod) = webClient.method(method)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(MedlemskapsunntakForGet::class.java)
        .block()!!
}
