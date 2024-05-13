package no.nav.melosys.saksflytapi.domain

class ManglendeInnbetalingBehandlingLåsReferanse(val låsReferanse: String) : LåsReferanse {
    val fakturaserieReferanse: String
    val fakturanummer: String

    init {
        require(erGyldigReferanse(låsReferanse)) { "$låsReferanse er ikke gyldig OpprettManglendeInnbetalingBehandling-referanse" }
        låsReferanse.split("_").let {
            fakturaserieReferanse = it[1]
            fakturanummer = it[2]
        }
    }

    override val gruppePrefiks: String
        get() = "${LåsReferanseType.UBETALT}_${fakturaserieReferanse}"

    override fun skalSettesPåVent(aktiveLåsReferanser: Collection<String>): Boolean {
        return aktiveLåsReferanser.isNotEmpty()
    }

    override fun toString(): String = gruppePrefiks

    private fun erGyldigReferanse(referanse: String): Boolean =
        LåsReferanseType.UBETALT.erGyldigReferanse(referanse)
}
