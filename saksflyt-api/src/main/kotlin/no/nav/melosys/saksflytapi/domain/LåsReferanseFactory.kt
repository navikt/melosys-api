package no.nav.melosys.saksflytapi.domain

import no.nav.melosys.domain.manglendebetaling.ManglendeFakturabetalingMelding

object LåsReferanseFactory {

    fun lagLåsReferanse(låsReferanse: String): LåsReferanse {
        val låsReferanseType: LåsReferanseType = LåsReferanseType.values().find {
            it.erGyldigReferanse(låsReferanse)
        } ?: throw IllegalArgumentException("$låsReferanse er ikke gyldig låsreferanse")

        return when (låsReferanseType) {
            LåsReferanseType.SED -> SedLåsReferanse(låsReferanse)
            LåsReferanseType.UBETALT -> ManglendeInnbetalingBehandlingLåsReferanse(låsReferanse)
        }
    }

    fun harSammeReferanse(lhsLåsReferanse: String, rhsLåsReferanse: String): Boolean =
        lagLåsReferanse(lhsLåsReferanse).gruppePrefiks == lagLåsReferanse(rhsLåsReferanse).gruppePrefiks

    @JvmStatic
    fun lagString(manglendeFakturabetalingMelding: ManglendeFakturabetalingMelding): String =
        manglendeFakturabetalingMelding.let {
            "${LåsReferanseType.UBETALT}_${it.fakturaserieReferanse}_${it.fakturanummer}"
        }
}
