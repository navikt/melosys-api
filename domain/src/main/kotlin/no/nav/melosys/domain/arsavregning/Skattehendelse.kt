package no.nav.melosys.domain.arsavregning

data class Skattehendelse(
    val gjelderPeriode: String,
    val identifikator: String,
    val sekvensnummer: Int,
    val somAktoerid: Boolean
)
