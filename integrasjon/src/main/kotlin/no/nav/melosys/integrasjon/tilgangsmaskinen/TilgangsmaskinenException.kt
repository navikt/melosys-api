package no.nav.melosys.integrasjon.tilgangsmaskinen

/**
 * Exception for feil ved kommunikasjon med Tilgangsmaskinen
 */
class TilgangsmaskinenException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
