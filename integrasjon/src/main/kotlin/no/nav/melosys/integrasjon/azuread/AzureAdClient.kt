package no.nav.melosys.integrasjon.azuread

import no.nav.melosys.integrasjon.azuread.dto.AzureAdGraphResponseDTO
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriBuilder

@Retryable
open class AzureAdClient(
    private val webClient: WebClient,
) {

    open fun hentSaksbehandlerNavn(ident: String): String? {
        return webClient.get().uri("/users") { uriBuilder: UriBuilder ->
            uriBuilder
                .queryParam("\$filter", "onPremisesSamAccountName eq '$ident'")
                .queryParam("\$count", true)
                .queryParam("\$select", "givenName,surname")
                .build()
        }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono<AzureAdGraphResponseDTO>()
            .mapNotNull { response ->
                response.value.firstOrNull()
                    ?.let { listOfNotNull(it.givenName, it.surname).joinToString(" ") }
                    ?.ifBlank { null }
            }
            .block()
    }
}
