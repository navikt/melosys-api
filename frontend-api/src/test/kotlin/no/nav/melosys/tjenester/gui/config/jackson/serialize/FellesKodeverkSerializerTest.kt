package no.nav.melosys.tjenester.gui.config.jackson.serialize

import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.domain.dokument.felles.KodeverkHjelper
import no.nav.melosys.service.kodeverk.KodeverkService
import org.junit.jupiter.api.Test

class FellesKodeverkSerializerTest {

    private val kodeverkService = mockk<KodeverkService>()
    private val serializer = FellesKodeverkSerializer(kodeverkService)
    private val generator = mockk<JsonGenerator>(relaxed = true)
    private val provider = mockk<SerializationContext>()

    @Test
    fun `serialize should write kode and term for valid kode`() {
        val kodeverkHjelper = testKodeverkHjelper("NO")
        every { kodeverkService.dekod(FellesKodeverk.LANDKODER, "NO") } returns "Norge"

        serializer.serialize(kodeverkHjelper, generator, provider)

        verifyOrder {
            generator.writeStartObject()
            generator.writeStringProperty("kode", "NO")
            generator.writeStringProperty("term", "Norge")
            generator.writeEndObject()
        }
    }

    @Test
    fun `serialize should write null kode and null term when kode is null`() {
        val kodeverkHjelper = testKodeverkHjelper(null)

        serializer.serialize(kodeverkHjelper, generator, provider)

        verifyOrder {
            generator.writeStartObject()
            generator.writeStringProperty("kode", null)
            generator.writeStringProperty("term", null)
            generator.writeEndObject()
        }
        verify(exactly = 0) { kodeverkService.dekod(any(), any()) }
    }

    @Test
    fun `serialize should write null kode and null term when kode is empty string`() {
        val kodeverkHjelper = testKodeverkHjelper("")

        serializer.serialize(kodeverkHjelper, generator, provider)

        verifyOrder {
            generator.writeStartObject()
            generator.writeStringProperty("kode", null)
            generator.writeStringProperty("term", null)
            generator.writeEndObject()
        }
        verify(exactly = 0) { kodeverkService.dekod(any(), any()) }
    }

    private fun testKodeverkHjelper(kode: String?): KodeverkHjelper = object : KodeverkHjelper {
        override val kode: String? = kode
        override fun hentKodeverkNavn() = FellesKodeverk.LANDKODER
    }
}
