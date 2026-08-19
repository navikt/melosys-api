package no.nav.melosys.saksflyt.statistikk

import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class RammeavtaleStatistikkServiceTest {

    @MockK
    private lateinit var rammeavtaleStatistikkRepository: RammeavtaleStatistikkRepository

    private lateinit var service: RammeavtaleStatistikkService

    @BeforeEach
    fun setup() {
        service = RammeavtaleStatistikkService(rammeavtaleStatistikkRepository)
    }

    @Test
    fun `summerer per vedtaksaar og bygger riktig prosesstype, data-monster og resultattype`() {
        val prosessTypeSlot = slot<String>()
        val dataLikePatternSlot = slot<String>()
        val resultatTypeSlot = slot<String>()
        every {
            rammeavtaleStatistikkRepository.tellFerdigbehandledePerVedtaksaarMedDataLike(
                capture(prosessTypeSlot),
                capture(dataLikePatternSlot),
                capture(resultatTypeSlot),
                any(),
                any(),
            )
        } returns listOf(
            arrayOf<Any>("2024", BigDecimal(3)),
            arrayOf<Any>("2025", BigDecimal(7)),
        )

        val statistikk = service.hentRammeavtaleFjernarbeidStatistikk(null, null)

        statistikk.antall shouldBe 10
        statistikk.antallPerVedtaksaar shouldBe linkedMapOf("2024" to 3L, "2025" to 7L)
        prosessTypeSlot.captured shouldBe ProsessType.ANMODNING_OM_UNNTAK.kode
        dataLikePatternSlot.captured shouldBe "%${ProsessDataKey.ER_FJERNARBEID_TWFA.kode}=true%"
        resultatTypeSlot.captured shouldBe Behandlingsresultattyper.FASTSATT_LOVVALGSLAND.name
    }

    @Test
    fun `hopper over rader uten vedtaksaar`() {
        every {
            rammeavtaleStatistikkRepository.tellFerdigbehandledePerVedtaksaarMedDataLike(any(), any(), any(), any(), any())
        } returns listOf(
            @Suppress("UNCHECKED_CAST")
            (arrayOf(null, BigDecimal(4)) as Array<Any>),
            arrayOf<Any>("2025", BigDecimal(2)),
        )

        val statistikk = service.hentRammeavtaleFjernarbeidStatistikk(null, null)

        statistikk.antallPerVedtaksaar shouldBe mapOf("2025" to 2L)
        statistikk.antall shouldBe 2
    }

    @Test
    fun `oversetter fom og inklusiv tom til tidspunkt`() {
        val fomSlot = slot<LocalDateTime?>()
        val tomSlot = slot<LocalDateTime?>()
        every {
            rammeavtaleStatistikkRepository.tellFerdigbehandledePerVedtaksaarMedDataLike(
                any(),
                any(),
                any(),
                captureNullable(fomSlot),
                captureNullable(tomSlot),
            )
        } returns emptyList()

        val statistikk = service.hentRammeavtaleFjernarbeidStatistikk(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 12, 31),
        )

        statistikk.antall shouldBe 0
        statistikk.antallPerVedtaksaar.shouldBeEmpty()
        fomSlot.captured shouldBe LocalDate.of(2024, 1, 1).atStartOfDay()
        // tom er inklusiv -> oversettes til starten av neste dag (eksklusiv øvre grense)
        tomSlot.captured shouldBe LocalDate.of(2025, 1, 1).atStartOfDay()
    }
}
