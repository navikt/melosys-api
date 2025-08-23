package no.nav.melosys.service.dokument.brev.mapper.felles

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

class BrevMapperUtilsTest {

    @Test
    fun `convertToXMLGregorianCalendarRemoveTimezone skal konvertere dato korrekt`() {
        val localDate = LocalDate.parse("2019-04-01")
        val april_1 = localDate.atStartOfDay(ZoneId.of("Europe/Paris")).toInstant()
        val xmlGregorianCalendar = BrevMapperUtils.convertToXMLGregorianCalendarRemoveTimezone(april_1)
        xmlGregorianCalendar.day shouldBe 1
    }
}
