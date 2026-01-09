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
        // Simulerer at denne ble opprettet tidligere og har gammel endretDato
        eventProsessinstans = Prosessinstans.builder()
            .medId(prosessinstansId)
            .medType(ProsessType.IVERKSETT_VEDTAK_FTRL)
            .medStatus(ProsessStatus.KLAR)
            .medEndretDato(java.time.LocalDateTime.of(2024, 1, 1, 10, 0))
            .build()

        // Fersk prosessinstans fra databasen - har nyere endretDato
        // Dette simulerer at entiteten har blitt oppdatert mellom event-publisering og AFTER_COMMIT
        freshProsessinstans = Prosessinstans.builder()
            .medId(prosessinstansId)
            .medType(ProsessType.IVERKSETT_VEDTAK_FTRL)
            .medStatus(ProsessStatus.KLAR)
            .medEndretDato(java.time.LocalDateTime.of(2024, 1, 1, 12, 0))
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

        // Ekstra verifisering: sjekk at det IKKE er event-objektet
        assert(capturedProsessinstans.captured !== eventProsessinstans) {
            "Prosessinstans fra event skal IKKE brukes - den kan ha stale data"
        }

        // Verifiser at vi faktisk brukte objektet med nyere endretDato (fra DB)
        assert(capturedProsessinstans.captured.endretDato == freshProsessinstans.endretDato) {
            "Forventet at prosessinstans med oppdatert endretDato ble brukt"
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
