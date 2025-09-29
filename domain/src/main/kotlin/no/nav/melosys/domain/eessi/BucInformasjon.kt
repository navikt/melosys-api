package no.nav.melosys.domain.eessi

import java.time.LocalDate

/**
 * Metadata om en BUC (Business Use Case) i EESSI-systemet.
 */
data class BucInformasjon(
    val id: String?,
    val erÅpen: Boolean,
    val bucType: String?,
    val opprettetDato: LocalDate,
    val mottakerinstitusjoner: Set<String>?,
    val seder: List<SedInformasjon>
) {
    /**
     * Eksplisitt metode for Java interop og bakoverkompatibilitet.
     * Java-kode forventer erÅpen() metode (ikke getErÅpen()).
     */
    fun erÅpen(): Boolean = erÅpen
}
