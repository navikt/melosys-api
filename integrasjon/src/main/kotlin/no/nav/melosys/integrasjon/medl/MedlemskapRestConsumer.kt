package no.nav.melosys.integrasjon.medl

import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForGet
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForPost
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForPut
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakSoekRequest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate

@Retryable
open class MedlemskapRestConsumer(private val webClient: WebClient)  {

    // Metoder må være open for at retry skal funke og at webClient ikke skal bli null
    // https://github.com/spring-projects/spring-framework/issues/26729

    /**
     * Henter liste over medlemskapsunntak for en gitt periode
     */
    open fun hentPeriodeListe(fnr: String, fom: LocalDate?, tom: LocalDate?): List<MedlemskapsunntakForGet> {
        return hentMedlemskapsunntakForPeriode(fnr, fom, tom)!!.toList()
    }

    /**
     * Søker etter medlemskapsunntak for en person i en gitt periode
     */
    private fun hentMedlemskapsunntakForPeriode(
        fnr: String,
        fom: LocalDate?,
        tom: LocalDate?
    ): Array<MedlemskapsunntakForGet>? {
        val request = MedlemskapsunntakSoekRequest(
            personident = fnr,
            fraOgMed = fom,
            tilOgMed = tom,
            inkluderSporingsinfo = true,
            ekskluderKilder = emptyList()
        )

        return webClient.post()
            .uri("/rest/v1/periode/soek")
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(Array<MedlemskapsunntakForGet>::class.java)
            .block()!!
    }

    /**
     * Henter en spesifikk medlemskapsunntak-periode basert på periode-ID
     */
    open fun hentPeriode(periodeId: String?) = webClient.get()
        .uri("/api/v1/medlemskapsunntak/{periodeId}?inkluderSporingsinfo={inkluderSporingsinfo}", periodeId, true)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(MedlemskapsunntakForGet::class.java)
        .block()!!

    /**
     * Oppretter en ny medlemskapsunntak-periode
     */
    open fun opprettPeriode(request: MedlemskapsunntakForPost) = utfør(request, HttpMethod.POST)

    /**
     * Oppdaterer en eksisterende medlemskapsunntak-periode
     */
    open fun oppdaterPeriode(request: MedlemskapsunntakForPut) = utfør(request, HttpMethod.PUT)

    private fun utfør(request: Any, method: HttpMethod) = webClient.method(method)
        .uri("/api/v1/medlemskapsunntak")
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(MedlemskapsunntakForGet::class.java)
        .block()!!
}
