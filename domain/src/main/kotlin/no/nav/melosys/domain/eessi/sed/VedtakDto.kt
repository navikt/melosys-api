package no.nav.melosys.domain.eessi.sed

import java.time.LocalDate

data class VedtakDto(
    val erFørstegangsvedtak: Boolean,
    val datoForrigeVedtak: LocalDate?
)
