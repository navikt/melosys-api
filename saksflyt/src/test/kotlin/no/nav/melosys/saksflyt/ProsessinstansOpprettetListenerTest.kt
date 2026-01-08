package no.nav.melosys.saksflyt

import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.saksflytapi.ProsessinstansOpprettetEvent
import no.nav.melosys.saksflytapi.domain.ProsessStatus
import no.nav.melosys.saksflytapi.domain.ProsessType
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class ProsessinstansOpprettetListenerTest {

    @MockK
    lateinit var prosessinstansBehandlerDelegate: ProsessinstansBehandlerDelegate

    @MockK
    lateinit var prosessinstansBehandler: ProsessinstansBehandler

    @MockK
    lateinit var prosessinstansRepository: ProsessinstansRepository

    @InjectMockKs
    lateinit var listener: ProsessinstansOpprettetListener

    private lateinit var prosessinstansId: UUID
    private lateinit var eventProsessinstans: Prosessinstans
    private lateinit var freshProsessinstans: Prosessinstans
    private lateinit var event: ProsessinstansOpprettetEvent

    @BeforeEach
    fun setUp() {
        prosessinstansId = UUID.randomUUID()

        // Prosessinstans fra eventen (stale referanse fra HTTP-transaksjon)
        eventProsessinstans = Prosessinstans.builder()
            .medId(prosessinstansId)
            .medType(ProsessType.IVERKSETT_VEDTAK_FTRL)
            .medStatus(ProsessStatus.KLAR)
            .build()

        // Fersk prosessinstans fra databasen
        freshProsessinstans = Prosessinstans.builder()
            .medId(prosessinstansId)
            .medType(ProsessType.IVERKSETT_VEDTAK_FTRL)
            .medStatus(ProsessStatus.KLAR)
            .build()

        event = ProsessinstansOpprettetEvent(eventProsessinstans)
    }

    @Test
    fun `behandleOpprettetProsessinstans laster prosessinstans fra database`() {
        // Given
        every { prosessinstansRepository.findById(prosessinstansId) } returns Optional.of(freshProsessinstans)
        every { prosessinstansBehandler.behandleProsessinstans(any()) } just Runs

        // When
        listener.behandleOpprettetProsessinstans(event)

        // Then - verifiser at findById ble kalt med riktig ID
        verify { prosessinstansRepository.findById(prosessinstansId) }

        // Verifiser at behandleProsessinstans ble kalt med prosessinstansen fra repository (ikke fra event)
        val capturedProsessinstans = slot<Prosessinstans>()
        verify { prosessinstansBehandler.behandleProsessinstans(capture(capturedProsessinstans)) }

        // Den fangede prosessinstansen skal være den fra repository, ikke den fra event
        // Vi sjekker dette ved å verifisere at det er samme referanse som freshProsessinstans
        assert(capturedProsessinstans.captured === freshProsessinstans) {
            "Forventet at prosessinstans fra repository ble brukt, ikke fra event"
        }
    }

    @Test
    fun `behandleOpprettetProsessinstans starter ikke behandling når prosessinstans er på vent`() {
        // Given
        val prosessinstansPåVent = Prosessinstans.builder()
            .medId(prosessinstansId)
            .medType(ProsessType.IVERKSETT_VEDTAK_FTRL)
            .medStatus(ProsessStatus.PÅ_VENT)
            .build()

        every { prosessinstansRepository.findById(prosessinstansId) } returns Optional.of(prosessinstansPåVent)

        // When
        listener.behandleOpprettetProsessinstans(event)

        // Then
        verify { prosessinstansRepository.findById(prosessinstansId) }
        verify(exactly = 0) { prosessinstansBehandler.behandleProsessinstans(any()) }
    }

    @Test
    fun `behandleOpprettetProsessinstans kaster exception når prosessinstans ikke finnes`() {
        // Given
        every { prosessinstansRepository.findById(prosessinstansId) } returns Optional.empty()

        // When/Then
        assertThrows<IllegalStateException> {
            listener.behandleOpprettetProsessinstans(event)
        }

        verify(exactly = 0) { prosessinstansBehandler.behandleProsessinstans(any()) }
    }

    @Test
    fun `oppdaterProsessinstansstatus bruker prosessinstans fra event`() {
        // Given - BEFORE_COMMIT bruker fortsatt event-objektet siden det er i samme transaksjon
        every { prosessinstansBehandlerDelegate.oppdaterStatusOmSkalPåVent(any()) } just Runs

        // When
        listener.oppdaterProsessinstansstatus(event)

        // Then
        verify { prosessinstansBehandlerDelegate.oppdaterStatusOmSkalPåVent(eventProsessinstans) }
    }
}
