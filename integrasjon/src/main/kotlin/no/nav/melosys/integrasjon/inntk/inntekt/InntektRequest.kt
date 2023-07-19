package no.nav.melosys.integrasjon.inntk.inntekt

import java.time.LocalDate
import java.time.YearMonth

data class InntektRequest(
    val ainntektsfilter: String? = null,
    val filterversjon: LocalDate? = null,
    val formaal: String? = null,
    val ident: Aktoer? = null,
    val maanedFom: YearMonth? = null,
    val maanedTom: YearMonth? = null
)
