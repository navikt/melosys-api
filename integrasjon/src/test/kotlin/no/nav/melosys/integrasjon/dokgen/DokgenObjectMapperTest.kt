package no.nav.melosys.integrasjon.dokgen

import tools.jackson.databind.json.JsonMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import no.nav.melosys.domain.Behandlingsmaate
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Verifiserer at JsonMapper-konfigurasjonen i DokgenClientProducer er korrekt.
 * Dokgen forventer enkle strings for Kodeverk-enums – ikke {"kode":"...","term":"..."} objekter
 * som MelosysModule produserer. Mapperen skal derfor IKKE ha MelosysModule registrert.
 */
class DokgenObjectMapperTest {

    // Speiler konfigurasjonen i DokgenClientProducer
    private val objectMapper = JsonMapper.builder().build()

    @Test
    fun `Kodeverk-enum skal serialiseres som plain string, ikke KodeDto-objekt`() {
        val json = objectMapper.writeValueAsString(Behandlingsmaate.MANUELT)

        json shouldBe "\"MANUELT\""
        json shouldNotContain "kode"
        json shouldNotContain "term"
    }

    @Test
    fun `data class med Kodeverk-felt skal serialisere enum som plain string`() {
        val dto = TestDto(Behandlingsmaate.AUTOMATISERT)

        val tree = objectMapper.readTree(objectMapper.writeValueAsString(dto))

        tree.get("behandlingsmaate").isTextual shouldBe true
        tree.get("behandlingsmaate").asText() shouldBe "AUTOMATISERT"
    }

    private data class TestDto(val behandlingsmaate: Behandlingsmaate)
}
