package no.nav.melosys.integrasjon.hendelser

import java.time.LocalDateTime

enum class RapportType {
    FORSTE_GANG,  // Første vedtak for dette året
    ENDRING,      // Nytt vedtak med forskjellig beløp for samme år
    AVGANG        // Bruker har ikke betalt, PGI skal fjernes (PGI = 0)
}

data class PensjonsopptjeningHendelse(
    val fnr: String,
    val pgi: Long,
    val inntektsAr: Int,
    val fastsattTidspunkt: LocalDateTime,
    val rapportType: RapportType,
    val vedtakId: String?
)
