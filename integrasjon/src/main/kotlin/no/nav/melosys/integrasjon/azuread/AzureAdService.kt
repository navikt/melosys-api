package no.nav.melosys.integrasjon.azuread

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class AzureAdService(
    private val azureAdClient: AzureAdClient,
) {
    // Graph-oppslaget kjøres per visning av saksbildet. Null caches ikke, slik at et forbigående
    // feilet oppslag ikke låser identen til «ukjent navn» i cachens levetid.
    @Cacheable(value = ["saksbehandlerNavn"], unless = "#result == null")
    fun hentSaksbehandlerNavn(ident: String): String? = azureAdClient.hentSaksbehandlerNavn(ident)
}
