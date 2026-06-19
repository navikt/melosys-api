package no.nav.melosys.saksflyt.statistikk

import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
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
    fun `summerer per aar og bygger riktig prosesstype og data-monster`() {
        val prosessTypeSlot = slot<String>()
        val monsterSlot = slot<String>()
        every {
            rammeavtaleStatistikkRepository.tellPerAarMedDataLike(capture(prosessTypeSlot), capture(monsterSlot), any(), any())
        } returns listOf(
            arrayOf<Any>("2024", BigDecimal(3)),
            arrayOf<Any>("2025", BigDecimal(7)),
        )


        val statistikk = service.hentRammeavtaleFjernarbeidStatistikk(null, null)


        statistikk.antall shouldBe 10
        statistikk.antallPerAar shouldBe linkedMapOf("2024" to 3L, "2025" to 7L)
        prosessTypeSlot.captured shouldBe ProsessType.ANMODNING_OM_UNNTAK.kode
        monsterSlot.captured shouldBe "%${ProsessDataKey.ER_FJERNARBEID_TWFA.kode}=true%"
    }

    @Test
    fun `oversetter fom og inklusiv tom til tidspunkt`() {
        val fomSlot = slot<LocalDateTime?>()
        val tomSlot = slot<LocalDateTime?>()
        every {
            rammeavtaleStatistikkRepository.tellPerAarMedDataLike(any(), any(), captureNullable(fomSlot), captureNullable(tomSlot))
        } returns emptyList()


        val statistikk = service.hentRammeavtaleFjernarbeidStatistikk(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 12, 31),
        )


        statistikk.antall shouldBe 0
        statistikk.antallPerAar.shouldBeEmpty()
        fomSlot.captured shouldBe LocalDate.of(2024, 1, 1).atStartOfDay()
        // tom er inklusiv -> oversettes til starten av neste dag (eksklusiv øvre grense)
        tomSlot.captured shouldBe LocalDate.of(2025, 1, 1).atStartOfDay()
    }
}
