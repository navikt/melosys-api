package no.nav.melosys.domain.dokument.inntekt

import java.time.YearMonth

data class Avvik(
    val ident: String,
    val opplysningspliktigID: String,
    val virksomhetID: String,
    val avvikPeriode: YearMonth,
    val tekst: String,
)
