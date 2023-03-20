package no.nav.melosys.integrasjon.azuread

import org.springframework.stereotype.Service

@Service
class AzureAdService(
    private val azureAdConsumer: AzureAdConsumer,
) {
    fun hentSaksbehandlerNavn(ident: String): String? = azureAdConsumer.hentSaksbehandlerNavn(ident)
}
