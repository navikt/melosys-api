package no.nav.melosys.domain.tekstblokk

import java.time.Instant

/**
 * Slank projeksjon av en tekstblokk for listevisninger. Mangler innhold (CLOB)
 * fordi liste-flyten ikke trenger det. Tags fylles inn separat av service.
 */
class TekstblokkOversikt(
    val id: Long,
    val tittel: String,
    val type: TekstblokkType,
    val endretDato: Instant,
    val endretAv: String,
) {
    var tags: Set<String> = emptySet()
}
