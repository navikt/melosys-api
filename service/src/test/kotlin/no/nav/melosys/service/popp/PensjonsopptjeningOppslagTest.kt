package no.nav.melosys.service.popp

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.popp.PoppHentInntektRequest
import no.nav.melosys.integrasjon.popp.PoppHentInntektResponse
import no.nav.melosys.integrasjon.popp.PoppInntektClient
import no.nav.melosys.integrasjon.popp.PoppInntektPost
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class PensjonsopptjeningOppslagTest {

    @RelaxedMockK
    lateinit var behandlingService: BehandlingService

    @RelaxedMockK
    lateinit var årsavregningService: ÅrsavregningService

    @RelaxedMockK
    lateinit var persondataService: PersondataService

    @RelaxedMockK
    lateinit var poppInntektClient: PoppInntektClient

    private lateinit var oppslag: PensjonsopptjeningOppslag

    @BeforeEach
    fun setUp() {
        oppslag = PensjonsopptjeningOppslag(
            behandlingService,
            årsavregningService,
            persondataService,
            poppInntektClient,
        )

        val fagsak = mockk<Fagsak> {
            every { hentBrukersAktørID() } returns AKTOR_ID
        }
        val behandling = mockk<Behandling> {
            every { this@mockk.fagsak } returns fagsak
        }
        every { behandlingService.hentBehandling(BEH_ID) } returns behandling
        every { persondataService.hentFolkeregisterident(AKTOR_ID) } returns FNR
    }

    @Test
    fun `hent - bruker 5-ars vindu og inntektType=SUM_PI`() {
        every { årsavregningService.finnGjeldendeÅrForÅrsavregning(BEH_ID) } returns 2024
        every { poppInntektClient.hentInntekt(any()) } returns PoppHentInntektResponse(emptyList())

        val capturedRequest = slot<PoppHentInntektRequest>()

        oppslag.hent(BEH_ID)

        verify { poppInntektClient.hentInntekt(capture(capturedRequest)) }
        capturedRequest.captured.fnr shouldBe FNR
        capturedRequest.captured.fomAr shouldBe 2020
        capturedRequest.captured.tomAr shouldBe 2024
        capturedRequest.captured.inntektType shouldBe "SUM_PI"
    }

    @Test
    fun `hent - manglende arsavregning - kaster IkkeFunnetException`() {
        every { årsavregningService.finnGjeldendeÅrForÅrsavregning(BEH_ID) } returns null

        shouldThrow<IkkeFunnetException> { oppslag.hent(BEH_ID) }
    }

    @Test
    fun `hent - sorterer nyeste ar forst og SKATT foran AVGIFTSSYSTEMET innen samme ar`() {
        every { årsavregningService.finnGjeldendeÅrForÅrsavregning(BEH_ID) } returns 2024
        every { poppInntektClient.hentInntekt(any()) } returns PoppHentInntektResponse(
            listOf(
                PoppInntektPost(inntektAr = 2024, belop = 120000, kilde = "AVGIFTSSYSTEMET"),
                PoppInntektPost(inntektAr = 2022, belop = 100, kilde = "SKATT"),
                PoppInntektPost(inntektAr = 2024, belop = 540000, kilde = "SKATT"),
            )
        )

        val perioder = oppslag.hent(BEH_ID).perioder

        perioder.shouldHaveSize(3)
        perioder.map { it.aar to it.kilde } shouldBe listOf(
            2024 to "SKATT",
            2024 to "AVGIFTSSYSTEMET",
            2022 to "SKATT",
        )
    }

    @Test
    fun `hent - hopper over poster uten ar eller belop`() {
        every { årsavregningService.finnGjeldendeÅrForÅrsavregning(BEH_ID) } returns 2024
        every { poppInntektClient.hentInntekt(any()) } returns PoppHentInntektResponse(
            listOf(
                PoppInntektPost(inntektAr = null, belop = 100, kilde = "SKATT"),
                PoppInntektPost(inntektAr = 2024, belop = null, kilde = "SKATT"),
            )
        )

        oppslag.hent(BEH_ID).perioder.shouldBeEmpty()
    }

    @Test
    fun `hent - null kilde mappes til UKJENT`() {
        every { årsavregningService.finnGjeldendeÅrForÅrsavregning(BEH_ID) } returns 2024
        every { poppInntektClient.hentInntekt(any()) } returns PoppHentInntektResponse(
            listOf(PoppInntektPost(inntektAr = 2024, belop = 100, kilde = null))
        )

        val perioder = oppslag.hent(BEH_ID).perioder

        perioder.shouldHaveSize(1)
        perioder.first().kilde shouldBe "UKJENT"
    }

    @Test
    fun `hent - tom liste fra POPP - tom perioder`() {
        every { årsavregningService.finnGjeldendeÅrForÅrsavregning(BEH_ID) } returns 2024
        every { poppInntektClient.hentInntekt(any()) } returns PoppHentInntektResponse(emptyList())

        oppslag.hent(BEH_ID).perioder.shouldBeEmpty()
    }

    companion object {
        private const val BEH_ID = 1234L
        private const val AKTOR_ID = "10000000000"
        private const val FNR = "12345678901"
    }
}
