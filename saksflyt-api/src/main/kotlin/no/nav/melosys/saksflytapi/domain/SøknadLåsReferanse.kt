package no.nav.melosys.saksflytapi.domain

class SøknadLåsReferanse(private val låsReferanse: String) : LåsReferanse {

    init {
        require(LåsReferanseType.SØKNAD.erGyldigReferanse(låsReferanse)) {
            "$låsReferanse er ikke gyldig SØKNAD-referanse ({gruppeId}_{skjemaId} eller {skjemaId})"
        }
    }

    /**
     * gruppeId-delen (inkl. skilletegn) — alle relaterte deler av samme søknad deler denne, slik at
     * de serialiseres mot hverandre (én flyt om gangen per gruppe, på tvers av instanser). skjemaId-delen
     * holder hele referansen unik per del for redelivery-dedup.
     *
     * Referanser i det gamle formatet (bar skjemaId, fra før MELOSYS-8151) har ingen gruppedel og
     * beholder hele referansen som prefiks. En slik referanse matcher da både seg selv og nye
     * referanser på formen {sammeId}_{skjemaId} — altså gruppen der skjemaet er gruppe-ID. Det er
     * ønsket: en gammel og en ny prosessinstans i samme gruppe skal fortsatt serialiseres.
     */
    override val gruppePrefiks: String
        get() = if (låsReferanse.contains("_")) låsReferanse.substringBefore("_") + "_" else låsReferanse

    override fun skalSettesPåVent(aktiveLåsReferanser: Collection<String>): Boolean =
        aktiveLåsReferanser.isNotEmpty()

    override fun toString(): String = låsReferanse
}
