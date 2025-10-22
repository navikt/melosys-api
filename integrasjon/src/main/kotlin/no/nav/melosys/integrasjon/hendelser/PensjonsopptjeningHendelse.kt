package no.nav.melosys.integrasjon.hendelser

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime
import java.util.UUID

/**
 * Hendelse som sendes til POPP (Pensjon Opptjening) for å rapportere pensjonsgivende inntekt
 * fra årsavregning av trygdeavgift for personer som ikke er skattepliktige til Norge.
 *
 * Denne hendelsen sendes etter at et årsavregningsvedtak er fattet for FTRL-saker
 * hvor bruker ikke er skattepliktig til Norge.
 *
 * @property hendelsesId Unik ID for hendelsen, generert deterministisk basert på behandlingID og inntektsår for idempotens
 * @property correlationId Sporings-ID for hendelsen
 * @property fnr Fødselsnummer/D-nummer for personen hendelsen gjelder
 * @property pgi Pensjonsgivende inntekt i kroner (brutto årsbeløp)
 * @property inntektsAr Hvilket år inntekten gjelder for
 * @property fastsattTidspunkt Tidspunkt når vedtaket ble fattet
 * @property endringstype Type endring (ny inntekt, oppdatering eller fjerning)
 * @property melosysBehandlingID ID på Melosys-behandlingen som ligger til grunn for vedtaket
 */
data class PensjonsopptjeningHendelse(
    val hendelsesId: String,
    val correlationId: String,
    val fnr: String,
    val pgi: Long,
    val inntektsAr: Int,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val fastsattTidspunkt: LocalDateTime,
    val endringstype: Endringstype,
    val melosysBehandlingID: Long
) {
    /**
     * Type endring for pensjonsopptjening.
     */
    enum class Endringstype {
        /** Pensjonsgivende inntekt fra første vedtak for dette året */
        NY_INNTEKT,

        /** Nytt vedtak med forskjellig beløp for samme år */
        OPPDATERING,

        /** Bruker har ikke betalt, PGI skal fjernes (PGI = 0) */
        FJERNING
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
