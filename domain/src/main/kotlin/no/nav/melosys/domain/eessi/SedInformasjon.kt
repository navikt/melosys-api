package no.nav.melosys.domain.eessi

import java.time.LocalDate

/**
 * Metadata om en SED (Structured Electronic Document).
 */
data class SedInformasjon(
    val bucId: String?,
    val sedId: String?,
    val opprettetDato: LocalDate,
    val sistOppdatert: LocalDate,
    val sedType: String?,
    val status: String?,
    val rinaUrl: String?
) {
    fun erAvbrutt(): Boolean = STATUS_AVBRUTT.equals(status, ignoreCase = true)

    fun erAktiv(): Boolean = !erAvbrutt()

    companion object {
        const val STATUS_AVBRUTT = "AVBRUTT"
    }
}
