package no.nav.melosys.domain.manglendebetaling

import java.time.LocalDate

data class ManglendeFakturabetalingMelding(
    val fakturaserieReferanse: String,
    val betalingsstatus: Betalingsstatus,
    val datoMottatt: LocalDate,
    val fakturanummer: String
)


