package no.nav.melosys.integrasjon.utbetaldata.utbetaling

data class UtbetalingRequest(
    val ident: String,
    val periode: Periode,
    val periodetype: String,
    val rolle: String
)
