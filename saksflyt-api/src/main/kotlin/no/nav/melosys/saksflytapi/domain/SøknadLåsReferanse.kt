package no.nav.melosys.saksflytapi.domain

class SøknadLåsReferanse(private val låsReferanse: String) : LåsReferanse {

    init {
        require(LåsReferanseType.SØKNAD.erGyldigReferanse(låsReferanse)) {
            "$låsReferanse er ikke gyldig SØKNAD-referanse (UUID)"
        }
    }

    override val gruppePrefiks: String
        get() = låsReferanse

    override fun skalSettesPåVent(aktiveLåsReferanser: Collection<String>): Boolean =
        aktiveLåsReferanser.isNotEmpty()

    override fun toString(): String = låsReferanse
}
