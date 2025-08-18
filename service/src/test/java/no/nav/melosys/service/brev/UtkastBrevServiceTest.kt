package no.nav.melosys.service.brev

import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.brev.utkast.BrevbestillingUtkast
import no.nav.melosys.domain.brev.utkast.UtkastBrev
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.UtkastBrevRepository
import no.nav.melosys.service.brev.bestilling.OppdaterUtkastService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class UtkastBrevServiceTest {

    @RelaxedMockK
    lateinit var utkastBrevRepository: UtkastBrevRepository

    @RelaxedMockK
    lateinit var oppdaterUtkastService: OppdaterUtkastService

    @InjectMockKs
    lateinit var utkastBrevService: UtkastBrevService

    @Test
    fun `lagre utkast finnes ingen utkast for samme brev ok`() {
        every { utkastBrevRepository.findAllByBehandlingIDOrderByLagringsdatoDesc(BEHANDLING_ID) } returns listOf(lagUtkastBrev(null))
        every { utkastBrevRepository.save(any<UtkastBrev>()) } answers { firstArg<UtkastBrev>() }
        val brevbestillingUtkast = lagBrevbestillingUtkast(DOKUMENT_TITTEL)


        utkastBrevService.lagreUtkast(BEHANDLING_ID, "Z123456", brevbestillingUtkast)


        verify { utkastBrevRepository.save(any()) }
    }

    @Test
    fun `lagre utkast finnes utkast for samme brev feiler`() {
        every { utkastBrevRepository.findAllByBehandlingIDOrderByLagringsdatoDesc(BEHANDLING_ID) } returns listOf(lagUtkastBrev(DOKUMENT_TITTEL))
        every { utkastBrevRepository.save(any<UtkastBrev>()) } answers { firstArg<UtkastBrev>() }
        val brevbestillingUtkast = lagBrevbestillingUtkast(DOKUMENT_TITTEL)


        shouldThrow<FunksjonellException> {
            utkastBrevService.lagreUtkast(BEHANDLING_ID, "Z123456", brevbestillingUtkast)
        }


        verify(exactly = 0) { utkastBrevRepository.save(any()) }
    }

    private fun lagBrevbestillingUtkast(dokumentTittel: String?) = BrevbestillingUtkast(
        Produserbaredokumenter.FRITEKSTBREV,
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        false, null, null, null, null, dokumentTittel, null, true
    )

    private fun lagUtkastBrev(dokumentTittel: String?) = UtkastBrev.Builder().apply {
        brevbestillingUtkast(lagBrevbestillingUtkast(dokumentTittel))
    }.build()

    companion object {
        private const val BEHANDLING_ID = 300L
        private const val DOKUMENT_TITTEL = "Dokumenttittel"
    }
}
