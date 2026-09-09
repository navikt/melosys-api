package no.nav.melosys.service.avklartefakta

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avklartefakta.Avklartefakta
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper
import no.nav.melosys.domain.kodeverk.ManglendeInnbetalingHandlingsvalg
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.AvklarteFaktaRepository
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.behandling.ReplikerBehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class ManglendeInnbetalingHandlingsvalgServiceTest {

    @MockK(relaxed = true)
    private lateinit var avklarteFaktaRepository: AvklarteFaktaRepository

    @MockK(relaxed = true)
    private lateinit var behandlingsresultatRepository: BehandlingsresultatRepository

    @MockK(relaxed = true)
    private lateinit var avklartefaktaDtoKonverterer: AvklartefaktaDtoKonverterer

    @MockK(relaxed = true)
    private lateinit var replikerBehandlingsresultatService: ReplikerBehandlingsresultatService

    @MockK(relaxed = true)
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    private val slotAvklartefakta = slot<Avklartefakta>()

    private lateinit var avklartefaktaService: AvklartefaktaService
    private lateinit var manglendeInnbetalingHandlingsvalgService: ManglendeInnbetalingHandlingsvalgService

    @BeforeEach
    fun setUp() {
        avklartefaktaService = AvklartefaktaService(avklarteFaktaRepository, behandlingsresultatRepository, avklartefaktaDtoKonverterer)
        manglendeInnbetalingHandlingsvalgService = ManglendeInnbetalingHandlingsvalgService(
            avklartefaktaService, replikerBehandlingsresultatService, behandlingsresultatService
        )
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns Behandlingsresultat().apply {
            behandling = mockk<Behandling>(relaxed = true) {
                every { erManglendeInnbetalingTrygdeavgift() } returns true
            }
        }
    }

    @Test
    fun hentFullstendigMandlendeInnbetaling_avklartFaktaFinnesIkke_returnererNull() {
        manglendeInnbetalingHandlingsvalgService.hentFullstendigManglendeInnbetaling(1L).shouldBeNull()
    }

    @Test
    fun lagreOgHent_manglerFullstendigInnbetaling_returnererTrue() {
        every { behandlingsresultatRepository.findById(1L) } returns Optional.of(Behandlingsresultat())
        every { avklarteFaktaRepository.save(capture(slotAvklartefakta)) } returnsArgument 0


        manglendeInnbetalingHandlingsvalgService.hentFullstendigManglendeInnbetaling(1L).shouldBeNull()

        manglendeInnbetalingHandlingsvalgService.lagreFullstendigManglendeInnbetalingSomAvklartFakta(1L, true)

        manglendeInnbetalingHandlingsvalgService.hentFullstendigManglendeInnbetaling(1L)?.shouldBeTrue()
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


        manglendeInnbetalingHandlingsvalgService.hentFullstendigManglendeInnbetaling(1L).shouldBeNull()

        manglendeInnbetalingHandlingsvalgService.lagreFullstendigManglendeInnbetalingSomAvklartFakta(1L, false)

        manglendeInnbetalingHandlingsvalgService.hentFullstendigManglendeInnbetaling(1L)?.shouldBeFalse()
        slotAvklartefakta.captured.shouldNotBeNull().run {
            type.shouldBe(Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING)
            referanse.shouldBe(Avklartefaktatyper.FULLSTENDIG_MANGLENDE_INNBETALING.kode)
            subjekt.shouldBeNull()
            fakta.shouldBe(false.toString().uppercase())
        }
    }

    @Test
    fun hentManglendeInnbetalingHandlingsvalg_avklartFaktaFinnesIkke_returnererNull() {
        manglendeInnbetalingHandlingsvalgService.hentManglendeInnbetalingHandlingsvalg(1L).shouldBeNull()
    }

    @Test
    fun lagreOgHent_manglendeInnbetalingHandlingsvalg_returnererLagretVerdi() {
        val behandling = mockk<Behandling>(relaxed = true)
        val behandlingsresultat = Behandlingsresultat().apply { this.behandling = behandling }
        every { behandlingsresultatRepository.findById(1L) } returns Optional.of(behandlingsresultat)
        every { avklarteFaktaRepository.save(capture(slotAvklartefakta)) } returnsArgument 0

        manglendeInnbetalingHandlingsvalgService.hentManglendeInnbetalingHandlingsvalg(1L).shouldBeNull()

        manglendeInnbetalingHandlingsvalgService.lagreManglendeInnbetalingHandlingsvalgSomAvklartFakta(
            1L, ManglendeInnbetalingHandlingsvalg.DELER_AV_PERIODEN_OPPHØRES
        )
        every { avklarteFaktaRepository.findByBehandlingsresultatId(1L) } returns setOf(slotAvklartefakta.captured)

        manglendeInnbetalingHandlingsvalgService.hentManglendeInnbetalingHandlingsvalg(1L)
            .shouldBe(ManglendeInnbetalingHandlingsvalg.DELER_AV_PERIODEN_OPPHØRES)
        slotAvklartefakta.captured.shouldNotBeNull().run {
            type.shouldBe(Avklartefaktatyper.MANGLENDE_INNBETALING_HANDLINGSVALG)
            referanse.shouldBe(Avklartefaktatyper.MANGLENDE_INNBETALING_HANDLINGSVALG.kode)
            subjekt.shouldBeNull()
            fakta.shouldBe(ManglendeInnbetalingHandlingsvalg.DELER_AV_PERIODEN_OPPHØRES.kode)
        }
    }

    @Test
    fun lagreManglendeInnbetalingHandlingsvalgSomAvklartFakta_tilbakestillerBehandlingsresultat() {
        every { behandlingsresultatRepository.findById(1L) } returns Optional.of(Behandlingsresultat())
        every { avklarteFaktaRepository.save(any()) } returnsArgument 0

        manglendeInnbetalingHandlingsvalgService.lagreManglendeInnbetalingHandlingsvalgSomAvklartFakta(
            1L, ManglendeInnbetalingHandlingsvalg.HELE_PERIODEN_OPPHØRES
        )

        verify(exactly = 1) { replikerBehandlingsresultatService.tilbakestillBehandlingsresultat(1L) }
    }

    @Test
    fun lagreManglendeInnbetalingHandlingsvalgSomAvklartFakta_uendretVerdi_tidligReturUtenTilbakestilling() {
        val eksisterendeAvklartefakta = Avklartefakta().apply {
            type = Avklartefaktatyper.MANGLENDE_INNBETALING_HANDLINGSVALG
            referanse = Avklartefaktatyper.MANGLENDE_INNBETALING_HANDLINGSVALG.kode
            fakta = ManglendeInnbetalingHandlingsvalg.HELE_PERIODEN_OPPHØRES.kode
        }
        every { avklarteFaktaRepository.findByBehandlingsresultatId(1L) } returns setOf(eksisterendeAvklartefakta)

        manglendeInnbetalingHandlingsvalgService.lagreManglendeInnbetalingHandlingsvalgSomAvklartFakta(
            1L, ManglendeInnbetalingHandlingsvalg.HELE_PERIODEN_OPPHØRES
        )

        verify(exactly = 0) { replikerBehandlingsresultatService.tilbakestillBehandlingsresultat(any()) }
        verify(exactly = 0) { avklarteFaktaRepository.save(any()) }
    }

    @Test
    fun lagreManglendeInnbetalingHandlingsvalgSomAvklartFakta_feilBehandlingstype_kasterFunksjonellException() {
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns Behandlingsresultat().apply {
            behandling = mockk<Behandling>(relaxed = true) {
                every { erManglendeInnbetalingTrygdeavgift() } returns false
            }
        }

        shouldThrow<FunksjonellException> {
            manglendeInnbetalingHandlingsvalgService.lagreManglendeInnbetalingHandlingsvalgSomAvklartFakta(
                1L, ManglendeInnbetalingHandlingsvalg.HELE_PERIODEN_OPPHØRES
            )
        }

        verify(exactly = 0) { replikerBehandlingsresultatService.tilbakestillBehandlingsresultat(any()) }
        verify(exactly = 0) { avklarteFaktaRepository.save(any()) }
    }
}
