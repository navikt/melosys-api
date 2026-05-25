package no.nav.melosys.domain.brev.tekstblokk

import java.time.Instant

/**
 * Projeksjon av en tekstblokk for listevisninger. Inkluderer innhold slik at
 * frontend kan søke i brødteksten uten et ekstra kall per blokk. Tags fylles
 * inn separat av service.
 */
class TekstblokkOversikt(
    val id: Long,
    val tittel: String,
    val innhold: String,
    val type: TekstblokkType,
    val endretDato: Instant,
    val endretAv: String,
) {
    var tags: Set<String> = emptySet()
}
