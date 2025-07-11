package no.nav.melosys.integrasjon.tilgangsmaskinen

import no.nav.melosys.integrasjon.tilgangsmaskinen.dto.RegelType
import no.nav.melosys.integrasjon.tilgangsmaskinen.dto.TilgangsmaskinenProblemDetail
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import no.nav.melosys.integrasjon.tilgangsmaskinen.TilgangsmaskinenException

/**
 * Implementasjon av TilgangsmaskinenConsumer som bruker Tilgangsmaskinen REST API
 *
 * Følger den offisielle OpenAPI-spesifikasjonen:
 * - Endepunkt: POST /api/v1/kjerne eller /api/v1/komplett
 * - Request: Bare fnr som string
 * - Response: 204 for suksess, 403 med ProblemDetail for avslag
 */
open class TilgangsmaskinenConsumerImpl(
    private val webClient: WebClient
) : TilgangsmaskinenConsumer {

    companion object {
        private val log = LoggerFactory.getLogger(TilgangsmaskinenConsumerImpl::class.java)
    }

    override fun sjekkTilgang(fnr: String, regeltype: RegelType): Boolean {
        log.debug("Sjekker tilgang for bruker med Tilgangsmaskinen. Regeltype: {}", regeltype)

        val endepunkt = when (regeltype) {
            RegelType.KJERNE_REGELTYPE -> "/api/v1/kjerne"
            RegelType.KOMPLETT_REGELTYPE -> "/api/v1/komplett"
            RegelType.OVERSTYRBAR_REGELTYPE -> "/api/v1/komplett" // Fallback til komplett
        }

        return try {
            webClient.post()
                .uri(endepunkt)
                .bodyValue(fnr) // Send bare fnr som string
                .retrieve()
                .bodyToMono<Void>()
                .block()

            // Hvis vi kommer hit, fikk vi 204 No Content = tilgang innvilget
            log.debug("Tilgang innvilget av Tilgangsmaskinen")
            true

        } catch (ex: WebClientResponseException) {
            when (ex.statusCode) {
                HttpStatus.FORBIDDEN -> {
                    // 403 = tilgang nektet med ProblemDetail
                    try {
                        val problemDetail = ex.getResponseBodyAs(TilgangsmaskinenProblemDetail::class.java)
                        log.debug("Tilgang nektet av Tilgangsmaskinen: {} - {}",
                            problemDetail?.title, problemDetail?.begrunnelse)
                    } catch (parseEx: Exception) {
                        log.debug("Tilgang nektet av Tilgangsmaskinen: {}", ex.responseBodyAsString)
                    }
                    false
                }
                HttpStatus.NOT_FOUND -> {
                    // 404 = ressurs ikke funnet
                    log.warn("Ressurs ikke funnet ved kall til Tilgangsmaskinen")
                    throw TilgangsmaskinenException("Ressurs ikke funnet", ex)
                }
                else -> {
                    log.error("Teknisk feil ved kall til Tilgangsmaskinen: {}", ex.statusCode)
                    throw TilgangsmaskinenException("Teknisk feil ved kall til Tilgangsmaskinen: ${ex.statusCode}", ex)
                }
            }
        } catch (ex: Exception) {
            log.error("Uventet feil ved kall til Tilgangsmaskinen", ex)
            throw TilgangsmaskinenException("Uventet feil ved kall til Tilgangsmaskinen", ex)
        }
    }
}
