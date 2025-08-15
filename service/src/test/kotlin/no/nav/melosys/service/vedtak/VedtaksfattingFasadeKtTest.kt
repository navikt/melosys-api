package no.nav.melosys.service.vedtak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Vedtakstyper.FØRSTEGANGSVEDTAK
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.TestSubjectHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class VedtaksfattingFasadeKtTest {

    private val behandlingID = 1L

    @MockK
    private lateinit var mockBehandlingService: BehandlingService

    @MockK
    private lateinit var mockEosVedtakService: EosVedtakService

    @MockK
    private lateinit var mockFtrlVedtakService: FtrlVedtakService

    @MockK
    private lateinit var trygdeavtaleVedtakService: TrygdeavtaleVedtakService

    @MockK
    private lateinit var årsavregningVedtakService: ÅrsavregningVedtakService

    private lateinit var vedtaksfattingFasade: VedtaksfattingFasade

    private lateinit var behandling: Behandling

    @BeforeEach
    fun init() {
        vedtaksfattingFasade = VedtaksfattingFasade(
            mockBehandlingService,
            FattVedtakVelger(mockEosVedtakService, mockFtrlVedtakService, trygdeavtaleVedtakService, årsavregningVedtakService)
        )
        behandling = lagBehandling()

        SpringSubjectHandler.set(TestSubjectHandler())

        // Mock setups for service methods
        every { mockEosVedtakService.fattVedtak(any(), any()) } returns Unit
        every { mockFtrlVedtakService.fattVedtak(any(), any()) } returns Unit
        every { trygdeavtaleVedtakService.fattVedtak(any(), any()) } returns Unit
        every { årsavregningVedtakService.fattVedtak(any(), any()) } returns Unit
    }

    @Test
    fun `skal kaste exception ved ugyldig behandlingstema`() {
        behandling.tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING
        every { mockBehandlingService.hentBehandling(behandlingID) } returns behandling
        val fattVedtakRequest = lagFattFtrlVedtakRequest()


        val exception = shouldThrow<FunksjonellException> {
            vedtaksfattingFasade.fattVedtak(behandlingID, fattVedtakRequest)
        }


        exception.message shouldContain "Kan ikke fatte vedtak ved behandlingstema"
    }

    @Test
    fun `skal kalle EOS vedtakservice for EU EØS saker`() {
        setFagsakPåBehandling(Sakstyper.EU_EOS)
        every { mockBehandlingService.hentBehandling(behandlingID) } returns behandling


        vedtaksfattingFasade.fattVedtak(behandlingID, lagFattEosVedtakRequest())


        verify { mockEosVedtakService.fattVedtak(eq(behandling), any()) }
        verify(exactly = 0) { mockFtrlVedtakService.fattVedtak(any(), any()) }
    }

    @Test
    fun `skal kalle EOS vedtakservice for delvis automatiserte vedtak`() {
        every { mockBehandlingService.hentBehandling(behandlingID) } returns behandling
        val request = FattVedtakRequest.Builder()
            .medBehandlingsresultatType(FASTSATT_LOVVALGSLAND)
            .medVedtakstype(FØRSTEGANGSVEDTAK).build()


        vedtaksfattingFasade.fattVedtak(behandlingID, request)


        verify {
            mockEosVedtakService.fattVedtak(
                match { it.id == behandlingID },
                match {
                    it.vedtakstype == FØRSTEGANGSVEDTAK &&
                        it.behandlingsresultatTypeKode == FASTSATT_LOVVALGSLAND
                }
            )
        }
        verify(exactly = 0) { mockFtrlVedtakService.fattVedtak(any(), any()) }
    }

    @Test
    fun `skal kalle FTRL vedtakservice for FTRL saker`() {
        setFagsakPåBehandling(Sakstyper.FTRL)
        every { mockBehandlingService.hentBehandling(behandlingID) } returns behandling


        vedtaksfattingFasade.fattVedtak(behandlingID, lagFattFtrlVedtakRequest())


        verify { mockFtrlVedtakService.fattVedtak(eq(behandling), any()) }
        verify(exactly = 0) { mockEosVedtakService.fattVedtak(any(), any()) }
    }

    @Test
    fun `skal kalle trygdeavtale vedtakservice for trygdeavtaler`() {
        setFagsakPåBehandling(Sakstyper.TRYGDEAVTALE)
        every { mockBehandlingService.hentBehandling(behandlingID) } returns behandling


        vedtaksfattingFasade.fattVedtak(behandlingID, lagFattTrygdeavtaleVedtakRequest())


        verify { trygdeavtaleVedtakService.fattVedtak(eq(behandling), any()) }
        verify(exactly = 0) { mockEosVedtakService.fattVedtak(any(), any()) }
    }

    private fun lagBehandling() = Behandling.forTest {
        id = behandlingID
        this.status = Behandlingsstatus.AVSLUTTET
        type = Behandlingstyper.FØRSTEGANG
        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
    }

    private fun setFagsakPåBehandling(sakstype: Sakstyper) {
        behandling.fagsak = Fagsak.forTest {
            type = sakstype
        }
    }

    private fun lagFattEosVedtakRequest() = FattVedtakRequest.Builder()
        .medBehandlingsresultatType(FASTSATT_LOVVALGSLAND)
        .medVedtakstype(FØRSTEGANGSVEDTAK)
        .medFritekst("Fritekst")
        .build()

    private fun lagFattFtrlVedtakRequest() = FattVedtakRequest.Builder()
        .medBehandlingsresultatType(FASTSATT_LOVVALGSLAND)
        .medVedtakstype(FØRSTEGANGSVEDTAK)
        .medBegrunnelseFritekst("Begrunnelse")
        .medBestillersId(SubjectHandler.getInstance().userID)
        .build()

    private fun lagFattTrygdeavtaleVedtakRequest() = FattVedtakRequest.Builder()
        .medBehandlingsresultatType(FASTSATT_LOVVALGSLAND)
        .medVedtakstype(FØRSTEGANGSVEDTAK)
        .medBegrunnelseFritekst("Begrunnelse")
        .build()
}
