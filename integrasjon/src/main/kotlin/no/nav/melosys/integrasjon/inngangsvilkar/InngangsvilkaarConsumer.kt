package no.nav.melosys.integrasjon.inngangsvilkar

import no.nav.melosys.domain.inngangsvilkar.InngangsvilkarResponse
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class InngangsvilkaarConsumer(private val inngangsvilkaarWebClient: WebClient) {

    fun vurderInngangsvilkår(
        vurderInngangsvilkaarRequest: VurderInngangsvilkaarRequest
    ): InngangsvilkarResponse = inngangsvilkaarWebClient.post()
        .uri("/inngangsvilkaar")
        .bodyValue(vurderInngangsvilkaarRequest)
        .retrieve()
        .bodyToMono(InngangsvilkarResponse::class.java)
        .block() ?: error("Fikk uforventet null-respons fra inngangsvilkår")

}
