package no.nav.melosys.domain.brev

import java.time.Instant
import java.time.Period

object DokumentasjonSvarfrist {

    private const val DOKUMENTASJON_SVARFRIST_MANGELBREV_UKER = 4
    private const val DOKUMENTASJON_SVARFRIST_UKER = 2

    @JvmStatic
    fun beregnFristPaaMangelbrevFraDagensDato(): Instant =
        Instant.now().plus(Period.ofWeeks(DOKUMENTASJON_SVARFRIST_MANGELBREV_UKER))

    @JvmStatic
    fun beregnFristFraDagensDato(): Instant =
        Instant.now().plus(Period.ofWeeks(DOKUMENTASJON_SVARFRIST_UKER))
}
