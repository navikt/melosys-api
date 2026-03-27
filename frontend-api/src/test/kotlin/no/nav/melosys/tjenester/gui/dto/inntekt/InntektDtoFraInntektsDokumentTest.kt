package no.nav.melosys.tjenester.gui.dto.inntekt

import tools.jackson.databind.JsonNode
import tools.jackson.databind.SerializationFeature
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.dokument.inntekt.InntektDokument
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.inntekt.InntektKonverter
import no.nav.melosys.integrasjon.inntekt.InntektResponse
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class InntektDtoFraInntektsDokumentTest {

    @Test
    fun `sjekk at mapping fra Saksopplysning InntektDokument til InntektDto blir som forventet`() {
        val inntektResponse = jacksonObjectMapper()
            
            .readValue<InntektResponse>(hentRessurs("mock/inntekt/inntektClientResponse.json"))

        val inntektDtoJson = InntektDto(
            InntektKonverter().lagSaksopplysning(inntektResponse).dokument as InntektDokument
        ).toJsonNode().toPrettyString()

        inntektDtoJson.trim().shouldBe(hentRessurs("mock/inntekt/inntektDtoMappingFraSaksopplysningResult.json").trim())
    }

    fun hentRessurs(fil: String): String = this::class.java.classLoader.getResource(fil)
        ?.readText(StandardCharsets.UTF_8) ?: throw IkkeFunnetException("Fant ikke $fil")

    private fun Any.toJsonNode(): JsonNode = jacksonObjectMapper()
        
        .valueToTree(this)
}
