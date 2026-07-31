package no.nav.melosys.tjenester.gui.dto.tekstblokk

import java.time.Instant
import no.nav.melosys.domain.brev.tekstblokk.Tekstblokk
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkType

/**
 * Full DTO med innhold. Brukes ved henting av enkelt tekstblokk for visning eller redigering.
 * Kontekstavgrensningene sendes som rene koder – rå enum ville KodeSerializer gjort om
 * til {kode, term}-objekter.
 */
data class TekstblokkDto(
    val id: Long,
    val tittel: String,
    val innhold: String,
    val type: TekstblokkType,
    val tags: List<String>,
    val sakstyper: List<String>,
    val behandlingstemaer: List<String>,
    val status: String,
    val registrertDato: Instant,
    val registrertAv: String,
    val endretDato: Instant,
    val endretAv: String,
) {
    companion object {
        fun av(t: Tekstblokk): TekstblokkDto = TekstblokkDto(
            id = requireNotNull(t.id) { "Tekstblokk uten id kan ikke mappes til DTO" },
            tittel = t.tittel,
            innhold = t.innhold,
            type = t.type,
            tags = t.tags.sorted(),
            sakstyper = t.sakstyper.map { it.kode }.sorted(),
            behandlingstemaer = t.behandlingstemaer.map { it.kode }.sorted(),
            status = t.status.name,
            registrertDato = t.registrertDato,
            registrertAv = t.registrertAv,
            endretDato = t.endretDato,
            endretAv = t.endretAv,
        )
    }
}
