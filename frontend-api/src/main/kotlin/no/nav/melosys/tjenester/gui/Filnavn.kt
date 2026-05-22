package no.nav.melosys.tjenester.gui

/**
 * Gjør en dokumenttittel trygg å bruke som filnavn i Content-Disposition-headeren.
 */
object Filnavn {

    /**
     * Fjerner kontrolltegn og path-separatorer fra tittelen.
     */
    @JvmStatic
    fun saner(tittel: String): String =
        tittel
            .filterNot { it.isISOControl() }
            .replace('/', '_')
            .replace('\\', '_')
}
