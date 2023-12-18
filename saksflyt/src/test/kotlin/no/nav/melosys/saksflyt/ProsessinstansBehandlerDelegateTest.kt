package no.nav.melosys.saksflyt

import io.kotest.matchers.shouldBe
import io.mockk.*
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.ProsessinstansInfo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ProsessinstansBehandlerDelegateTest {
    private lateinit var prosessinstansRepository: ProsessinstansRepository
    private lateinit var prosessinstansBehandler: ProsessinstansBehandler
    private lateinit var prosessinstansBehandlerDelegate: ProsessinstansBehandlerDelegate
    private lateinit var prosessinstans: Prosessinstans

    @BeforeEach
    fun setup() {
        prosessinstansRepository = mockk<ProsessinstansRepository>()
        prosessinstansBehandler = mockk<ProsessinstansBehandler>()
        prosessinstansBehandlerDelegate = ProsessinstansBehandlerDelegate(prosessinstansBehandler, prosessinstansRepository)
        prosessinstans = Prosessinstans().apply { id = UUID.randomUUID() }
    }

    @Test
    fun oppdaterStatusOmSkalPåVent_harIkkeLås_settesIkkePåVent() {
        prosessinstans.status = ProsessStatus.KLAR
        every { prosessinstansRepository.findAllByIdNotAndStatusNotInAndLåsReferanseStartingWith(any(), any(), any()) } returns emptySet()


        prosessinstansBehandlerDelegate.oppdaterStatusOmSkalPåVent(prosessinstans)


        verify {
            prosessinstansRepository wasNot Called
            prosessinstansBehandler wasNot Called
        }
        prosessinstans.status.shouldBe(ProsessStatus.KLAR)
    }

    @Test
    fun oppdaterStatusOmSkalPåVent_finnesProsessMedSammeReferanseUnderBehandling_settesIkkePåVent() {
        prosessinstans.status = ProsessStatus.KLAR
        val låsReferanse = "12_12_1"
        prosessinstans.låsReferanse = låsReferanse

        val eksisterendeProsessinstans = lagProsessinstans(låsReferanse)
        every {
            prosessinstansRepository.findAllByIdNotAndStatusNotInAndLåsReferanseStartingWith(prosessinstans.id, any(), any())
        } returns setOf(ProsessinstansInfo(eksisterendeProsessinstans))


        prosessinstansBehandlerDelegate.oppdaterStatusOmSkalPåVent(prosessinstans)


        prosessinstans.status.shouldBe(ProsessStatus.KLAR)
    }

    @Test
    fun oppdaterStatusOmSkalPåVent_finnesProsessMedSammeReferanseUlikId_settesPåVent() {
        prosessinstans.status = ProsessStatus.KLAR
        val låsReferanse = "12_12_1"
        prosessinstans.låsReferanse = låsReferanse

        val eksisterendeProsessinstans = lagProsessinstans("12_13_1")
        every {
            prosessinstansRepository.findAllByIdNotAndStatusNotInAndLåsReferanseStartingWith(prosessinstans.id, any(), any())
        } returns setOf(ProsessinstansInfo(eksisterendeProsessinstans))
        every { prosessinstansRepository.save(any()) } returns prosessinstans


        prosessinstansBehandlerDelegate.oppdaterStatusOmSkalPåVent(prosessinstans)


        verify { prosessinstansRepository.save(prosessinstans) }
        prosessinstans.status.shouldBe(ProsessStatus.PÅ_VENT)
    }

    private fun lagProsessinstans(låsReferanse: String): Prosessinstans {
        return Prosessinstans().apply {
            id = UUID.randomUUID()
            this.låsReferanse = låsReferanse
            status = ProsessStatus.UNDER_BEHANDLING
            registrertDato = LocalDateTime.now()
            registrertDato = LocalDateTime.now()
        }
    }
}
