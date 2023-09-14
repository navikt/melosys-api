package no.nav.melosys.domain.ftrl

import java.time.LocalDate

data class ManglendeFakturabetalingMelding(
    val fakturaserieReferanse: String,
    val mottaksDato: LocalDate
)


