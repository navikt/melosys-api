package no.nav.melosys.tjenester.gui.helseutgiftdekkesperiode.dto

import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import java.time.LocalDate

data class HelseutgiftDekkesPeriodeDto(
    val fomDato: LocalDate,
    val tomDato: LocalDate?,
    val bostedsland: String,
) {
    companion object {
        fun av(helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode): HelseutgiftDekkesPeriodeDto {
            return HelseutgiftDekkesPeriodeDto(
                fomDato = helseutgiftDekkesPeriode.fomDato,
                tomDato = helseutgiftDekkesPeriode.tomDato,
                bostedsland = helseutgiftDekkesPeriode.bostedsland
            )
        }
    }
}
