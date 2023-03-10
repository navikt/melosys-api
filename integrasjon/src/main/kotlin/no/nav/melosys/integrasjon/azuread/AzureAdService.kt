package no.nav.melosys.integrasjon.azuread

import no.nav.melosys.exception.IkkeFunnetException
import org.springframework.stereotype.Service

@Service
class AzureAdService(
    private val azureAdConsumer: AzureAdConsumer,
) {
    fun hentSaksbehandlerNavn(ident: String): String = azureAdConsumer.hentSaksbehandlerNavn(ident)
        ?: throw IkkeFunnetException("Fant ikke saksbehandlers navn på $ident")
}
