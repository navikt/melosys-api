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
import no.nav.melosys.integrasjon.popp.PoppChangeStamp
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
import java.time.LocalDate
import java.util.Date

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
    fun `hent - bruker 5-ars vindu og ber om alle inntektTyper`() {
        every { årsavregningService.finnGjeldendeÅrForÅrsavregning(BEH_ID) } returns 2024
        every { poppInntektClient.hentInntekt(any()) } returns PoppHentInntektResponse(emptyList())

        val capturedRequest = slot<PoppHentInntektRequest>()

        oppslag.hent(BEH_ID)

        verify { poppInntektClient.hentInntekt(capture(capturedRequest)) }
        capturedRequest.captured.fnr shouldBe FNR
        capturedRequest.captured.fomAr shouldBe 2020
        capturedRequest.captured.tomAr shouldBe 2024
        capturedRequest.captured.inntektType shouldBe null
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
                PoppInntektPost(inntektAr = 2024, belop = 120000, kilde = "AVGIFTSSYSTEMET", inntektType = "FL_PGI_LOENN"),
                PoppInntektPost(inntektAr = 2022, belop = 100, kilde = "SKATT", inntektType = "FL_PGI_LOENN"),
                PoppInntektPost(inntektAr = 2024, belop = 540000, kilde = "SKATT", inntektType = "FL_PGI_LOENN"),
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
    fun `hent - PGI-typer sorteres alfabetisk innen samme ar og kilde`() {
        every { årsavregningService.finnGjeldendeÅrForÅrsavregning(BEH_ID) } returns 2024
        every { poppInntektClient.hentInntekt(any()) } returns PoppHentInntektResponse(
            listOf(
                PoppInntektPost(inntektAr = 2024, belop = 100, kilde = "SKATT", inntektType = "KSL_PGI_LOENN"),
                PoppInntektPost(inntektAr = 2024, belop = 540000, kilde = "SKATT", inntektType = "SVA_PGI_LOENN"),
                PoppInntektPost(inntektAr = 2024, belop = 200, kilde = "SKATT", inntektType = "FL_PGI_LOENN"),
            )
        )

        val perioder = oppslag.hent(BEH_ID).perioder

        perioder.map { it.inntektType } shouldBe listOf("FL_PGI_LOENN", "KSL_PGI_LOENN", "SVA_PGI_LOENN")
    }

    @Test
    fun `hent - filtrerer bort ikke-PGI inntektTyper inkludert SUM_PI`() {
        every { årsavregningService.finnGjeldendeÅrForÅrsavregning(BEH_ID) } returns 2024
        every { poppInntektClient.hentInntekt(any()) } returns PoppHentInntektResponse(
            listOf(
                PoppInntektPost(inntektAr = 2024, belop = 540000, kilde = "SKATT", inntektType = "SUM_PI"),
                PoppInntektPost(inntektAr = 2024, belop = 400000, kilde = "SKATT", inntektType = "INN_LON"),
                PoppInntektPost(inntektAr = 2024, belop = 50000, kilde = "SKATT", inntektType = "DIP_SEL"),
                PoppInntektPost(inntektAr = 2024, belop = 200000, kilde = "SKATT", inntektType = "FL_PGI_LOENN"),
                PoppInntektPost(inntektAr = 2024, belop = 30000, kilde = "SKATT", inntektType = null),
            )
        )

        val perioder = oppslag.hent(BEH_ID).perioder

        perioder.map { it.inntektType } shouldBe listOf("FL_PGI_LOENN")
    }

    @Test
    fun `hent - hopper over poster uten ar eller belop`() {
        every { årsavregningService.finnGjeldendeÅrForÅrsavregning(BEH_ID) } returns 2024
        every { poppInntektClient.hentInntekt(any()) } returns PoppHentInntektResponse(
            listOf(
                PoppInntektPost(inntektAr = null, belop = 100, kilde = "SKATT", inntektType = "FL_PGI_LOENN"),
                PoppInntektPost(inntektAr = 2024, belop = null, kilde = "SKATT", inntektType = "FL_PGI_LOENN"),
            )
        )

        oppslag.hent(BEH_ID).perioder.shouldBeEmpty()
    }

    @Test
    fun `hent - null kilde mappes til UKJENT`() {
        every { årsavregningService.finnGjeldendeÅrForÅrsavregning(BEH_ID) } returns 2024
        every { poppInntektClient.hentInntekt(any()) } returns PoppHentInntektResponse(
            listOf(PoppInntektPost(inntektAr = 2024, belop = 100, kilde = null, inntektType = "FL_PGI_LOENN"))
        )

        val perioder = oppslag.hent(BEH_ID).perioder

        perioder.shouldHaveSize(1)
        perioder.first().kilde shouldBe "UKJENT"
    }

    @Test
    fun `hent - mapper inntektTypeDekode fra POPP-post`() {
        every { årsavregningService.finnGjeldendeÅrForÅrsavregning(BEH_ID) } returns 2024
        every { poppInntektClient.hentInntekt(any()) } returns PoppHentInntektResponse(
            listOf(
                PoppInntektPost(
                    inntektAr = 2024,
                    belop = 200000,
                    kilde = "SKATT",
                    inntektType = "FL_PGI_LOENN",
                    inntektTypeDekode = "Lønn frilanser",
                ),
            )
        )

        val periode = oppslag.hent(BEH_ID).perioder.single()

        periode.inntektType shouldBe "FL_PGI_LOENN"
        periode.inntektTypeDekode shouldBe "Lønn frilanser"
    }

    @Test
    fun `hent - mapper changeStamp til Oslo LocalDate for registrert og oppdatert`() {
        every { årsavregningService.finnGjeldendeÅrForÅrsavregning(BEH_ID) } returns 2024
        // 2026-07-31 23:30 UTC = 2026-08-01 01:30 Europe/Oslo (CEST, +02:00)
        val createdUtc = Date.from(java.time.Instant.parse("2026-07-31T23:30:00Z"))
        // 2026-01-31 23:30 UTC = 2026-02-01 00:30 Europe/Oslo (CET, +01:00)
        val updatedUtc = Date.from(java.time.Instant.parse("2026-01-31T23:30:00Z"))
        every { poppInntektClient.hentInntekt(any()) } returns PoppHentInntektResponse(
            listOf(
                PoppInntektPost(
                    inntektAr = 2024,
                    belop = 540000,
                    kilde = "SKATT",
                    inntektType = "FL_PGI_LOENN",
                    changeStamp = PoppChangeStamp(createdDate = createdUtc, updatedDate = updatedUtc),
                ),
            )
        )

        val periode = oppslag.hent(BEH_ID).perioder.single()

        periode.registrert shouldBe LocalDate.of(2026, 8, 1)
        periode.oppdatert shouldBe LocalDate.of(2026, 2, 1)
    }

    @Test
    fun `hent - manglende changeStamp gir null registrert og oppdatert`() {
        every { årsavregningService.finnGjeldendeÅrForÅrsavregning(BEH_ID) } returns 2024
        every { poppInntektClient.hentInntekt(any()) } returns PoppHentInntektResponse(
            listOf(
                PoppInntektPost(inntektAr = 2024, belop = 540000, kilde = "SKATT", inntektType = "FL_PGI_LOENN", changeStamp = null),
                PoppInntektPost(
                    inntektAr = 2023,
                    belop = 510000,
                    kilde = "SKATT",
                    inntektType = "FL_PGI_LOENN",
                    changeStamp = PoppChangeStamp(createdDate = null, updatedDate = null),
                ),
            )
        )

        val perioder = oppslag.hent(BEH_ID).perioder

        perioder.forEach {
            it.registrert shouldBe null
            it.oppdatert shouldBe null
        }
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
