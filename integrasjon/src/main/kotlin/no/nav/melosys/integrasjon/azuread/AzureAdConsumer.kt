package no.nav.melosys.integrasjon.azuread

import no.nav.melosys.integrasjon.azuread.dto.AzureAdGraphResponseDTO
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriBuilder

@Retryable
open class AzureAdConsumer(
    private val webClient: WebClient,
) {

    private fun hentSaksbehandler(ident: String): AzureAdGraphResponseDTO? {
        return webClient.get().uri("/users") { uriBuilder: UriBuilder ->
            uriBuilder
                .queryParam("\$filter", "mailnickname eq '$ident'")
                .queryParam("\$select", "displayName")
                .build()
        }
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono<AzureAdGraphResponseDTO>()
            .block()!!
    }

    open fun hentSaksbehandlerNavn(ident: String): String? {
        val saksbehandlerListe = hentSaksbehandler(ident)?.value
        return if (saksbehandlerListe == null || saksbehandlerListe.isEmpty()) {
            ""
        } else {
            saksbehandlerListe[0].displayName
        }
    }
}
