package no.nav.melosys.service.popp

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
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

        val fagsak = io.mockk.mockk<Fagsak> {
            every { hentBrukersAktørID() } returns AKTOR_ID
        }
        val behandling = io.mockk.mockk<Behandling> {
            every { this@mockk.fagsak } returns fagsak
        }
        every { behandlingService.hentBehandling(BEH_ID) } returns behandling
        every { persondataService.hentFolkeregisterident(AKTOR_ID) } returns FNR
    }

    @Test
    fun `hent - bruker 5-ars vindu fomAr=ar-4 tomAr=ar`() {
        every { årsavregningService.finnGjeldendeÅrForÅrsavregning(BEH_ID) } returns 2024
        every { poppInntektClient.hentInntekt(any()) } returns PoppHentInntektResponse(emptyList())

        val capturedRequest = slot<PoppHentInntektRequest>()

        oppslag.hent(BEH_ID)

        verify { poppInntektClient.hentInntekt(capture(capturedRequest)) }
        capturedRequest.captured.fnr shouldBe FNR
        capturedRequest.captured.fomAr shouldBe 2020
        capturedRequest.captured.tomAr shouldBe 2024
    }

    @Test
    fun `hent - mapper poster og sorterer nyeste ar forst`() {
        every { årsavregningService.finnGjeldendeÅrForÅrsavregning(BEH_ID) } returns 2024
        every { poppInntektClient.hentInntekt(any()) } returns PoppHentInntektResponse(
            listOf(
                PoppInntektPost(inntektAr = 2022, belop = 100, kilde = "SKATT"),
                PoppInntektPost(inntektAr = 2024, belop = 540000, kilde = "SKATT"),
                PoppInntektPost(inntektAr = 2024, belop = 120000, kilde = "AVGIFTSSYSTEMET"),
            )
        )

        val resultat = oppslag.hent(BEH_ID)

        resultat.inntektsAr shouldBe 2024
        resultat.perioder.shouldHaveSize(3)
        resultat.perioder.first().aar shouldBe 2024
        resultat.perioder.map { it.aar } shouldBe listOf(2024, 2024, 2022)
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
