package no.nav.melosys.tjenester.gui.dto.anmodning

import no.nav.melosys.domain.AnmodningsperiodeSvar
import no.nav.melosys.domain.kodeverk.Anmodningsperiodesvartyper
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto
import java.time.LocalDate

data class AnmodningsperiodeSvarDto(
    val anmodningsperiodeSvarType: String?,
    val endretPeriode: PeriodeDto,
    val begrunnelseFritekst: String?
) {
    companion object {
        @JvmStatic
        fun tom(): AnmodningsperiodeSvarDto {
            return AnmodningsperiodeSvarDto(null, PeriodeDto(), null)
        }

        @JvmStatic
        fun av(anmodningsperiodeSvar: AnmodningsperiodeSvar): AnmodningsperiodeSvarDto {
            return AnmodningsperiodeSvarDto(
                anmodningsperiodeSvar.anmodningsperiodeSvarType.kode,
                PeriodeDto(anmodningsperiodeSvar.innvilgetFom, anmodningsperiodeSvar.innvilgetTom),
                anmodningsperiodeSvar.begrunnelseFritekst
            )
        }
    }

    fun til(): AnmodningsperiodeSvar {
        return AnmodningsperiodeSvar(
            null,
            enumVerdiEllerNull(Anmodningsperiodesvartyper::class.java, anmodningsperiodeSvarType),
        LocalDate.now(),
            begrunnelseFritekst,
            endretPeriode.fom,
            endretPeriode.tom
        )
    }

    private fun <E : Enum<E>> enumVerdiEllerNull(enumKlasse: Class<E>, nøkkel: String?): E? {
        return nøkkel?.let {
            java.lang.Enum.valueOf(enumKlasse, it)
        }
    }
}
