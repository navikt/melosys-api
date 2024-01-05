package no.nav.melosys.saksflytapi.domain

import no.nav.melosys.domain.manglendebetaling.ManglendeFakturabetalingMelding

object LåsReferanseFactory {

    fun lagLåsReferanse(referanse: String): LåsReferanse {
        val låsReferanseType: LåsReferanseType = LåsReferanseType.values().find {
            it.erGyldigReferanse(referanse)
        } ?: throw IllegalArgumentException("$referanse er ikke gyldig låsreferanse")

        return when (låsReferanseType) {
            LåsReferanseType.SED -> SedLåsReferanse(referanse)
            LåsReferanseType.UBETALT -> OpprettManglendeInnbetalingBehandlingLåsReferanse(referanse)
        }
    }

    @JvmStatic
    fun lagStringFraManglendeFakturabetalingMelding(manglendeFakturabetalingMelding: ManglendeFakturabetalingMelding): String =
        manglendeFakturabetalingMelding.let {
            "${LåsReferanseType.UBETALT}_${it.fakturaserieReferanse}_${it.fakturanummer}"
        }
}
