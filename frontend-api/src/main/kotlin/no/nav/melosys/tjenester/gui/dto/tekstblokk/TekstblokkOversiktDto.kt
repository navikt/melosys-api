package no.nav.melosys.tjenester.gui.dto.tekstblokk

import java.time.Instant
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkOversikt
import no.nav.melosys.domain.brev.tekstblokk.TekstblokkType

/**
 * DTO for liste-visning. Inkluderer innhold slik at frontend kan søke i
 * brødteksten og vise forhåndsvisning uten et ekstra kall per blokk.
 */
data class TekstblokkOversiktDto(
    val id: Long,
    val tittel: String,
    val innhold: String,
    val type: TekstblokkType,
    val tags: List<String>,
    val sakstyper: List<String>,
    val behandlingstemaer: List<String>,
    val status: String,
    val endretDato: Instant,
    val endretAv: String,
    val endretAvNavn: String?,
) {
    companion object {
        fun av(o: TekstblokkOversikt): TekstblokkOversiktDto = TekstblokkOversiktDto(
            id = o.id,
            tittel = o.tittel,
            innhold = o.innhold,
            type = o.type,
            tags = o.tags.sorted(),
            sakstyper = o.sakstyper.map { it.kode }.sorted(),
            behandlingstemaer = o.behandlingstemaer.map { it.kode }.sorted(),
            status = o.status.name,
            endretDato = o.endretDato,
            endretAv = o.endretAv,
            endretAvNavn = o.endretAvNavn,
        )
    }
}
