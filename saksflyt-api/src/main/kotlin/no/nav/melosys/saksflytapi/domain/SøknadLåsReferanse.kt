package no.nav.melosys.saksflytapi.domain

import java.util.UUID

/**
 * Låsreferanse for mottak av digital søknad: `{gruppe}_{skjemaId}`.
 *
 * Gruppen er skjemaId-en til første relaterte skjema som ble mottatt i Melosys. Relaterte innsendinger får
 * samme gruppePrefiks og behandles derfor én om gangen. Det gamle formatet (`{skjemaId}`) støttes fortsatt,
 * med skjemaId som gruppePrefiks.
 */
class SøknadLåsReferanse(private val låsReferanse: String) : LåsReferanse {

    init {
        require(LåsReferanseType.SØKNAD.erGyldigReferanse(låsReferanse)) {
            "$låsReferanse er ikke gyldig SØKNAD-referanse (UUID eller UUID_UUID)"
        }
    }

    override val gruppePrefiks: String
        get() = låsReferanse.substringBefore(SKILLETEGN)

    val skjemaId: UUID
        get() = UUID.fromString(låsReferanse.substringAfter(SKILLETEGN))

    override fun skalSettesPåVent(aktiveLåsReferanser: Collection<String>): Boolean =
        aktiveLåsReferanser.isNotEmpty()

    override fun toString(): String = låsReferanse

    companion object {
        private const val SKILLETEGN = "_"

        @JvmStatic
        fun lag(gruppe: UUID, skjemaId: UUID): SøknadLåsReferanse = SøknadLåsReferanse("${gruppe}$SKILLETEGN$skjemaId")
    }
}
