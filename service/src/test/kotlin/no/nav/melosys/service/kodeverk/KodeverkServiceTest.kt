package no.nav.melosys.service.kodeverk

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.integrasjon.kodeverk.Kode
import no.nav.melosys.integrasjon.kodeverk.Kodeverk
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
internal class KodeverkServiceTest {
    private val landkoder: MutableMap<String, List<Kode>> = HashMap()
    private lateinit var kodeverkService: KodeverkService

    private val kodeverkRegisterMock: KodeverkRegister = mockk()

    @BeforeEach
    fun setup() {
        kodeverkService = KodeverkService(kodeverkRegisterMock)
        landkoder[BAK] = listOf(
            Kode(
                BAK,
                BAKVENDTLAND,
                LocalDate.MIN,
                LocalDate.MAX
            )
        )
        every { kodeverkRegisterMock.hentKodeverk(FellesKodeverk.LANDKODER.navn) } returns Kodeverk(FellesKodeverk.LANDKODER.navn, landkoder)
    }

    @Test
    fun `dekoder kode BAK til BAKVENDTLAND`() {
        val res = kodeverkService.dekod(FellesKodeverk.LANDKODER, BAK)


        res shouldBe BAKVENDTLAND
    }

    @Test
    fun `skal hente alle gyldige koder for kodeverk`() {
        val idag = LocalDate.now()
        landkoder[OPP] = listOf(
            Kode(
                OPP,
                BAKVENDTLAND,
                LocalDate.MIN,
                idag.minusDays(1)
            ), Kode(OPP, OPPNEDLAND, idag, LocalDate.MAX)
        )


        val res = kodeverkService.hentGyldigeKoderForKodeverk(FellesKodeverk.LANDKODER)


        res shouldHaveSize 2
        res[0].kode shouldBe BAK
        res[0].navn shouldBe BAKVENDTLAND
        res[1].kode shouldBe OPP
        res[1].navn shouldBe OPPNEDLAND
    }

    @Test
    fun `skal kun hente koder for kodeverk som er gyldige`() {
        val idag = LocalDate.now()
        landkoder[OPP] = listOf(
            Kode(OPP, BAKVENDTLAND, LocalDate.MIN, idag.minusDays(1)),
            Kode(OPP, OPPNEDLAND, idag.plusDays(1), LocalDate.MAX)
        )


        val res = kodeverkService.hentGyldigeKoderForKodeverk(FellesKodeverk.LANDKODER)


        res shouldHaveSize 1
        res.single().kode shouldBe BAK
    }

    companion object {
        private const val BAK = "BAK"
        private const val BAKVENDTLAND = "BAKVENDTLAND"
        private const val OPP = "OPP"
        private const val OPPNEDLAND = "OPPNEDLAND"
    }
}
