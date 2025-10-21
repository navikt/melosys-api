package no.nav.melosys.integrasjon.hendelser

import java.time.LocalDateTime
import java.util.UUID

data class PensjonsopptjeningHendelse(
    val hendelsesId: String,
    val correlationId: String,
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

    companion object {
        /**
         * Genererer en deterministisk hendelsesId basert på behandlingID og inntektsår
         * for å sikre idempotens ved retry av saga-steget.
         */
        fun genererHendelsesId(behandlingId: Long, inntektsAr: Int): String {
            return UUID.nameUUIDFromBytes("$behandlingId-$inntektsAr".toByteArray()).toString()
        }
    }
}
