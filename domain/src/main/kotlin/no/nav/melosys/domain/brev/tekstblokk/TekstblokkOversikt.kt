package no.nav.melosys.domain.brev.tekstblokk

import java.time.Instant

import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema

/**
 * Projeksjon av en tekstblokk for listevisninger. Inkluderer innhold slik at
 * frontend kan søke i brødteksten uten et ekstra kall per blokk. Tags og
 * kontekstavgrensninger fylles inn separat av service.
 */
class TekstblokkOversikt(
    val id: Long,
    val tittel: String,
    val innhold: String,
    val type: TekstblokkType,
    val endretDato: Instant,
    val endretAv: String,
    val endretAvNavn: String?,
) {
    var tags: Set<String> = emptySet()
    var sakstyper: Set<Sakstyper> = emptySet()
    var behandlingstemaer: Set<Behandlingstema> = emptySet()
}
