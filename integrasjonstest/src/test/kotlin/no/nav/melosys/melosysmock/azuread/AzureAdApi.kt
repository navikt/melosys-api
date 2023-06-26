package no.nav.melosys.melosysmock.azuread

import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/graph/v1.0")
@Unprotected
class AzureAdApi {

    @GetMapping("/users")
    fun hentSaksbehandlerNavn(): GraphAzureResponseDTO =
        GraphAzureResponseDTO(listOf()).apply {
            value = listOf(GraphAzureUser(givenName = "Lokal", surname = "Testbruker"))
        }
}


data class GraphAzureResponseDTO(var value: List<GraphAzureUser>)

data class GraphAzureUser(
    val givenName: String,
    val surname: String
)
