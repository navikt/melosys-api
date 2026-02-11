package no.nav.melosys.integrasjon.inngangsvilkar

import no.nav.melosys.domain.inngangsvilkar.InngangsvilkarResponse
import org.springframework.web.reactive.function.client.WebClient

class InngangsvilkaarClient(private val webClient: WebClient) {

    fun vurderInngangsvilkår(
        vurderInngangsvilkaarRequest: VurderInngangsvilkaarRequest
    ): InngangsvilkarResponse = webClient.post()
        .uri("/inngangsvilkaar")
        .bodyValue(vurderInngangsvilkaarRequest)
        .retrieve()
        .bodyToMono(InngangsvilkarResponse::class.java)
        .block() ?: error("Fikk uforventet null-respons fra inngangsvilkår")

}
