package no.nav.melosys.saksflytapi.domain

class OpprettManglendeInnbetalingBehandlingLåsReferanse(val låsReferanse: String) : LåsReferanse {

    override val referanse: String
        get() = TODO("Not yet implemented")

    override fun skalSettesPåVent(aktiveLåsReferanser: Collection<String>): Boolean {
        TODO("Not yet implemented")
    }

    override fun toString(): String = referanse
}
