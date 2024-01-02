package no.nav.melosys.saksflytapi.domain

import no.nav.melosys.domain.manglendebetaling.ManglendeFakturabetalingMelding

object LåsReferanseFactory {

    fun lagLåsReferanse(referanse: String): LåsReferanse {
        val låsReferanseType: LåsReferanseType = LåsReferanseType.values().find {
            referanse.matches(Regex(it.prefixRegexString))
        } ?: throw IllegalArgumentException("$referanse er ikke gyldig låsreferanse")

        return when (låsReferanseType) {
            LåsReferanseType.SED -> SedLåsReferanse(referanse)
            LåsReferanseType.OMIB -> OpprettManglendeInnbetalingBehandlingLåsReferanse(referanse)
        }
    }

    @JvmStatic
    fun lagStringFraManglendeFakturabetalingMelding(manglendeFakturabetalingMelding: ManglendeFakturabetalingMelding): String =
        manglendeFakturabetalingMelding.let {
            "${LåsReferanseType.OMIB}_${it.fakturaserieReferanse}_${it.fakturanummer}"
        }
}
