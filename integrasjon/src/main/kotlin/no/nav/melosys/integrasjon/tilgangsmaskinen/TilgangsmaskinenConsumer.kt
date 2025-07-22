package no.nav.melosys.integrasjon.tilgangsmaskinen

import mu.KotlinLogging
import no.nav.melosys.integrasjon.tilgangsmaskinen.dto.RegelType
import no.nav.melosys.integrasjon.tilgangsmaskinen.dto.TilgangsmaskinenProblemDetail
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Retryable
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono

private val log = KotlinLogging.logger {}

/**
 * Bruker Tilgangsmaskinen REST API
 *
 * Følger den offisielle OpenAPI-spesifikasjonen:
 * - Endepunkt: POST /api/v1/kjerne eller /api/v1/komplett
 * - Request: Bare fnr som string
 * - Response: 204 for suksess, 403 med ProblemDetail for avslag
 */
open class TilgangsmaskinenConsumer(
    private val webClient: WebClient
) {

    fun sjekkTilgang(fnr: String, regeltype: RegelType = RegelType.KOMPLETT_REGELTYPE): Boolean {
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
                        log.debug(
                            "Tilgang nektet av Tilgangsmaskinen: {} - {}",
                            problemDetail?.title, problemDetail?.begrunnelse
                        )
                    } catch (_: Exception) {
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
