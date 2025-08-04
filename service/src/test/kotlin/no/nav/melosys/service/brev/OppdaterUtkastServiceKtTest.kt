package no.nav.melosys.service.brev

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.brev.utkast.BrevbestillingUtkast
import no.nav.melosys.domain.brev.utkast.UtkastBrev
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.UtkastBrevRepository
import no.nav.melosys.service.brev.bestilling.OppdaterUtkastService
import org.junit.jupiter.api.Test

class OppdaterUtkastServiceKtTest {

    private val UTKAST_BREV_ID = 12L

    private val utkastBrevRepository: UtkastBrevRepository = mockk()

    private val oppdaterUtkastService = OppdaterUtkastService(utkastBrevRepository)

    @Test
    fun oppdaterUtkast_mappesRiktig() {
        every { utkastBrevRepository.existsById(UTKAST_BREV_ID) } returns true
        every { utkastBrevRepository.save(any()) } returns mockk<UtkastBrev>()
        val request = lagRequest()

        oppdaterUtkastService.oppdaterUtkast(request)

        val slot = slot<UtkastBrev>()
        verify { utkastBrevRepository.save(capture(slot)) }

        val actual = slot.captured
        actual.id shouldBe request.utkastBrevID()
        actual.behandlingID shouldBe request.behandlingID()
        actual.lagretAvSaksbehandler shouldBe request.saksbehandlerIdent()
        actual.getBrevbestillingUtkast() shouldBe request.brevbestillingUtkast()
    }

    @Test
    fun oppdaterUtkast_utkastFinnes_oppdatererUtkast() {
        every { utkastBrevRepository.existsById(UTKAST_BREV_ID) } returns true
        every { utkastBrevRepository.save(any()) } returns mockk<UtkastBrev>()

        oppdaterUtkastService.oppdaterUtkast(lagRequest())

        verify { utkastBrevRepository.save(any()) }
    }

    @Test
    fun oppdaterUtkast_utkastFinnesIkke_kasterFeil() {
        every { utkastBrevRepository.existsById(UTKAST_BREV_ID) } returns false

        shouldThrow<FunksjonellException> {
            oppdaterUtkastService.oppdaterUtkast(lagRequest())
        }

        verify(exactly = 0) { utkastBrevRepository.save(any()) }
    }

    private fun lagRequest() = OppdaterUtkastService.RequestDto(
        UTKAST_BREV_ID,
        1L,
        "Z123123",
        BrevbestillingUtkast(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            true
        )
    )
}
