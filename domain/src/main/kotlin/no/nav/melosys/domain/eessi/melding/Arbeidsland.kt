package no.nav.melosys.domain.eessi.melding


data class Arbeidsland (
    val land: String,
    val arbeidssted: List<Arbeidssted?>? = emptyList(),
)
