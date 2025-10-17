package no.nav.melosys.integrasjon.hendelser

import java.time.LocalDateTime

data class PensjonsopptjeningHendelse(
    val fnr: String,
    val pgi: Long,
    val inntektsAr: Int,
    val fastsattTidspunkt: LocalDateTime,
    val endringstype: Endringstype,
    val melosysBehandlingID: String
) {
    enum class Endringstype {
        NY_INNTEKT,  // Pensjonsgivende inntekt fra første vedtak for dette året
        OPPDATERING, // Nytt vedtak med forskjellig beløp for samme år
        FJERNING     // Bruker har ikke betalt, PGI skal fjernes (PGI = 0)
    }
}
