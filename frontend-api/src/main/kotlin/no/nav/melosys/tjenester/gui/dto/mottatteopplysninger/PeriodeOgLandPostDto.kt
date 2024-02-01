package no.nav.melosys.tjenester.gui.dto.mottatteopplysninger

import java.time.LocalDate

@JvmRecord
data class PeriodeOgLandPostDto(@JvmField val fom: LocalDate, @JvmField val tom: LocalDate, @JvmField val land: List<String>)
