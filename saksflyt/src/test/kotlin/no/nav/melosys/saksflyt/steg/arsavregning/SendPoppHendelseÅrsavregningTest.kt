package no.nav.melosys.saksflyt.steg.arsavregning

import io.getunleash.FakeUnleash
import io.kotest.matchers.shouldBe
import io.mockk.*
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.VedtakMetadata
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.integrasjon.hendelser.KafkaPensjonsopptjeningHendelseProducer
import no.nav.melosys.integrasjon.hendelser.PensjonsopptjeningHendelse
import no.nav.melosys.integrasjon.hendelser.PensjonsopptjeningHendelse.*
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class SendPoppHendelseÅrsavregningTest {

    private val behandlingsresultatService: BehandlingsresultatService = mockk()
    private val persondataService: PersondataService = mockk()
    private val kafkaPensjonsopptjeningHendelseProducer: KafkaPensjonsopptjeningHendelseProducer = mockk(relaxed = true)
    private val årsavregningService: ÅrsavregningService = mockk()
    private val fakeUnleash = FakeUnleash()

    private val sendPoppHendelseÅrsavregning = SendPoppHendelseÅrsavregning(
        behandlingsresultatService,
        persondataService,
        kafkaPensjonsopptjeningHendelseProducer,
        årsavregningService,
        fakeUnleash
    )

    @BeforeEach
    fun setup() {
        clearAllMocks()
        fakeUnleash.enableAll()
    }

    @Test
    fun `inngangsSteg should return correct step`() {
        sendPoppHendelseÅrsavregning.inngangsSteg() shouldBe ProsessSteg.SEND_POPP_HENDELSE_AARSAVREGNING
    }

    @Test
    fun `utfør should send POPP event for FTRL yearly settlement`() {
        // Arrange
        val behandlingId = 123L
        val aktørId = "1234567890123"
        val fnr = "12345678901"
        val saksnummer = "SAK123"

        val fagsak = mockk<Fagsak>()
        every { fagsak.type } returns Sakstyper.FTRL
        every { fagsak.saksnummer } returns saksnummer
        every { fagsak.hentBrukersAktørID() } returns aktørId

        val behandling = mockk<Behandling>()
        every { behandling.id } returns behandlingId
        every { behandling.fagsak } returns fagsak

        val årsavregning = mockk<Årsavregning>()
        every { årsavregning.id } returns behandlingId
        every { årsavregning.aar } returns 2023
        every { årsavregning.beregnetAvgiftBelop } returns BigDecimal("50000")
        every { årsavregning.manueltAvgiftBeloep } returns null

        val vedtakMetadata = mockk<VedtakMetadata>()
        every { vedtakMetadata.vedtaksdato } returns Instant.parse("2024-01-15T00:00:00Z")

        val behandlingsresultat = mockk<Behandlingsresultat>()
        every { behandlingsresultat.id } returns behandlingId
        every { behandlingsresultat.behandling } returns behandling
        every { behandlingsresultat.årsavregning } returns årsavregning
        every { behandlingsresultat.vedtakMetadata } returns vedtakMetadata
        every { behandlingsresultat.utledSkatteplikttype() } returns Skatteplikttype.IKKE_SKATTEPLIKTIG

        val prosessinstans = mockk<Prosessinstans>()
        every { prosessinstans.hentBehandling } returns behandling

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns behandlingsresultat
        every { persondataService.hentFolkeregisterident(aktørId) } returns fnr
        every { årsavregningService.finnÅrsavregningerPåFagsak(saksnummer, 2023, null) } returns emptyList()

        val capturedEvent = slot<PensjonsopptjeningHendelse>()
        every { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(capture(capturedEvent)) } just Runs

        // Act
        sendPoppHendelseÅrsavregning.utfør(prosessinstans)

        // Assert
        verify { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(any()) }

        val hendelse = capturedEvent.captured
        hendelse.fnr shouldBe fnr
        hendelse.pgi shouldBe 50000L
        hendelse.inntektsAr shouldBe 2023
        hendelse.endringstype shouldBe Endringstype.NY_INNTEKT
        hendelse.melosysBehandlingID shouldBe behandlingId.toString()
    }

    @Test
    fun `utfør should not send event for non-FTRL case`() {
        // Arrange
        val behandlingId = 123L
        val aktørId = "1234567890123"

        val fagsak = mockk<Fagsak>()
        every { fagsak.type } returns Sakstyper.EU_EOS  // Not FTRL
        every { fagsak.hentBrukersAktørID() } returns aktørId

        val behandling = mockk<Behandling>()
        every { behandling.id } returns behandlingId
        every { behandling.fagsak } returns fagsak

        val årsavregning = mockk<Årsavregning>()
        every { årsavregning.aar } returns 2023

        val behandlingsresultat = mockk<Behandlingsresultat>()
        every { behandlingsresultat.id } returns behandlingId
        every { behandlingsresultat.behandling } returns behandling
        every { behandlingsresultat.årsavregning } returns årsavregning

        val prosessinstans = mockk<Prosessinstans>()
        every { prosessinstans.hentBehandling } returns behandling

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns behandlingsresultat

        // Act
        sendPoppHendelseÅrsavregning.utfør(prosessinstans)

        // Assert
        verify(exactly = 0) { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(any()) }
    }

    @Test
    fun `utfør should determine ENDRING report type when previous yearly settlement exists`() {
        // Arrange
        val behandlingId = 123L
        val aktørId = "1234567890123"
        val fnr = "12345678901"
        val saksnummer = "SAK123"

        val fagsak = mockk<Fagsak>()
        every { fagsak.type } returns Sakstyper.FTRL
        every { fagsak.saksnummer } returns saksnummer
        every { fagsak.hentBrukersAktørID() } returns aktørId

        val behandling = mockk<Behandling>()
        every { behandling.id } returns behandlingId
        every { behandling.fagsak } returns fagsak

        val årsavregning = mockk<Årsavregning>()
        every { årsavregning.id } returns behandlingId
        every { årsavregning.aar } returns 2023
        every { årsavregning.beregnetAvgiftBelop } returns BigDecimal("60000")
        every { årsavregning.manueltAvgiftBeloep } returns null

        val previousÅrsavregning = mockk<Årsavregning>()
        every { previousÅrsavregning.id } returns 100L  // Different ID
        every { previousÅrsavregning.aar } returns 2023  // Same year

        val vedtakMetadata = mockk<VedtakMetadata>()
        every { vedtakMetadata.vedtaksdato } returns Instant.parse("2024-01-15T00:00:00Z")

        val behandlingsresultat = mockk<Behandlingsresultat>()
        every { behandlingsresultat.id } returns behandlingId
        every { behandlingsresultat.behandling } returns behandling
        every { behandlingsresultat.årsavregning } returns årsavregning
        every { behandlingsresultat.vedtakMetadata } returns vedtakMetadata
        every { behandlingsresultat.utledSkatteplikttype() } returns Skatteplikttype.IKKE_SKATTEPLIKTIG

        val prosessinstans = mockk<Prosessinstans>()
        every { prosessinstans.hentBehandling } returns behandling

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns behandlingsresultat
        every { persondataService.hentFolkeregisterident(aktørId) } returns fnr
        every { årsavregningService.finnÅrsavregningerPåFagsak(saksnummer, 2023, null) } returns listOf(previousÅrsavregning)

        val capturedEvent = slot<PensjonsopptjeningHendelse>()
        every { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(capture(capturedEvent)) } just Runs

        // Act
        sendPoppHendelseÅrsavregning.utfør(prosessinstans)

        // Assert
        val hendelse = capturedEvent.captured
        hendelse.endringstype shouldBe Endringstype.OPPDATERING
        hendelse.pgi shouldBe 60000L
    }

    @Test
    fun `utfør should use manual amount when available`() {
        // Arrange
        val behandlingId = 123L
        val aktørId = "1234567890123"
        val fnr = "12345678901"
        val saksnummer = "SAK123"

        val fagsak = mockk<Fagsak>()
        every { fagsak.type } returns Sakstyper.FTRL
        every { fagsak.saksnummer } returns saksnummer
        every { fagsak.hentBrukersAktørID() } returns aktørId

        val behandling = mockk<Behandling>()
        every { behandling.id } returns behandlingId
        every { behandling.fagsak } returns fagsak

        val årsavregning = mockk<Årsavregning>()
        every { årsavregning.id } returns behandlingId
        every { årsavregning.aar } returns 2023
        every { årsavregning.beregnetAvgiftBelop } returns BigDecimal("50000")
        every { årsavregning.manueltAvgiftBeloep } returns BigDecimal("75000")  // Manual amount set

        val vedtakMetadata = mockk<VedtakMetadata>()
        every { vedtakMetadata.vedtaksdato } returns Instant.parse("2024-01-15T00:00:00Z")

        val behandlingsresultat = mockk<Behandlingsresultat>()
        every { behandlingsresultat.id } returns behandlingId
        every { behandlingsresultat.behandling } returns behandling
        every { behandlingsresultat.årsavregning } returns årsavregning
        every { behandlingsresultat.vedtakMetadata } returns vedtakMetadata
        every { behandlingsresultat.utledSkatteplikttype() } returns Skatteplikttype.IKKE_SKATTEPLIKTIG

        val prosessinstans = mockk<Prosessinstans>()
        every { prosessinstans.hentBehandling } returns behandling

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns behandlingsresultat
        every { persondataService.hentFolkeregisterident(aktørId) } returns fnr
        every { årsavregningService.finnÅrsavregningerPåFagsak(saksnummer, 2023, null) } returns emptyList()

        val capturedEvent = slot<PensjonsopptjeningHendelse>()
        every { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(capture(capturedEvent)) } just Runs

        // Act
        sendPoppHendelseÅrsavregning.utfør(prosessinstans)

        // Assert
        val hendelse = capturedEvent.captured
        hendelse.pgi shouldBe 75000L  // Should use manual amount
    }

    @Test
    fun `utfør should not send event when feature toggle is disabled`() {
        // Arrange
        val behandlingId = 123L
        val behandling = mockk<Behandling>()
        every { behandling.id } returns behandlingId

        val prosessinstans = mockk<Prosessinstans>()
        every { prosessinstans.hentBehandling } returns behandling

        // Disable all toggles
        fakeUnleash.disableAll()

        // Act
        sendPoppHendelseÅrsavregning.utfør(prosessinstans)

        // Assert
        verify(exactly = 0) { behandlingsresultatService.hentBehandlingsresultat(any()) }
        verify(exactly = 0) { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(any()) }
    }

    @Test
    fun `utfør should not send event when user is skattepliktig to Norway`() {
        // Arrange
        val behandlingId = 123L
        val aktørId = "1234567890123"
        val saksnummer = "SAK123"

        val fagsak = mockk<Fagsak>()
        every { fagsak.type } returns Sakstyper.FTRL
        every { fagsak.saksnummer } returns saksnummer
        every { fagsak.hentBrukersAktørID() } returns aktørId

        val behandling = mockk<Behandling>()
        every { behandling.id } returns behandlingId
        every { behandling.fagsak } returns fagsak

        val årsavregning = mockk<Årsavregning>()
        every { årsavregning.id } returns behandlingId
        every { årsavregning.aar } returns 2023

        val behandlingsresultat = mockk<Behandlingsresultat>()
        every { behandlingsresultat.id } returns behandlingId
        every { behandlingsresultat.behandling } returns behandling
        every { behandlingsresultat.årsavregning } returns årsavregning
        every { behandlingsresultat.utledSkatteplikttype() } returns Skatteplikttype.SKATTEPLIKTIG

        val prosessinstans = mockk<Prosessinstans>()
        every { prosessinstans.hentBehandling } returns behandling

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns behandlingsresultat

        // Act
        sendPoppHendelseÅrsavregning.utfør(prosessinstans)

        // Assert
        verify(exactly = 0) { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(any()) }
    }

    @Test
    fun `utfør should not send event when skatteplikttype cannot be determined`() {
        // Arrange
        val behandlingId = 123L
        val aktørId = "1234567890123"
        val saksnummer = "SAK123"

        val fagsak = mockk<Fagsak>()
        every { fagsak.type } returns Sakstyper.FTRL
        every { fagsak.saksnummer } returns saksnummer
        every { fagsak.hentBrukersAktørID() } returns aktørId

        val behandling = mockk<Behandling>()
        every { behandling.id } returns behandlingId
        every { behandling.fagsak } returns fagsak

        val årsavregning = mockk<Årsavregning>()
        every { årsavregning.id } returns behandlingId
        every { årsavregning.aar } returns 2023

        val behandlingsresultat = mockk<Behandlingsresultat>()
        every { behandlingsresultat.id } returns behandlingId
        every { behandlingsresultat.behandling } returns behandling
        every { behandlingsresultat.årsavregning } returns årsavregning
        every { behandlingsresultat.utledSkatteplikttype() } throws RuntimeException("Trygdeavgiftsperiode ikke funnet")

        val prosessinstans = mockk<Prosessinstans>()
        every { prosessinstans.hentBehandling } returns behandling

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns behandlingsresultat

        // Act
        sendPoppHendelseÅrsavregning.utfør(prosessinstans)

        // Assert
        verify(exactly = 0) { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(any()) }
    }
}
