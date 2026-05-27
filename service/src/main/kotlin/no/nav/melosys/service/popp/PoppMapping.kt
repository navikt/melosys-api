package no.nav.melosys.service.popp

import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

private val OSLO: ZoneId = ZoneId.of("Europe/Oslo")

fun Date?.toOsloLocalDate(): LocalDate? =
    this?.toInstant()?.atZone(OSLO)?.toLocalDate()
