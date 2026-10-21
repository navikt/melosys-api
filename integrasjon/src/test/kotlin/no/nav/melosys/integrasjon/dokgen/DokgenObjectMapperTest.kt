package no.nav.melosys.integrasjon.dokgen

import tools.jackson.databind.json.JsonMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import no.nav.melosys.domain.Behandlingsmaate
import no.nav.melosys.domain.avgift.Avgiftsberegningsregel
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.AvgiftPerAvgiftsdelDto
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.AvgiftssatsDto
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.OrdinærSatsPåfølgendeÅrDto
import no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl.ÅrsinntektDto
import com.fasterxml.jackson.annotation.JsonProperty
import org.junit.jupiter.api.Test

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

    @Test
    fun `årsavregningsår skal serialiseres med norsk feltnavn - reproduserer e2e-feilen`() {
        // Reproduserer feilen: "required key [årsavregningsår] not found"
        // Jackson 3 uten KotlinModule serialiserer non-ASCII Kotlin-feltnavn feil
        // med mindre @JsonProperty("årsavregningsår") er eksplisitt satt
        val dto = DtoMedÅrsavregningsår(årsavregningsår = 2023)

        val json = objectMapper.writeValueAsString(dto)

        json shouldContain """"årsavregningsår":2023"""
    }

    // Speiler mønsteret fra ÅrsavregningVedtaksbrev og InnhentingAvInntektsopplysninger
    @Test
    fun `dokgen-DTOer med norske feltnavn uten @JsonProperty skal serialiseres med riktige feltnavn`() {
        // Disse feltene finnes i ÅrsavregningVedtaksbrev, InnvilgelseFtrlPensjonistFrivillig m.fl.
        // uten @JsonProperty — verifiserer at Jackson 3 håndterer disse korrekt
        val dto = DtoMedNorskeFeltnavn(
            endeligTrygdeavgiftTotalbeløp = java.math.BigDecimal("6000"),
            differansebeløp = java.math.BigDecimal("1000"),
            eøsEllerTrygdeavtale = true,
            avslåttMedlemskapsIPensjonsdel = false,
            avslåttMedlemskapsperiodeFørMottaksdatoHelsedel = true
        )

        val json = objectMapper.writeValueAsString(dto)

        json shouldContain "endeligTrygdeavgiftTotalbeløp"
        json shouldContain "differansebeløp"
        json shouldContain "eøsEllerTrygdeavtale"
        json shouldContain "avslåttMedlemskapsIPensjonsdel"
        json shouldContain "avslåttMedlemskapsperiodeFørMottaksdatoHelsedel"
    }

    @Test
    fun `felt i DTOene for 25 prosent-regelen som starter med å skal serialiseres med riktige feltnavn`() {
        val avgiftssats = objectMapper.writeValueAsString(
            AvgiftssatsDto(2026, null, Avgiftsberegningsregel.TJUEFEM_PROSENT_REGEL, null, true, "FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON", null)
        )
        val avgiftPerAvgiftsdel = objectMapper.writeValueAsString(
            AvgiftPerAvgiftsdelDto(2026, null, Avgiftsberegningsregel.TJUEFEM_PROSENT_REGEL, java.math.BigDecimal.ONE)
        )
        val årsinntekt = objectMapper.writeValueAsString(ÅrsinntektDto(2026, java.math.BigDecimal.ONE))
        val ordinærSats = objectMapper.writeValueAsString(OrdinærSatsPåfølgendeÅrDto(2026, 2027, emptyList()))

        avgiftssats shouldContain """"år":2026"""
        avgiftPerAvgiftsdel shouldContain """"år":2026"""
        årsinntekt shouldContain """"år":2026"""
        årsinntekt shouldContain """"årsinntekt":1"""
        ordinærSats shouldContain """"år":2027"""
    }

    private data class DtoMedNorskeFeltnavn(
        val endeligTrygdeavgiftTotalbeløp: java.math.BigDecimal,
        val differansebeløp: java.math.BigDecimal,
        val eøsEllerTrygdeavtale: Boolean,
        val avslåttMedlemskapsIPensjonsdel: Boolean,
        val avslåttMedlemskapsperiodeFørMottaksdatoHelsedel: Boolean
    )

    private data class DtoMedÅrsavregningsår(
        @JsonProperty("årsavregningsår")
        val årsavregningsår: Int
    )

    private data class TestDto(val behandlingsmaate: Behandlingsmaate)
}
