package no.nav.melosys.domain.ftrl

import java.time.LocalDate

data class ManglendeFakturabetalingMelding(
    val behandlingId: Long,
    val mottaksDato: LocalDate
)


