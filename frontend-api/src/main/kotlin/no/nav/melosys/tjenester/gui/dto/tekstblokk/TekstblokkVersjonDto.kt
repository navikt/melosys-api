package no.nav.melosys.tjenester.gui.dto.tekstblokk

import java.time.LocalDateTime

import no.nav.melosys.service.tekstblokk.Endringstype
import no.nav.melosys.service.tekstblokk.TekstblokkVersjon

/**
 * Én revisjon av en tekstblokk. Versjonsnummeret er per blokk og telles opp av
 * servicen – Envers' revisjonsnummer deles med alle andre auditerte entiteter.
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
        )
    }
}
