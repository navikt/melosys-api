package no.nav.melosys.domain.eessi.melding

data class Arbeidssted(
    val navn: String,
    val adresse: Adresse,
    val hjemmebase: String?,
    val erIkkeFastAdresse: Boolean
) {
    constructor(navn: String, adresse: Adresse) : this(navn, adresse, null, false)
}
