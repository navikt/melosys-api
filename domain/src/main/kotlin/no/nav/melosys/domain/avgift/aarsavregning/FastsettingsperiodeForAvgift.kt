package no.nav.melosys.domain.avgift.aarsavregning

import no.nav.melosys.domain.ErPeriode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Bestemmelse
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import java.time.LocalDate

data class FastsettingsperiodeForAvgift(
    val periodeFra: LocalDate,
    val periodeTil: LocalDate,
    val dekning: Trygdedekninger? = null,
    val bestemmelse: Bestemmelse? = null,
    val medlemskapstyper: Medlemskapstyper? = null,
) : ErPeriode {
    constructor(medlemskapsperiode: Medlemskapsperiode) : this(
        periodeFra = medlemskapsperiode.fom,
        periodeTil = medlemskapsperiode.tom,
        dekning = medlemskapsperiode.trygdedekning,
        bestemmelse = medlemskapsperiode.bestemmelse,
        medlemskapstyper = medlemskapsperiode.medlemskapstype
    )
    constructor(helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode) : this(
        periodeFra = helseutgiftDekkesPeriode.fomDato,
        periodeTil = helseutgiftDekkesPeriode.tomDato,
    )

    constructor(gjeldendeÅr: Int, helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode) : this(
        periodeFra = avkortFraOgMedDatoForÅr(gjeldendeÅr, helseutgiftDekkesPeriode.fomDato),
        periodeTil = avkortTilOgMedDatoForÅr(gjeldendeÅr, helseutgiftDekkesPeriode.tomDato),
    )

    constructor(gjeldendeÅr: Int, medlemskapsperiode: Medlemskapsperiode) : this(
        periodeFra = avkortFraOgMedDatoForÅr(gjeldendeÅr, medlemskapsperiode.fom),
        periodeTil = avkortTilOgMedDatoForÅr(gjeldendeÅr, medlemskapsperiode.tom),
        dekning = medlemskapsperiode.trygdedekning,
        bestemmelse = medlemskapsperiode.bestemmelse,
        medlemskapstyper = medlemskapsperiode.medlemskapstype
    )
    override fun getTom(): LocalDate = periodeTil
    override fun getFom(): LocalDate = periodeFra
}

private fun avkortFraOgMedDatoForÅr(gjelderÅr: Int, fom: LocalDate): LocalDate = if (fom.year < gjelderÅr) {
    LocalDate.of(gjelderÅr, 1, 1)
} else fom

private fun avkortTilOgMedDatoForÅr(gjelderÅr: Int, tom: LocalDate): LocalDate = if (tom.year > gjelderÅr) {
    LocalDate.of(gjelderÅr, 12, 31)
} else tom
