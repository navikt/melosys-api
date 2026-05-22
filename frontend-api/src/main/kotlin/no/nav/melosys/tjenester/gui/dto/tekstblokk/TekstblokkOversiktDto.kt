package no.nav.melosys.tjenester.gui.dto.tekstblokk

import java.time.Instant
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkOversikt
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkType

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
) {
    companion object {
        fun av(o: TekstblokkOversikt): TekstblokkOversiktDto = TekstblokkOversiktDto(
            id = o.id,
            tittel = o.tittel,
            type = o.type,
            tags = o.tags.sorted(),
            endretDato = o.endretDato,
            endretAv = o.endretAv,
        )
    }
}
