package no.nav.melosys.tjenester.gui.config.jackson.serialize

import com.fasterxml.jackson.annotation.JsonInclude
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode
import no.nav.melosys.domain.dokument.medlemskap.Periode
import no.nav.melosys.service.kodeverk.KodeverkService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class MedlemsperiodeSerializerTest {

    private lateinit var mapper: ObjectMapper
    private val kodeverkService = mockk<KodeverkService>()

    @BeforeEach
    fun setUp() {
        mapper = JsonMapper.builder()
            .addModule(SimpleModule().apply {
                addSerializer(MedlemsperiodeSerializer(kodeverkService))
            })
            .build()
    }

    @Test
    fun `skal serialisere alle felter med kode og term`() {
        every { kodeverkService.dekod(FellesKodeverk.PERIODETYPE_MEDL, "PERIODE") } returns "Periode"
        every { kodeverkService.dekod(FellesKodeverk.LANDKODER, "NOR") } returns "Norge"
        every { kodeverkService.dekod(FellesKodeverk.GRUNNLAG_MEDL, "ARBEID") } returns "Arbeid"
        every { kodeverkService.dekod(FellesKodeverk.KILDEDOKUMENT_MEDL, "SOKNAD") } returns "Søknad"
        every { kodeverkService.dekod(FellesKodeverk.LOVVALG_MEDL, "NASJONAL") } returns "Nasjonal"
        every { kodeverkService.dekod(FellesKodeverk.PERIODESTATUS_MEDL, "GYLD") } returns "Gyldig"
        every { kodeverkService.dekod(FellesKodeverk.DEKNING_MEDL, "FT") } returns "Folketrygd"

        val periode = Medlemsperiode(
            id = 42L,
            type = "PERIODE",
            land = "NOR",
            grunnlagstype = "ARBEID",
            kildedokumenttype = "SOKNAD",
            kilde = "NAV",
            lovvalg = "NASJONAL",
            status = "GYLD",
            trygdedekning = "FT",
        ).medPeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))

        val tree = mapper.readTree(mapper.writeValueAsString(periode))

        tree.get("periodeID").asLong() shouldBe 42L
        tree.get("periodetype").get("kode").asText() shouldBe "PERIODE"
        tree.get("periodetype").get("term").asText() shouldBe "Periode"
        tree.get("land").get("kode").asText() shouldBe "NOR"
        tree.get("land").get("term").asText() shouldBe "Norge"
        tree.get("grunnlagstype").get("kode").asText() shouldBe "ARBEID"
        tree.get("grunnlagstype").get("term").asText() shouldBe "Arbeid"
        tree.get("kildedokumenttype").get("kode").asText() shouldBe "SOKNAD"
        tree.get("kildedokumenttype").get("term").asText() shouldBe "Søknad"
        tree.get("lovvalg").get("kode").asText() shouldBe "NASJONAL"
        tree.get("lovvalg").get("term").asText() shouldBe "Nasjonal"
        tree.get("status").get("kode").asText() shouldBe "GYLD"
        tree.get("status").get("term").asText() shouldBe "Gyldig"
        tree.get("trygdedekning").get("kode").asText() shouldBe "FT"
        tree.get("trygdedekning").get("term").asText() shouldBe "Folketrygd"
        tree.get("periode").get("fom").asText() shouldBe "2024-01-01"
        tree.get("periode").get("tom").asText() shouldBe "2024-12-31"
    }

    @Test
    fun `skal serialisere null-felter som null`() {
        val periode = Medlemsperiode(id = 1L)

        val tree = mapper.readTree(mapper.writeValueAsString(periode))

        tree.get("periodetype").isNull shouldBe true
        tree.get("land").isNull shouldBe true
        tree.get("grunnlagstype").isNull shouldBe true
        tree.get("periode").isNull shouldBe true
        // kilde settes alltid som KodeDto(kilde, kilde) – aldri JSON null
        tree.get("kilde").get("kode").isNull shouldBe true
        tree.get("kilde").get("term").isNull shouldBe true
    }

    @Test
    fun `kilde skal serialiseres med kode lik term`() {
        val periode = Medlemsperiode(id = 1L, kilde = "LAANEKASSEN")

        val tree = mapper.readTree(mapper.writeValueAsString(periode))

        tree.get("kilde").get("kode").asText() shouldBe "LAANEKASSEN"
        tree.get("kilde").get("term").asText() shouldBe "LAANEKASSEN"
    }

    private fun Medlemsperiode.medPeriode(fom: LocalDate?, tom: LocalDate?): Medlemsperiode {
        javaClass.getDeclaredField("periode").apply { isAccessible = true }.set(this, Periode(fom, tom))
        return this
    }
}
