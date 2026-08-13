package no.nav.melosys.saksflytapi.domain

class SøknadLåsReferanse(private val låsReferanse: String) : LåsReferanse {

    init {
        require(LåsReferanseType.SØKNAD.erGyldigReferanse(låsReferanse)) {
            "$låsReferanse er ikke gyldig SØKNAD-referanse ({gruppeId}_{skjemaId})"
        }
    }

    /**
     * gruppeId-delen (inkl. skilletegn) — alle relaterte deler av samme søknad deler denne, slik at
     * de serialiseres mot hverandre (én flyt om gangen per gruppe, på tvers av instanser). skjemaId-delen
     * holder hele referansen unik per del for redelivery-dedup.
     */
    override val gruppePrefiks: String
        get() = låsReferanse.substringBefore("_") + "_"

    override fun skalSettesPåVent(aktiveLåsReferanser: Collection<String>): Boolean =
        aktiveLåsReferanser.isNotEmpty()

    override fun toString(): String = låsReferanse
}
