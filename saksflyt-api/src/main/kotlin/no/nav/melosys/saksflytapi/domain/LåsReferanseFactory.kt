package no.nav.melosys.saksflytapi.domain

object LåsReferanseFactory {
    enum class LåsReferanseType(val prefixRegExString: String) {
        SED("^\\d+_[a-zA-Z0-9]+_\\d+"),
        OMIB("^OMIB_.*") // TODO finn ut hva OMIB-referanse skal være
    }

    fun låsReferanseFraString(referanse: String): LåsReferanse {
        val låsReferanseType: LåsReferanseType = LåsReferanseType.values().find {
            referanse.matches(Regex(it.prefixRegExString))
        } ?: throw IllegalArgumentException("$referanse er ikke gyldig låsreferanse")

        return when (låsReferanseType) {
            LåsReferanseType.SED -> SedLåsReferanse(referanse)
            LåsReferanseType.OMIB -> OpprettManglendeInnbetalingBehandlingLåsReferanse(referanse)
        }
    }
}
