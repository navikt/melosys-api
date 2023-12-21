package no.nav.melosys.saksflytapi.domain

class OpprettManglendeInnbetalingBehandlingLåsReferanse(val låsReferanse: String) : LåsReferanse {
    val fakturaserieReferanse: String
    val fakturanummer: String

    init {
        require(erGyldigReferanse(låsReferanse)) { "$låsReferanse er ikke gyldig OpprettManglendeInnbetalingBehandling-referanse" }
        låsReferanse.split("_").let {
            fakturaserieReferanse = it[1]
            fakturanummer = it[2]
        }
    }

    override val referanse: String
        get() = fakturaserieReferanse

    override fun skalSettesPåVent(aktiveLåsReferanser: Collection<String>): Boolean {
        return aktiveLåsReferanser.isNotEmpty()
    }

    override fun toString(): String = referanse

    private fun erGyldigReferanse(referanse: String): Boolean =
        LåsReferanseType.OMIB.erGyldigReferanse(referanse)
}
