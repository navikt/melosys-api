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

    @Test
    fun `tilMetaforceLinjeskift erstatter newlines med pilcrow-markør`() {
        BrevMapperUtils.tilMetaforceLinjeskift("Linje 1\nLinje 2\nLinje 3") shouldBe "Linje 1[_¶_]Linje 2[_¶_]Linje 3"
    }

    @Test
    fun `tilMetaforceLinjeskift returnerer null for null-input`() {
        BrevMapperUtils.tilMetaforceLinjeskift(null) shouldBe null
    }

    @Test
    fun `tilMetaforceLinjeskift returnerer uendret tekst uten newlines`() {
        BrevMapperUtils.tilMetaforceLinjeskift("Ingen linjeskift her") shouldBe "Ingen linjeskift her"
    }
}
