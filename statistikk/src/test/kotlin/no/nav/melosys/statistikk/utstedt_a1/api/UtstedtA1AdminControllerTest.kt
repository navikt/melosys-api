package no.nav.melosys.statistikk.utstedt_a1.api

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.*
import io.mockk.junit5.MockKExtension
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.repository.VedtakMetadataRepository
import no.nav.melosys.statistikk.utstedt_a1.service.UtstedtA1Service
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class UtstedtA1AdminControllerTest {

    private val utstedtA1Service = mockk<UtstedtA1Service>()
    private val vedtakMetadataRepository = mockk<VedtakMetadataRepository>()

    private lateinit var utstedtA1AdminTjeneste: UtstedtA1AdminController

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        utstedtA1AdminTjeneste = UtstedtA1AdminController(utstedtA1Service, vedtakMetadataRepository)
    }

    @Test
    fun `publiser melding skal kalle service med korrekt behandling-id`() {
        justRun { utstedtA1Service.sendMeldingOmUtstedtA1(1L) }

        utstedtA1AdminTjeneste.publiserMelding(1L)

        verify { utstedtA1Service.sendMeldingOmUtstedtA1(1L) }
    }

    @Test
    fun `publiserEksisterendeBehandlinger forventListe`() {
        every { vedtakMetadataRepository.findBehandlingsresultatIdByRegistrertDatoIsGreaterThanEqual(any<Instant>()) } returns listOf(1L, 2L, 3L)
        justRun { utstedtA1Service.sendMeldingOmUtstedtA1(any()) }

        val behandlinger = utstedtA1AdminTjeneste.publiserEksisterendeBehandlinger(LocalDate.now()).body
        behandlinger.shouldNotBeNull()
        behandlinger["feiledeBehandlinger"].shouldBeEmpty()
        behandlinger["sendteBehandlinger"].shouldNotBeNull()
            .shouldContainExactlyInAnyOrder(1L, 2L, 3L)
    }

    @Test
    fun `publiserEksisterendeBehandlinger medOppgitteBehandlingerOgBehandlingFeiler forventListe`() {
        every { vedtakMetadataRepository.findBehandlingsresultatIdByRegistrertDatoIsGreaterThanEqual(any<Instant>()) } returns listOf(1L, 2L, 3L)
        justRun { utstedtA1Service.sendMeldingOmUtstedtA1(1L) }
        justRun { utstedtA1Service.sendMeldingOmUtstedtA1(2L) }
        every { utstedtA1Service.sendMeldingOmUtstedtA1(3L) } throws TekniskException("ugyldig behandling")

        val behandlinger = utstedtA1AdminTjeneste.publiserEksisterendeBehandlinger(LocalDate.now()).body
        behandlinger.shouldNotBeNull()
        behandlinger["feiledeBehandlinger"].shouldNotBeNull()
            .shouldContainExactly(3L)
        behandlinger["sendteBehandlinger"].shouldNotBeNull()
            .shouldContainExactlyInAnyOrder(1L, 2L)
    }
}
