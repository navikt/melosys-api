package no.nav.melosys.saksflyt

import io.kotest.matchers.shouldBe
import io.mockk.*
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ProsessinstansFerdigListenerTest {
    private lateinit var prosessinstansRepository: ProsessinstansRepository
    private lateinit var prosessinstansBehandler: ProsessinstansBehandler
    private lateinit var prosessinstansFerdigListener: ProsessinstansFerdigListener

    @BeforeEach
    fun setup() {
        prosessinstansRepository = mockk<ProsessinstansRepository>()
        prosessinstansBehandler = mockk<ProsessinstansBehandler>()
        prosessinstansFerdigListener = ProsessinstansFerdigListener(prosessinstansRepository, prosessinstansBehandler)
    }

    @Test
    fun prosessinstansFerdig_harIngenLås_gjørIngenting() {
        val ferdigProsessinstans: Prosessinstans = lagProsessInstans()


        prosessinstansFerdigListener.prosessinstansFerdig(ProsessinstansFerdigEvent(ferdigProsessinstans))


        verify {
            prosessinstansRepository wasNot Called
            prosessinstansBehandler wasNot Called
        }
    }

    @Test
    fun prosesssinstansFerdig_harLåsFinnesAktiveReferanser_gjørIngenting() {
        val ferdigProsessinstans = lagProsessInstans { låsReferanse = "12_12_1" }
        every { prosessinstansRepository.existsByStatusNotInAndLåsReferanse(any(), any()) } returns true


        prosessinstansFerdigListener.prosessinstansFerdig(ProsessinstansFerdigEvent(ferdigProsessinstans))


        verify(exactly = 1) { prosessinstansRepository.existsByStatusNotInAndLåsReferanse(any(), any()) }
    }

    @Test
    fun prosessinstansFerdig_harLåsIngenAktiveReferanser_starterTidligstOpprettetProsessinstans() {
        val ferdigProsessinstans = lagProsessInstans { låsReferanse = "12_12_1" }

        val prosessinstansUlikReferanse = lagProsessinstans(LocalDateTime.now().minusDays(2), "13_12_1")
        val tidligstOpprettetProsessinstans = lagProsessinstans(LocalDateTime.now().minusDays(1), "12_13_1")
        val senestOpprettetProsessinstans = lagProsessinstans(LocalDateTime.now(), "12_14_1")

        every { prosessinstansBehandler.behandleProsessinstans(any()) } returns mockk()
        every { prosessinstansRepository.save(any()) } returns mockk()
        every { prosessinstansRepository.existsByStatusNotInAndLåsReferanse(any(), any()) } returns false
        every { prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT) } returns setOf(
            prosessinstansUlikReferanse,
            tidligstOpprettetProsessinstans,
            senestOpprettetProsessinstans
        )


        prosessinstansFerdigListener.prosessinstansFerdig(ProsessinstansFerdigEvent(ferdigProsessinstans))


        verify { prosessinstansBehandler.behandleProsessinstans(tidligstOpprettetProsessinstans) }
        tidligstOpprettetProsessinstans.status.shouldBe(ProsessStatus.KLAR)
    }

    private fun lagProsessinstans(registrertDato: LocalDateTime, referanse: String): Prosessinstans {
        return Prosessinstans().apply {
            status = ProsessStatus.PÅ_VENT
            låsReferanse = referanse
            this.registrertDato = registrertDato
        }
    }

    private fun lagProsessInstans(block: Prosessinstans.() -> Unit = {}): Prosessinstans = Prosessinstans().apply {
        id = UUID.randomUUID()
        block()
    }

}
