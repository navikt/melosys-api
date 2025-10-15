package no.nav.melosys.integrasjon.hendelser

import java.time.LocalDateTime

enum class RapportType {
    FORSTE_GANG,  // First decision for this year
    ENDRING,      // New decision with different amount for same year
    AVGANG        // User hasn't paid, PGI should be removed (PGI = 0)
}

data class PensjonsopptjeningHendelse(
    val fnr: String,
    val pgi: Long,
    val inntektsAr: Int,
    val fastsattTidspunkt: LocalDateTime,
    val rapportType: RapportType,
    val vedtakId: String?
)
