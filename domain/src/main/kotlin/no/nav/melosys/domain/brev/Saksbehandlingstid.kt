package no.nav.melosys.domain.brev

import java.time.Instant
import java.time.Period

object Saksbehandlingstid {
    const val SAKSBEHANDLINGSTID_UKER: Int = 12

    fun beregnSaksbehandlingsfrist(forsendelseMottatt: Instant?): Instant =
        forsendelseMottatt?.plus(Period.ofWeeks(SAKSBEHANDLINGSTID_UKER)) ?: error(
            "Forsendelse mottatt dato kan ikke være null når saksbehandlingstid skal beregnes"
        )
}
