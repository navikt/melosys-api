package no.nav.melosys.saksflyt

import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.melosys.saksflytapi.domain.*
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
        prosessinstans = prosessinstansForTest { id(UUID.randomUUID()) }
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
    fun `har SED låsReferanse - oppdaterStatusOmSkalPåVent finnesProsessMedSammeReferanseUnderBehandling settesPåVent`() {
        prosessinstans.status = ProsessStatus.KLAR
        val låsReferanse = "12_12_1"
        prosessinstans.låsReferanse = låsReferanse

        val eksisterendeProsessinstans = lagProsessinstans(låsReferanse)
        every {
            prosessinstansRepository.findAllByIdNotAndStatusNotInAndLåsReferanseStartingWith(prosessinstans.id, any(), any())
        } returns setOf(eksisterendeProsessinstans.tilProsessinstansInfo())

        every { prosessinstansRepository.save(any()) } returns prosessinstans


        prosessinstansBehandlerDelegate.oppdaterStatusOmSkalPåVent(prosessinstans)


        verify { prosessinstansRepository.save(prosessinstans) }
        prosessinstans.status.shouldBe(ProsessStatus.PÅ_VENT)
    }

    @Test
    fun `Har OpprettManglendeInnbetalingBehandlingLåsReferanse låsReferanse og aktiv behandling med samme referanse skal da settes på vent`() {
        prosessinstans.status = ProsessStatus.KLAR
        val låsReferanse = "${LåsReferanseType.UBETALT}_ABC_123"
        prosessinstans.låsReferanse = låsReferanse

        val eksisterendeProsessinstans = lagProsessinstans(låsReferanse)
        every {
            prosessinstansRepository.findAllByIdNotAndStatusNotInAndLåsReferanseStartingWith(prosessinstans.id, any(), any())
        } returns setOf(eksisterendeProsessinstans.tilProsessinstansInfo())
        every { prosessinstansRepository.save(any()) } returns prosessinstans


        prosessinstansBehandlerDelegate.oppdaterStatusOmSkalPåVent(prosessinstans)


        verify { prosessinstansRepository.save(prosessinstans) }
        prosessinstans.status.shouldBe(ProsessStatus.PÅ_VENT)
    }

    @Test
    fun oppdaterStatusOmSkalPåVent_finnesProsessMedSammeReferanseUlikId_settesPåVent() {
        prosessinstans.status = ProsessStatus.KLAR
        val låsReferanse = "12_12_1"
        prosessinstans.låsReferanse = låsReferanse

        val eksisterendeProsessinstans = lagProsessinstans("12_13_1")
        every {
            prosessinstansRepository.findAllByIdNotAndStatusNotInAndLåsReferanseStartingWith(prosessinstans.id, any(), any())
        } returns setOf(eksisterendeProsessinstans.tilProsessinstansInfo())
        every { prosessinstansRepository.save(any()) } returns prosessinstans


        prosessinstansBehandlerDelegate.oppdaterStatusOmSkalPåVent(prosessinstans)


        verify { prosessinstansRepository.save(prosessinstans) }
        prosessinstans.status.shouldBe(ProsessStatus.PÅ_VENT)
    }

    private fun lagProsessinstans(låsReferanse: String): Prosessinstans {
        return prosessinstansForTest {
            id(UUID.randomUUID())
            låsReferanse(låsReferanse)
            status(ProsessStatus.UNDER_BEHANDLING)
            registrertDato(LocalDateTime.now())
        }
    }

    private fun Prosessinstans.tilProsessinstansInfo() =
        ProsessinstansInfo(id!!, status!!, registrertDato!!, hentLåsReferanse)

}
