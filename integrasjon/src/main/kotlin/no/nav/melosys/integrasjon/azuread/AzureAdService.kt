package no.nav.melosys.integrasjon.azuread

import org.springframework.stereotype.Service

@Service
class AzureAdService(
    private val azureAdClient: AzureAdClient,
) {
    fun hentSaksbehandlerNavn(ident: String): String? = azureAdClient.hentSaksbehandlerNavn(ident)
}
