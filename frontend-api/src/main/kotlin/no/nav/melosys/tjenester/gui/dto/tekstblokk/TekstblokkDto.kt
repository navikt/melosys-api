package no.nav.melosys.tjenester.gui.dto.tekstblokk

import java.time.Instant
import no.nav.melosys.domain.tekstblokk.TekstblokkType

/**
 * Full DTO med innhold. Brukes ved henting av enkelt tekstblokk for visning eller redigering.
 */
data class TekstblokkDto(
    val id: Long,
    val tittel: String,
    val innhold: String,
    val type: TekstblokkType,
    val tags: List<String>,
    val registrertDato: Instant,
    val registrertAv: String,
    val endretDato: Instant,
    val endretAv: String,
)
