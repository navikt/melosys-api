package no.nav.melosys.domain.serializer

import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.trygdeavtale.Lovvalgsbestemmelser_trygdeavtale_gb
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LovvalgBestemmelseDeserializerTest {

    private lateinit var mapper: JsonMapper

    @BeforeEach
    fun setUp() {
        mapper = JsonMapper.builder()
            .addModule(SimpleModule().apply {
                addDeserializer(LovvalgBestemmelse::class.java, LovvalgBestemmelseDeserializer())
            })
            .build()
    }

    @Test
    fun `skal deserialisere gyldig kode til korrekt LovvalgBestemmelse`() {
        val result = mapper.readValue("\"UK_ART7_3\"", LovvalgBestemmelse::class.java)

        result shouldBe Lovvalgsbestemmelser_trygdeavtale_gb.UK_ART7_3
    }

    @Test
    fun `skal deserialisere til riktig kode`() {
        val result = mapper.readValue("\"UK_ART7_3\"", LovvalgBestemmelse::class.java)

        result.kode shouldBe "UK_ART7_3"
    }
}
