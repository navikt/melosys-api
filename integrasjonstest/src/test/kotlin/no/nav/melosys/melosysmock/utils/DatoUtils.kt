package no.nav.melosys.melosysmock.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

val datatypeFactory = DatatypeFactory.newInstance()

fun tilXmlGregorianCalendar(localDate: LocalDate): XMLGregorianCalendar {
    return datatypeFactory.newXMLGregorianCalendar(
        OffsetDateTime.of(localDate, LocalTime.now(), OffsetDateTime.now().offset).toString()
    )
}
fun tilXmlGregorianCalendar(localDateTime: LocalDateTime): XMLGregorianCalendar {
    return datatypeFactory.newXMLGregorianCalendar(
        OffsetDateTime.of(localDateTime, OffsetDateTime.now().offset).toString()
    )
}
