package no.nav.melosys.service.avklartefakta

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.repository.AvklarteFaktaRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class AvklartManglendeInnbetalingServiceTest {

    @MockK(relaxed = true)
    private lateinit var avklarteFaktaRepository: AvklarteFaktaRepository

    @MockK(relaxed = true)
    private lateinit var behandlingsresultatRepository: BehandlingsresultatRepository

    @MockK(relaxed = true)
    private lateinit var avklartefaktaDtoKonverterer: AvklartefaktaDtoKonverterer

    private val slotAvklartefakta = slot<Avklartefakta>()

    private lateinit var avklartefaktaService: AvklartefaktaService
    private lateinit var avklartManglendeInnbetalingService: AvklartManglendeInnbetalingService

    @BeforeEach
    fun setUp() {
        avklartefaktaService = AvklartefaktaService(avklarteFaktaRepository, behandlingsresultatRepository, avklartefaktaDtoKonverterer)
        avklartManglendeInnbetalingService = AvklartManglendeInnbetalingService(avklartefaktaService)
    }

    @Test
    fun hentFullstendigMandlendeInnbetaling_avklartFaktaFinnesIkke_returnererNull() {
        avklartManglendeInnbetalingService.hentFullstendigManglendeInnbetaling(1L).shouldBeNull()
    }

    @Test
    fun lagreOgHent_manglerFullstendigInnbetaling_returnererTrue() {
        every { behandlingsresultatRepository.findById(1L) } returns Optional.of(Behandlingsresultat())
        every { avklarteFaktaRepository.save(capture(slotAvklartefakta)) } returnsArgument 0


        avklartManglendeInnbetalingService.hentFullstendigManglendeInnbetaling(1L).shouldBeNull()

        avklartManglendeInnbetalingService.lagreFullstendigManglendeInnbetalingSomAvklartFakta(1L, true)

        avklartManglendeInnbetalingService.hentFullstendigManglendeInnbetaling(1L)?.shouldBeTrue()
        slotAvklartefakta.captured.shouldNotBeNull().run {
            type.shouldBe(Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING)
            referanse.shouldBe(Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING.kode)
            subjekt.shouldBeNull()
            fakta.shouldBe(true.toString().uppercase())
        }
    }

    @Test
    fun lagreOgHent_manglerDelvisInnbetaling_returnererFalse() {
        every { behandlingsresultatRepository.findById(1L) } returns Optional.of(Behandlingsresultat())
        every { avklarteFaktaRepository.save(capture(slotAvklartefakta)) } returnsArgument 0


        avklartManglendeInnbetalingService.hentFullstendigManglendeInnbetaling(1L).shouldBeNull()

        avklartManglendeInnbetalingService.lagreFullstendigManglendeInnbetalingSomAvklartFakta(1L, false)

        avklartManglendeInnbetalingService.hentFullstendigManglendeInnbetaling(1L)?.shouldBeFalse()
        slotAvklartefakta.captured.shouldNotBeNull().run {
            type.shouldBe(Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING)
            referanse.shouldBe(Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING.kode)
            subjekt.shouldBeNull()
            fakta.shouldBe(false.toString().uppercase())
        }
    }
}
