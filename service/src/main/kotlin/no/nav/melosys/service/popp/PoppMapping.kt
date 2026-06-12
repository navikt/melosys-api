package no.nav.melosys.service.popp

import mu.KotlinLogging
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

private val log = KotlinLogging.logger { }
private val OSLO: ZoneId = ZoneId.of("Europe/Oslo")

fun String?.toOsloLocalDate(): LocalDate? =
    this?.takeIf { it.isNotBlank() }?.let { raw ->
        runCatching { OffsetDateTime.parse(raw).atZoneSameInstant(OSLO).toLocalDate() }
            .getOrElse {
                log.warn { "Kunne ikke tolke POPP changeStamp-dato '$raw' — settes til null" }
                null
            }
    }
