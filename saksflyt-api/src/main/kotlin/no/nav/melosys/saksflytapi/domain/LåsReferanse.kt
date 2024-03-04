package no.nav.melosys.saksflytapi.domain

interface LåsReferanse {
    val gruppePrefiks: String
    fun skalSettesPåVent(aktiveLåsReferanser : Collection<String>  ) : Boolean

    override fun toString(): String
}
