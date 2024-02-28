package no.nav.melosys.saksflyt

import io.kotest.matchers.shouldBe
import io.mockk.*
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
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
    fun prosesssinstansFerdig_harLåsFinnesIngenPåVent_gjørIngenting() {
        val ferdigProsessinstans = lagProsessInstans { låsReferanse = "12_12_1" }
        every { prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT) } returns emptySet()


        prosessinstansFerdigListener.prosessinstansFerdig(ProsessinstansFerdigEvent(ferdigProsessinstans))


        verify {
            prosessinstansBehandler wasNot Called
        }
    }

    @Test
    fun prosessinstansFerdig_harLåsIngenAktiveReferanser_starterTidligstOpprettetProsessinstans() {
        val ferdigProsessinstans = lagProsessInstans { låsReferanse = "12_12_1" }

        val prosessinstansUlikReferanse = lagProsessInstans {
            låsReferanse = "13_12_1"
            registrertDato = LocalDateTime.now().minusDays(2)
        }
        val tidligstOpprettetProsessinstans = lagProsessInstans {
            låsReferanse = "12_13_1"
            registrertDato = LocalDateTime.now().minusDays(1)
        }
        val senestOpprettetProsessinstans = lagProsessInstans {
            låsReferanse = "12_14_1"
            registrertDato = LocalDateTime.now()
        }
        every { prosessinstansBehandler.behandleProsessinstans(any()) } returns Unit
        every { prosessinstansRepository.save(any()) } returns mockk()
        every { prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT) } returns setOf(
            prosessinstansUlikReferanse,
            tidligstOpprettetProsessinstans,
            senestOpprettetProsessinstans
        )


        prosessinstansFerdigListener.prosessinstansFerdig(ProsessinstansFerdigEvent(ferdigProsessinstans))


        verify { prosessinstansBehandler.behandleProsessinstans(tidligstOpprettetProsessinstans) }
        tidligstOpprettetProsessinstans.status.shouldBe(ProsessStatus.KLAR)
        confirmVerified(prosessinstansBehandler)
    }

    @Test
    fun `start eldste sub-prosesser først`() {
        val lås1 = "12_13_1"
        val lås2 = "12_14_1"

        val rootProsessinstans = lagProsessInstans {
            låsReferanse = lås1
        }
        val tidligstOpprettetProsessinstans = lagProsessInstans {
            låsReferanse = lås2
            registrertDato = LocalDateTime.now().minusDays(2)
        }
        val subProsessinstansEldst = lagProsessInstans {
            låsReferanse = lås1
            setData(ProsessDataKey.PROCESS_PARENT_ID, rootProsessinstans.id)
            registrertDato = LocalDateTime.now().minusDays(1)
        }

        val subProsessinstansNy = lagProsessInstans {
            låsReferanse = lås1
            setData(ProsessDataKey.PROCESS_PARENT_ID, rootProsessinstans.id)
            registrertDato = LocalDateTime.now()
        }

        every { prosessinstansRepository.save(any()) } returns mockk()
        every { prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT) } returns setOf(
            tidligstOpprettetProsessinstans,
            subProsessinstansEldst,
            subProsessinstansNy

        )
        every { prosessinstansBehandler.behandleProsessinstans(any()) } returns Unit


        prosessinstansFerdigListener.prosessinstansFerdig(ProsessinstansFerdigEvent(rootProsessinstans))


        verify(exactly = 1) { prosessinstansBehandler.behandleProsessinstans(subProsessinstansEldst) }
        confirmVerified(prosessinstansBehandler)
    }

    @Test
    fun `start sub-prosesser før root-prosessers`() {
        val lås1 = "12_13_1"
        val lås2 = "12_14_1"

        val rootProsessinstans1 = lagProsessInstans {
            låsReferanse = lås1
        }
        val tidligstOpprettetProsessinstans = lagProsessInstans {
            låsReferanse = lås2
            registrertDato = LocalDateTime.now().minusDays(2)
        }
        val subProsessinstansEldst = lagProsessInstans {
            låsReferanse = lås1
            setData(ProsessDataKey.PROCESS_PARENT_ID, rootProsessinstans1.id)
            registrertDato = LocalDateTime.now().minusDays(1)
        }
        val subProsessinstansNy = lagProsessInstans {
            låsReferanse = lås1
            setData(ProsessDataKey.PROCESS_PARENT_ID, rootProsessinstans1.id)
            registrertDato = LocalDateTime.now()
        }

        every { prosessinstansRepository.save(any()) } returns mockk()
        every { prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT) } returns setOf(
            tidligstOpprettetProsessinstans,
            subProsessinstansNy,
        )
        every { prosessinstansBehandler.behandleProsessinstans(any()) } returns Unit


        prosessinstansFerdigListener.prosessinstansFerdig(ProsessinstansFerdigEvent(subProsessinstansEldst))


        verify(exactly = 1) { prosessinstansBehandler.behandleProsessinstans(subProsessinstansNy) }
        confirmVerified(prosessinstansBehandler)
    }


    @Test
    fun `start eldste sub-prosesser først når duplikat`() {
        val lås = "12_13_1"

        val rootProsessinstans = lagProsessInstans {
            låsReferanse = lås
        }
        val tidligstOpprettetProsessinstans = lagProsessInstans {
            låsReferanse = lås
            registrertDato = LocalDateTime.now().minusDays(2)
        }
        val subProsessinstansEldst = lagProsessInstans {
            låsReferanse = lås
            setData(ProsessDataKey.PROCESS_PARENT_ID, rootProsessinstans.id)
            registrertDato = LocalDateTime.now().minusDays(1)
        }

        val subProsessinstansNy = lagProsessInstans {
            låsReferanse = lås
            setData(ProsessDataKey.PROCESS_PARENT_ID, rootProsessinstans.id)
            registrertDato = LocalDateTime.now()
        }

        every { prosessinstansRepository.save(any()) } returns mockk()
        every { prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT) } returns setOf(
            tidligstOpprettetProsessinstans,
            subProsessinstansEldst,
            subProsessinstansNy

        )
        every { prosessinstansBehandler.behandleProsessinstans(any()) } returns Unit


        prosessinstansFerdigListener.prosessinstansFerdig(ProsessinstansFerdigEvent(rootProsessinstans))


        verify(exactly = 1) { prosessinstansBehandler.behandleProsessinstans(subProsessinstansEldst) }
        confirmVerified(prosessinstansBehandler)
    }

    @Test
    fun `start eldste prosesser først`() {
        val lås = "12_13_1"

        val rootProsessinstans = lagProsessInstans {
            låsReferanse = lås
        }
        val tidligstOpprettetProsessinstans = lagProsessInstans {
            låsReferanse = lås
            registrertDato = LocalDateTime.now().minusDays(2)
        }
        val nyesteOpprettetProsessinstans = lagProsessInstans {
            låsReferanse = lås
            registrertDato = LocalDateTime.now()
        }
        every { prosessinstansRepository.save(any()) } returns mockk()
        every { prosessinstansRepository.findAllByStatus(ProsessStatus.PÅ_VENT) } returns setOf(
            tidligstOpprettetProsessinstans,
            nyesteOpprettetProsessinstans,
        )
        every { prosessinstansBehandler.behandleProsessinstans(any()) } returns Unit


        prosessinstansFerdigListener.prosessinstansFerdig(ProsessinstansFerdigEvent(rootProsessinstans))


        verify(exactly = 1) { prosessinstansBehandler.behandleProsessinstans(tidligstOpprettetProsessinstans) }
        confirmVerified(prosessinstansBehandler)
    }

    private fun lagProsessInstans(block: Prosessinstans.() -> Unit = {}): Prosessinstans = Prosessinstans().apply {
        registrertDato = LocalDateTime.now()
        status = ProsessStatus.PÅ_VENT
        id = UUID.randomUUID()
        block()
    }
}
