package no.nav.melosys.integrasjon.faktureringskomponenten.dto

import java.math.BigDecimal
import java.time.LocalDate

data class FakturaDto(
    val fodselsnummer: String,
    val fakturaserieReferanse: String?,
    val fullmektig: FullmektigDto?,
    val referanseBruker: String,
    val referanseNAV: String,
    val fakturaGjelderInnbetalingstype: Innbetalingstype,
    val belop: BigDecimal,
    val startDato: LocalDate,
    val sluttDato: LocalDate,
    val beskrivelse: String
)
