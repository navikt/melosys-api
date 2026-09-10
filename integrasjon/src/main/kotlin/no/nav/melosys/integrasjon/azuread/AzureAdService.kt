package no.nav.melosys.integrasjon.azuread

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class AzureAdService(
    private val azureAdClient: AzureAdClient,
) {
    // Navneoppslaget er et eksternt Graph-kall. Det gjøres nå per visning av saksbildet
    // (tildelt saksbehandler), så resultatet caches. Null caches ikke, slik at et
    // forbigående feilet oppslag ikke låser identen til «ukjent navn» i cachens levetid.
    @Cacheable(value = ["saksbehandlerNavn"], unless = "#result == null")
    fun hentSaksbehandlerNavn(ident: String): String? = azureAdClient.hentSaksbehandlerNavn(ident)
}
