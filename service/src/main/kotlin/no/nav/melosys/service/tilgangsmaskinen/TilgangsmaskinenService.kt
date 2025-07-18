package no.nav.melosys.service.tilgangsmaskinen

import mu.KotlinLogging
import no.nav.melosys.service.persondata.PersondataService
import no.nav.melosys.integrasjon.tilgangsmaskinen.TilgangsmaskinenConsumer
import no.nav.melosys.integrasjon.tilgangsmaskinen.TilgangsmaskinenException
import no.nav.melosys.integrasjon.tilgangsmaskinen.dto.RegelType
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

/**
 * Service for tilgangskontroll via Tilgangsmaskinen
 *
 * Erstatter ABAC-systemet for tilgangskontroll til persondata.
 * Separation of concerns: denne servicen håndterer kun Tilgangsmaskinen-logikk.
 *
 * Tilgangsmaskinen evaluerer automatisk alle relevante roller for brukeren,
 * så vi trenger ikke lenger å spesifisere spesifikke roller.
 *
 * Bruker KOMPLETT_REGELTYPE for komplett tilgangskontroll.
 */

private val log = KotlinLogging.logger { }

@Service
class TilgangsmaskinenService(
    private val tilgangsmaskinenConsumer: TilgangsmaskinenConsumer,
    private val persondataService: PersondataService
) {

    /**
     * Sjekker tilgang til person basert på fødselsnummer/d-nummer
     *
     * @param fnr Fødselsnummer eller d-nummer (11 tegn)
     * @return true hvis bruker har tilgang, false hvis ikke
     * @throws TilgangsmaskinenException ved tekniske feil
     */
    @Cacheable(value = ["tilgangsmaskinen"], key = "#fnr + '_' + T(no.nav.melosys.sikkerhet.context.SubjectHandler).getInstance().getUserID()")
    fun sjekkTilgangTilFnr(fnr: String): Boolean {
        log.debug("Sjekker tilgang til fnr via Tilgangsmaskinen med KOMPLETT_REGELTYPE")

        return try {
            tilgangsmaskinenConsumer.sjekkTilgang(fnr, RegelType.KOMPLETT_REGELTYPE)
        } catch (ex: TilgangsmaskinenException) {
            log.error("Feil ved kall til Tilgangsmaskinen for fnr-tilgang", ex)
            throw ex
        }
    }

    /**
     * Sjekker tilgang til person basert på aktørId
     *
     * Henter fnr/dnr fra PDL først, deretter sjekker tilgang via Tilgangsmaskinen
     *
     * @param aktørId NAVs interne aktørId
     * @return true hvis bruker har tilgang, false hvis ikke
     * @throws TilgangsmaskinenException ved tekniske feil eller manglende mapping
     */
    @Cacheable(value = ["tilgangsmaskinen"], key = "#aktørId + '_' + T(no.nav.melosys.sikkerhet.context.SubjectHandler).getInstance().getUserID()")
    fun sjekkTilgangTilAktørId(aktørId: String): Boolean {
        log.debug("Sjekker tilgang til aktørId via Tilgangsmaskinen med KOMPLETT_REGELTYPE")

        return try {
            // Hent fnr/dnr fra PDL (bruker eksisterende cache)
            val fnr = hentFnrFraPdl(aktørId)

            // Sjekk tilgang via Tilgangsmaskinen
            tilgangsmaskinenConsumer.sjekkTilgang(fnr, RegelType.KOMPLETT_REGELTYPE)

        } catch (ex: Exception) {
            log.error("Feil ved tilgangskontroll for aktørId: {}", aktørId, ex)
            throw TilgangsmaskinenException("Feil ved tilgangskontroll for aktørId", ex)
        }
    }

    /**
     * Henter fødselsnummer/d-nummer fra PDL basert på aktørId
     *
     * Bruker eksisterende cache via PersondataService.hentFolkeregisterident()
     *
     * @param aktørId NAVs interne aktørId
     * @return fødselsnummer eller d-nummer
     * @throws TilgangsmaskinenException hvis mapping ikke finnes
     */
    private fun hentFnrFraPdl(aktørId: String): String {
        log.debug("Henter fnr fra PDL for aktørId")

        return try {
            persondataService.hentFolkeregisterident(aktørId)
        } catch (ex: Exception) {
            log.warn("Kunne ikke hente fnr fra PDL for aktørId: {}", aktørId, ex)
            throw TilgangsmaskinenException("Kunne ikke hente fnr fra PDL for aktørId: $aktørId", ex)
        }
    }
}
