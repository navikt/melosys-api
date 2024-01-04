package no.nav.melosys.saksflytapi.domain

class SedLåsReferanse(val låsReferanse: String) : LåsReferanse {
    val rinaSaksnummer: String
    val sedID: String
    val sedVersjon: String

    init {
        require(erGyldigReferanse(låsReferanse)) { "$låsReferanse er ikke gyldig SED-referanse" }
        låsReferanse.split("_").let {
            rinaSaksnummer = it[0]
            sedID = it[1]
            sedVersjon = it[2]
        }
    }

    override val referanse: String
        get() = rinaSaksnummer

    override fun skalSettesPåVent(aktiveLåsReferanser: Collection<String>): Boolean {
        if (aktiveLåsReferanser.isEmpty()) return false

        // Det må nå sjekkes at det ikke finnes prossess med nøyaktig samme låsreferanse siden sed mottak
        // lager nye prosesser med samme låsreferanse som forelderen
        // Tenker fiksen blir å legge på postfix på subprosesser som lages. Når det er gjort kan denne sjekken fjernes
        return !aktiveLåsReferanser.contains(låsReferanse)
    }

    override fun toString(): String = låsReferanse

    private fun erGyldigReferanse(referanse: String): Boolean =
        LåsReferanseType.SED.erGyldigReferanse(referanse)
}
