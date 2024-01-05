package no.nav.melosys.saksflytapi.domain

interface LåsReferanse {
    val referanse: String
    fun skalSettesPåVent(aktiveLåsReferanser : Collection<String>  ) : Boolean

    override fun toString(): String
}
