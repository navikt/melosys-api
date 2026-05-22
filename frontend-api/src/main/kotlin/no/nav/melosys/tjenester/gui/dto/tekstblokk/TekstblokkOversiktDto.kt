package no.nav.melosys.tjenester.gui.dto.tekstblokk

import java.time.Instant
import no.nav.melosys.domain.tekstblokk.TekstblokkType

/**
 * Lett DTO uten innhold. Brukes for liste-visning og søk i frontend.
 */
data class TekstblokkOversiktDto(
    val id: Long,
    val tittel: String,
    val type: TekstblokkType,
    val tags: List<String>,
    val endretDato: Instant,
    val endretAv: String,
)
