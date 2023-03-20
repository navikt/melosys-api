package no.nav.melosys.melosysmock.azuread

import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/graph/v1.0")
class AzureAdApi {

    @GetMapping("/users")
    fun hentSaksbehandlerNavn(): GraphAzureResponseDTO =
        GraphAzureResponseDTO(listOf()).apply {
            value = listOf(GraphAzureUser("Lokal Testbruker"))
        }
}


data class GraphAzureResponseDTO(var value: List<GraphAzureUser>)

data class GraphAzureUser(
    val displayName: String
)
