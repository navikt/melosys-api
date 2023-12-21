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
        if (aktiveLåsReferanser.contains(låsReferanse)) {
            return false
        }
        // SedMottakTestIT og SaksflytOppstartIT feiler uten denne sjekken
        // Burde ikke være mulig da finnAndreAktiveLåsMedSammeReferanse bare skal retunere
        // liste filtrer på referanse
        return aktiveLåsReferanser.any { SedLåsReferanse(it).referanse == referanse }
    }

    override fun toString(): String = låsReferanse

    private fun erGyldigReferanse(referanse: String): Boolean =
        LåsReferanseType.SED.erGyldigReferanse(referanse)
}
