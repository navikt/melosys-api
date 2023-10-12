package no.nav.melosys.integrasjon.inntk.inntekt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.jpa.SaksopplysningDokumentConverter
import no.nav.melosys.exception.IkkeFunnetException
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class InntektDocumentConvertTest {

    @Test
    fun `sjekk json resulat fra konvertering skal mot fasit fra fil`() {
        val inntektResponse = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .readValue<InntektResponse>(hentRessurs("mock/inntekt/inntektConsumerResponse.json"))

        val saksopplysning = InntektKonverter().lagSaksopplysning(inntektResponse)
        val convertToDatabaseColumn = SaksopplysningDokumentConverterForTest().convertToDatabaseColumn(saksopplysning.dokument)

        convertToDatabaseColumn.trim().shouldBe(hentRessurs("mock/inntekt/InntektDocumentConverterResult.json").trim())
    }

    fun hentRessurs(fil: String): String = InntekKonverterTest::class.java.classLoader.getResource(fil)
        ?.readText(StandardCharsets.UTF_8) ?: throw IkkeFunnetException("Fant ikke $fil")

    class SaksopplysningDokumentConverterForTest : SaksopplysningDokumentConverter() {
        override fun getObjectMapper(): ObjectMapper {
            return super.getObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true)
        }
    }
}
