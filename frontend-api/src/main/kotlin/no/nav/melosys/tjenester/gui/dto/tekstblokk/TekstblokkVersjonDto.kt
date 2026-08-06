package no.nav.melosys.tjenester.gui.dto.tekstblokk

import java.time.LocalDateTime

import no.nav.melosys.service.tekstblokk.Endringstype
import no.nav.melosys.service.tekstblokk.TekstblokkVersjon

/**
 * Én revisjon av en tekstblokk. Versjonsnummeret er per blokk og telles opp av
 * servicen – Envers' revisjonsnummer deles med alle andre auditerte entiteter.
 * Avgrensninger og status er med så web kan vise hva som skiller versjonene, og
 * leveres som rene koder på samme form som TekstblokkDto.
 */
data class TekstblokkVersjonDto(
    val versjon: Int,
    val gyldigFra: LocalDateTime,
    val gyldigTil: LocalDateTime?,
    val endretAv: String,
    val endretAvNavn: String?,
    val endringstype: Endringstype,
    val tittel: String,
    val innhold: String,
    val tags: List<String>,
    val sakstyper: List<String>,
    val sakstemaer: List<String>,
    val behandlingstemaer: List<String>,
    val status: String,
) {
    companion object {
        fun av(v: TekstblokkVersjon): TekstblokkVersjonDto = TekstblokkVersjonDto(
            versjon = v.versjon,
            gyldigFra = v.gyldigFra,
            gyldigTil = v.gyldigTil,
            endretAv = v.endretAv,
            endretAvNavn = v.endretAvNavn,
            endringstype = v.endringstype,
            tittel = v.tittel,
            innhold = v.innhold,
            tags = v.tags.sorted(),
            sakstyper = v.sakstyper.map { it.kode }.sorted(),
            sakstemaer = v.sakstemaer.map { it.kode }.sorted(),
            behandlingstemaer = v.behandlingstemaer.map { it.kode }.sorted(),
            status = v.status.name,
        )
    }
}
