package no.nav.melosys.integrasjon.azuread

import com.fasterxml.jackson.databind.JsonNode
import no.nav.melosys.integrasjon.azuread.dto.DisplayNameDTO
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder

@Retryable
open class AzureAdConsumer(
    private val webClient: WebClient,
) {

    open fun hentSaksbehandlerNavn(ident: String): String? {
        return webClient.get().uri("/users") { uriBuilder: UriBuilder ->
            uriBuilder
                .queryParam("\$filter", "mailnickname eq '$ident'")
                .queryParam("\$select", "displayName")
                .build()
        }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { jsonNode ->
                println(jsonNode)
                val displayName = jsonNode["value"][0]["displayName"].asText()
                DisplayNameDTO(displayName).displayName
            }
            .block()!!
    }
}
