package no.nav.melosys.saksflyt.steg.arsavregning

import io.getunleash.FakeUnleash
import io.kotest.matchers.shouldBe
import io.mockk.*
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.integrasjon.hendelser.KafkaPensjonsopptjeningHendelseProducer
import no.nav.melosys.integrasjon.hendelser.PensjonsopptjeningHendelse
import no.nav.melosys.integrasjon.hendelser.PensjonsopptjeningHendelse.*
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

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
        val testAktørId = "1234567890123"
        val fnr = "12345678901"
        val saksnummer = "SAK123"

        val behandlingsresultat = behandlingsresultatForTest {
            behandling {
                id = behandlingId
                fagsak {
                    this.saksnummer = saksnummer
                    type = Sakstyper.FTRL
                    medBruker {
                        aktørId = testAktørId
                    }
                }
            }
            årsavregning {
                id = behandlingId
                aar = 2023
                beregnetAvgiftBelop = BigDecimal("50000")
            }
            vedtakMetadata {
                vedtaksdato = Instant.parse("2024-01-15T00:00:00Z")
            }
            medlemskapsperiode {
                fom = LocalDate.of(2023, 1, 1)
                tom = LocalDate.of(2023, 12, 31)
                trygdeavgiftsperioder = setOf(
                    createTrygdeavgiftsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG, this)
                )
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            behandling {
                id = behandlingId
                fagsak {
                    this.saksnummer = saksnummer
                    type = Sakstyper.FTRL
                    medBruker {
                        aktørId = testAktørId
                    }
                }
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns behandlingsresultat
        every { persondataService.hentFolkeregisterident(testAktørId) } returns fnr
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

        val behandlingsresultat = behandlingsresultatForTest {
            behandling {
                id = behandlingId
                fagsak {
                    type = Sakstyper.EU_EOS  // Not FTRL
                    medBruker()
                }
            }
            årsavregning {
                aar = 2023
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            behandling {
                id = behandlingId
                fagsak = behandlingsresultat.behandling!!.fagsak
            }
        }

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
        val testAktørId = "1234567890123"
        val fnr = "12345678901"
        val saksnummer = "SAK123"

        val behandlingsresultat = behandlingsresultatForTest {
            behandling {
                id = behandlingId
                fagsak {
                    this.saksnummer = saksnummer
                    type = Sakstyper.FTRL
                    medBruker {
                        aktørId = testAktørId
                    }
                }
            }
            årsavregning {
                id = behandlingId
                aar = 2023
                beregnetAvgiftBelop = BigDecimal("60000")
            }
            vedtakMetadata {
                vedtaksdato = Instant.parse("2024-01-15T00:00:00Z")
            }
            medlemskapsperiode {
                fom = LocalDate.of(2023, 1, 1)
                tom = LocalDate.of(2023, 12, 31)
                trygdeavgiftsperioder = setOf(
                    createTrygdeavgiftsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG, this)
                )
            }
        }

        val previousÅrsavregning = Årsavregning.forTest {
            id = 100L  // Different ID
            aar = 2023  // Same year
        }

        val prosessinstans = Prosessinstans.forTest {
            behandling {
                id = behandlingId
                fagsak = behandlingsresultat.hentBehandling().fagsak
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns behandlingsresultat
        every { persondataService.hentFolkeregisterident(testAktørId) } returns fnr
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
        val testAktørId = "1234567890123"
        val fnr = "12345678901"
        val saksnummer = "SAK123"

        val behandlingsresultat = behandlingsresultatForTest {
            behandling {
                id = behandlingId
                fagsak {
                    this.saksnummer = saksnummer
                    type = Sakstyper.FTRL
                    medBruker {
                        aktørId = testAktørId
                    }
                }
            }
            årsavregning {
                id = behandlingId
                aar = 2023
                beregnetAvgiftBelop = BigDecimal("50000")
                manueltAvgiftBeloep = BigDecimal("75000")  // Manual amount set
            }
            vedtakMetadata {
                vedtaksdato = Instant.parse("2024-01-15T00:00:00Z")
            }
            medlemskapsperiode {
                fom = LocalDate.of(2023, 1, 1)
                tom = LocalDate.of(2023, 12, 31)
                trygdeavgiftsperioder = setOf(
                    createTrygdeavgiftsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG, this)
                )
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            behandling {
                id = behandlingId
                fagsak = behandlingsresultat.behandling!!.fagsak
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns behandlingsresultat
        every { persondataService.hentFolkeregisterident(testAktørId) } returns fnr
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

        val prosessinstans = Prosessinstans.forTest {
            behandling {
                id = behandlingId
            }
        }

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

        val behandlingsresultat = behandlingsresultatForTest {
            behandling {
                id = behandlingId
                fagsak {
                    type = Sakstyper.FTRL
                    medBruker()
                }
            }
            årsavregning {
                aar = 2023
            }
            medlemskapsperiode {
                fom = LocalDate.of(2023, 1, 1)
                tom = LocalDate.of(2023, 12, 31)
                trygdeavgiftsperioder = setOf(
                    createTrygdeavgiftsperiode(Skatteplikttype.SKATTEPLIKTIG, this)
                )
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            behandling {
                id = behandlingId
                fagsak = behandlingsresultat.behandling!!.fagsak
            }
        }

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

        val behandlingsresultat = behandlingsresultatForTest {
            behandling {
                id = behandlingId
                fagsak {
                    type = Sakstyper.FTRL
                    medBruker()
                }
            }
            årsavregning {
                aar = 2023
            }
            // No trygdeavgiftsperioder - will cause utledSkatteplikttype() to throw
        }

        val prosessinstans = Prosessinstans.forTest {
            behandling {
                id = behandlingId
                fagsak = behandlingsresultat.behandling!!.fagsak
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns behandlingsresultat

        // Act
        sendPoppHendelseÅrsavregning.utfør(prosessinstans)

        // Assert
        verify(exactly = 0) { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(any()) }
    }

    // Helper function to create Trygdeavgiftsperiode with skatteplikttype
    private fun createTrygdeavgiftsperiode(
        skatteplikttype: Skatteplikttype,
        medlemskapsperiode: Medlemskapsperiode
    ): Trygdeavgiftsperiode {
        val skatteforholdTilNorge = SkatteforholdTilNorge().apply {
            fomDato = LocalDate.of(2023, 1, 1)
            tomDato = LocalDate.of(2023, 12, 31)
            this.skatteplikttype = skatteplikttype
        }

        val inntektsperiode = Inntektsperiode().apply {
            fomDato = LocalDate.of(2023, 1, 1)
            tomDato = LocalDate.of(2023, 12, 31)
            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
            avgiftspliktigMndInntekt = Penger(10000.toBigDecimal())
        }

        return Trygdeavgiftsperiode(
            periodeFra = LocalDate.of(2023, 1, 1),
            periodeTil = LocalDate.of(2023, 12, 31),
            trygdesats = 6.8.toBigDecimal(),
            trygdeavgiftsbeløpMd = Penger(1000.toBigDecimal()),
            grunnlagMedlemskapsperiode = medlemskapsperiode,
            grunnlagSkatteforholdTilNorge = skatteforholdTilNorge,
            grunnlagInntekstperiode = inntektsperiode
        )
    }
}
