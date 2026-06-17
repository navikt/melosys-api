package no.nav.melosys.service.popp

import mu.KotlinLogging
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId

private val log = KotlinLogging.logger { }
private val OSLO: ZoneId = ZoneId.of("Europe/Oslo")

fun String?.toOsloLocalDate(): LocalDate? =
    this?.takeIf { it.isNotBlank() }?.let { raw ->
        // POPP changeStamp er en fri String. Vanligst er ISO med offset/Z (konverteres til Oslo-dato),
        // men vi tåler også offset-løs LocalDateTime og ren dato slik at en formatvariasjon fra POPP
        // ikke fører til at vi stille mister registrert/oppdatert.
        runCatching { OffsetDateTime.parse(raw).atZoneSameInstant(OSLO).toLocalDate() }
            .recoverCatching { LocalDateTime.parse(raw).toLocalDate() }
            .recoverCatching { LocalDate.parse(raw) }
            .getOrElse {
                log.warn { "Kunne ikke tolke POPP changeStamp-dato '$raw' — settes til null" }
                null
            }
    }
