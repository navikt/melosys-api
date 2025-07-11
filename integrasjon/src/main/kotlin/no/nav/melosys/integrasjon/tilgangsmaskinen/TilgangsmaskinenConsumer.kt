package no.nav.melosys.integrasjon.tilgangsmaskinen

import no.nav.melosys.integrasjon.tilgangsmaskinen.dto.RegelType
import org.springframework.retry.annotation.Retryable

/**
 * Consumer interface for Tilgangsmaskinen API
 *
 * Tilgangsmaskinen evaluerer automatisk alle relevante roller for brukeren.
 * API bruker 204 No Content for suksess og 403 Forbidden for avslag.
 */
@Retryable
interface TilgangsmaskinenConsumer {

    /**
     * Sjekker tilgang for person med gitt fødselsnummer/d-nummer/NPID
     *
     * @param fnr Fødselsnummer, d-nummer eller NPID (11 tegn)
     * @param regeltype Type regelsett som skal evalueres (default: KJERNE_REGELTYPE)
     * @return true hvis tilgang er innvilget (204), false hvis nektet (403)
     * @throws TilgangsmaskinenException ved tekniske feil eller andre HTTP-statuser
     */
    fun sjekkTilgang(fnr: String, regeltype: RegelType = RegelType.KOMPLETT_REGELTYPE): Boolean
}
