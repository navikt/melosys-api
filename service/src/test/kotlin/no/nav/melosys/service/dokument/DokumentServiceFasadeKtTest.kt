package no.nav.melosys.service.dokument

import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.brev.DoksysBrevbestilling
import no.nav.melosys.domain.brev.Mottaker
import no.nav.melosys.domain.kodeverk.Mottakerroller.BRUKER
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.dokument.brev.BrevbestillingDto
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.ApplicationEventPublisher

@ExtendWith(MockKExtension::class)
class DokumentServiceFasadeKtTest {

    @MockK
    private lateinit var mockDokumentService: DokumentService

    @MockK
    private lateinit var mockDokgenService: DokgenService

    @MockK
    private lateinit var mockBehandlingService: BehandlingService

    @MockK
    private lateinit var applicationEventPublisher: ApplicationEventPublisher

    private lateinit var dokumentServiceFasade: DokumentServiceFasade

    @BeforeEach
    fun init() {
        SpringSubjectHandler.set(TestSubjectHandler())
        dokumentServiceFasade = DokumentServiceFasade(
            mockDokumentService,
            mockDokgenService,
            mockBehandlingService,
            applicationEventPublisher
        )
        clearMocks(mockDokgenService, mockDokumentService, mockBehandlingService)

        // Set up default mocks for methods that are called
        every { mockBehandlingService.hentBehandlingMedSaksopplysninger(any()) } returns null
        every { mockDokumentService.produserDokument(any(), any(), any(), any()) } returns Unit
        every { mockDokgenService.produserOgDistribuerBrev(any(), any()) } returns Unit
        every { applicationEventPublisher.publishEvent(any()) } returns Unit
    }

    @Test
    fun `skal kalle DokumentService produserDokument`() {
        every { mockDokgenService.erTilgjengeligDokgenmal(any()) } returns false

        dokumentServiceFasade.produserDokument(
            MELDING_FORVENTET_SAKSBEHANDLINGSTID,
            Mottaker.medRolle(BRUKER),
            123L,
            DoksysBrevbestilling.Builder().build()
        )

        verify { mockDokumentService.produserDokument(any(), any(), any(), any()) }
    }

    @Test
    fun `skal kalle DokgenService produserOgDistribuer`() {
        every { mockDokgenService.erTilgjengeligDokgenmal(any()) } returns true

        dokumentServiceFasade.produserDokument(
            MELDING_FORVENTET_SAKSBEHANDLINGSTID,
            Mottaker.medRolle(BRUKER),
            123L,
            DoksysBrevbestilling.Builder().build()
        )

        verify { mockDokgenService.produserOgDistribuerBrev(any(), any()) }
        verify(exactly = 0) { mockDokumentService.produserDokument(any(), any(), any(), any()) }
    }

    @Test
    fun `skal kalle DokgenService produserOgDistribuer med dto`() {
        every { mockDokgenService.erTilgjengeligDokgenmal(any()) } returns true

        dokumentServiceFasade.produserDokument(1, BrevbestillingDto())

        verify { mockDokgenService.produserOgDistribuerBrev(any(), any()) }
        verify(exactly = 0) { mockDokumentService.produserDokument(any(), any(), any(), any()) }
    }

    @Test
    fun `skal lage riktig DokgenBrevRequest ved avslag manglende opplysninger`() {
        every { mockDokgenService.erTilgjengeligDokgenmal(Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER) } returns true

        val brevbestilling = DoksysBrevbestilling.Builder()
            .medProduserbartDokument(Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER)
            .medAvsenderID("Z123456")
            .medMottakere(listOf(Mottaker.medRolle(BRUKER)))
            .medFritekst("avslag fritekst")
            .build()

        dokumentServiceFasade.produserDokument(
            Produserbaredokumenter.AVSLAG_MANGLENDE_OPPLYSNINGER,
            Mottaker.medRolle(BRUKER),
            1L,
            brevbestilling
        )

        val brevbestillingRequestSlot = slot<BrevbestillingDto>()
        verify { mockDokgenService.produserOgDistribuerBrev(eq(1L), capture(brevbestillingRequestSlot)) }
        verify(exactly = 0) { mockDokumentService.produserDokument(any(), any(), any(), any()) }

        val dokgenBrevbestillingRequest = brevbestillingRequestSlot.captured

        dokgenBrevbestillingRequest.run {
            bestillersId shouldBe "Z123456"
            mottaker shouldBe BRUKER
            fritekst shouldBe "avslag fritekst"
        }
    }

    @Test
    fun `skal lage riktig DokgenBrevRequest ved melding henlegg sak`() {
        dokumentServiceFasade.produserOgDistribuerBrev(
            Produserbaredokumenter.MELDING_HENLAGT_SAK,
            Mottaker.medRolle(BRUKER),
            "henlagt sak fritekst",
            "ANNET",
            "Z123456",
            1L
        )

        val brevbestillingRequestSlot = slot<BrevbestillingDto>()
        verify { mockDokgenService.produserOgDistribuerBrev(eq(1L), capture(brevbestillingRequestSlot)) }
        verify(exactly = 0) { mockDokumentService.produserDokument(any(), any(), any(), any()) }

        val dokgenBrevbestillingRequest = brevbestillingRequestSlot.captured

        dokgenBrevbestillingRequest.run {
            bestillersId shouldBe "Z123456"
            mottaker shouldBe BRUKER
            fritekst shouldBe "henlagt sak fritekst"
            begrunnelseKode shouldBe "ANNET"
        }
    }
}
