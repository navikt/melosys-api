package no.nav.melosys.integrasjon.inntekt

import java.time.YearMonth

data class InntektRequest(
    val ainntektsfilter: String,
    val filterversjon: String? = null,
    val formaal: String,
    val ident: Aktoer,
    val maanedFom: YearMonth,
    val maanedTom: YearMonth
)
