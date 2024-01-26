package no.nav.melosys.tjenester.gui.dto.utpeking;
import no.nav.melosys.domain.Utpekingsperiode
import no.nav.melosys.domain.jpa.LovvalgBestemmelsekonverterer
import no.nav.melosys.domain.kodeverk.Land_iso2
import java.time.LocalDate

data class UtpekingsperiodeDto(
    val fomDato: LocalDate,
    val tomDato: LocalDate,
    val lovvalgsbestemmelse: String,
    val tilleggsbestemmelse: String?,
    val lovvalgsland: String
) {
    companion object {
        private val konverterer = LovvalgBestemmelsekonverterer()

        fun av(utpekingsperiode: Utpekingsperiode): UtpekingsperiodeDto {
            return UtpekingsperiodeDto(
                utpekingsperiode.fom,
                utpekingsperiode.tom,
                utpekingsperiode.bestemmelse.name(),
                utpekingsperiode.tilleggsbestemmelse?.name(),
            utpekingsperiode.lovvalgsland.name
            )
        }
    }

    fun tilDomene(): Utpekingsperiode {
        return Utpekingsperiode(
            fomDato,
            tomDato,
            enumVerdiEllerNull(Land_iso2::class.java, lovvalgsland),
        konverterer.convertToEntityAttribute(lovvalgsbestemmelse),
            konverterer.convertToEntityAttribute(tilleggsbestemmelse)
        )
    }

    private fun <E : Enum<E>> enumVerdiEllerNull(enumKlasse: Class<E>, nøkkel: String?): E? {
        return nøkkel?.let {
            java.lang.Enum.valueOf(enumKlasse, it)
        }
    }
}
