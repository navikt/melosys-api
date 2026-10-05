package no.nav.melosys.saksflyt.steg.arsavregning

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.BehandlingsresultatTestFactory
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.helseutgiftDekkesPeriode
import no.nav.melosys.domain.medlemskapsperiode
import no.nav.melosys.domain.tidligereBehandlingsresultat
import no.nav.melosys.domain.vedtakMetadata
import no.nav.melosys.domain.årsavregning
import no.nav.melosys.domain.avgift.ÅrsavregningTestFactory
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenClient
import no.nav.melosys.integrasjon.faktureringskomponenten.NyFakturaserieResponseDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaDto
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.ProsessinstansTestFactory
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.avgift.aarsavregning.GjeldendeBehandlingsresultaterForÅrsavregning
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockKExtension::class)
class SendFakturaÅrsavregningTest {

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var pdlService: PersondataService

    @MockK
    private lateinit var faktureringskomponentenClient: FaktureringskomponentenClient

    @MockK
    private lateinit var årsavregningService: ÅrsavregningService

    private lateinit var sendFakturaÅrsavregning: SendFakturaÅrsavregning

    @BeforeEach
    fun setUp() {
        sendFakturaÅrsavregning = SendFakturaÅrsavregning(
            behandlingService,
            behandlingsresultatService,
            faktureringskomponentenClient,
            pdlService,
            årsavregningService
        )
        every { årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(any(), any(), any()) } returns null
    }

    @Test
    fun `sender ikke faktura når faktureringsbelop er mindre enn 100`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            årsavregning {
                aar = 2023
                tilFaktureringBeloep = BigDecimal(99)
            }
        }
        val prosessinstans = lagProsessInstans {
            behandling {
                id = 100
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(prosessinstans.hentBehandling.id) } returns behandlingsresultat

        sendFakturaÅrsavregning.utfør(prosessinstans)

        verify(exactly = 0) { faktureringskomponentenClient.lagFaktura(any(), any()) }
    }

    @Test
    fun `sender faktura når belop er større eller lik 100`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 100
            behandling {
                id = 100
                fagsak = lagFagsak()
            }
            vedtakMetadata {
                vedtaksdato = Instant.now()
            }
            årsavregning {
                aar = inneværendeÅr
                beregnetAvgiftBelop = BigDecimal(2300)
                tilFaktureringBeloep = BigDecimal(2300)
                tidligereBehandlingsresultat {
                    fakturaserieReferanse = tidligereFakturaserieRef
                    behandling {
                        type = Behandlingstyper.ÅRSAVREGNING
                    }
                }
            }
            medlemskapsperiode {
                trygdeavgiftsperiode {
                    periodeFra = PERIODE_START
                    periodeTil = PERIODE_SLUTT
                    trygdeavgiftsbeløpMd = BigDecimal(100)
                    trygdesats = BigDecimal(1)
                }
            }
        }
        val behandling = behandlingsresultat.hentBehandling()
        val prosessinstans = lagProsessInstans { this.behandling = behandling }

        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { pdlService.finnFolkeregisterident(behandling.fagsak.hentBrukersAktørID()) } returns Optional.of("123456789")

        val fakturaDtoSlot = slot<FakturaDto>()
        every {
            faktureringskomponentenClient.lagFaktura(
                capture(fakturaDtoSlot),
                SAKSBEHANDLER
            )
        } returns NyFakturaserieResponseDto(fakturaserieRef)

        val behandlingsresultatSlot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(behandlingsresultatSlot)) } returns behandlingsresultat

        sendFakturaÅrsavregning.utfør(prosessinstans)

        fakturaDtoSlot.captured.run {
            this.fakturaserieReferanse shouldBe tidligereFakturaserieRef
            startDato shouldBe PERIODE_START
            sluttDato shouldBe PERIODE_SLUTT
            beskrivelse shouldBe """Periode 01.02.$inneværendeÅr - 31.10.$inneværendeÅr, endelig beregnet trygdeavgift ${behandlingsresultat.hentÅrsavregning().beregnetAvgiftBelop} - """ +
                """forskuddsvis betalt trygdeavgift ${behandlingsresultat.hentÅrsavregning().tidligereFakturertBeloep ?: 0}"""
        }

        behandlingsresultatSlot.captured.run {
            this.fakturaserieReferanse shouldBe fakturaserieRef
        }
    }

    @Test
    fun `sender faktura med helseutgiftDekkesPeriode når belop er større eller lik 100`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 100
            behandling {
                id = 100
                tema = Behandlingstema.PENSJONIST
                fagsak {
                    saksnummer = SAKSNUMMER
                    gsakSaksnummer = 123L
                    type = Sakstyper.EU_EOS
                    tema = Sakstemaer.TRYGDEAVGIFT
                    status = Saksstatuser.OPPRETTET
                    medBruker()
                }
            }
            vedtakMetadata {
                vedtaksdato = Instant.now()
            }
            årsavregning {
                aar = inneværendeÅr
                beregnetAvgiftBelop = BigDecimal(2300)
                tilFaktureringBeloep = BigDecimal(2300)
                tidligereBehandlingsresultat {
                    fakturaserieReferanse = tidligereFakturaserieRef
                    behandling {
                        type = Behandlingstyper.ÅRSAVREGNING
                    }
                }
            }
            helseutgiftDekkesPeriode {
                fomDato = PERIODE_START
                tomDato = PERIODE_SLUTT
                trygdeavgiftsperiode {
                    periodeFra = PERIODE_START
                    periodeTil = PERIODE_SLUTT
                    trygdeavgiftsbeløpMd = BigDecimal(100)
                    trygdesats = BigDecimal(1)
                }
            }
        }
        val behandling = behandlingsresultat.hentBehandling()
        val prosessinstans = lagProsessInstans { this.behandling = behandling }

        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { pdlService.finnFolkeregisterident(behandling.fagsak.hentBrukersAktørID()) } returns Optional.of("123456789")

        val fakturaDtoSlot = slot<FakturaDto>()
        every {
            faktureringskomponentenClient.lagFaktura(
                capture(fakturaDtoSlot),
                SAKSBEHANDLER
            )
        } returns NyFakturaserieResponseDto(fakturaserieRef)

        val behandlingsresultatSlot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(behandlingsresultatSlot)) } returns behandlingsresultat

        sendFakturaÅrsavregning.utfør(prosessinstans)

        fakturaDtoSlot.captured.run {
            this.fakturaserieReferanse shouldBe tidligereFakturaserieRef
            startDato shouldBe PERIODE_START
            sluttDato shouldBe PERIODE_SLUTT
            beskrivelse shouldBe """Periode 01.02.$inneværendeÅr - 31.10.$inneværendeÅr, endelig beregnet trygdeavgift ${behandlingsresultat.hentÅrsavregning().beregnetAvgiftBelop} - """ +
                """forskuddsvis betalt trygdeavgift ${behandlingsresultat.hentÅrsavregning().tidligereFakturertBeloep ?: 0}"""
        }

        behandlingsresultatSlot.captured.run {
            this.fakturaserieReferanse shouldBe fakturaserieRef
        }
    }

    @Test
    fun `sender faktura - året er fjernet av ny vurdering, perioden hentes fra siste årsavregning for året`() {
        // Førstegang 01.12.2025–31.12.2026, årsavregning 2025 med manuelt beløp, ny vurdering avslår desember 2025
        val sisteÅrsavregning = Behandlingsresultat.forTest {
            årsavregning { aar = 2025 }
            medlemskapsperiode {
                fom = LocalDate.of(2025, 12, 1)
                tom = LocalDate.of(2025, 12, 31)
            }
        }
        val behandlingsresultat = lagÅrsavregning(2025, årsavregningInit = {
            tidligereBehandlingsresultat {
                behandling { type = Behandlingstyper.NY_VURDERING }
                medlemskapsperiode {
                    fom = LocalDate.of(2026, 1, 1)
                    tom = LocalDate.of(2026, 12, 31)
                    trygdeavgiftsperiode {
                        periodeFra = LocalDate.of(2026, 1, 1)
                        periodeTil = LocalDate.of(2026, 12, 31)
                        trygdeavgiftsbeløpMd = BigDecimal(3540)
                        trygdesats = BigDecimal(1)
                    }
                }
            }
        })
        every {
            årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(SAKSNUMMER, 2025, behandlingsresultat.vedtakMetadata?.vedtaksdato)
        } returns GjeldendeBehandlingsresultaterForÅrsavregning(sisteÅrsavregning = sisteÅrsavregning)

        sendOgHentFaktura(behandlingsresultat).run {
            startDato shouldBe LocalDate.of(2025, 12, 1)
            sluttDato shouldBe LocalDate.of(2025, 12, 31)
        }
    }

    @Test
    fun `sender faktura - uten egne perioder hentes perioden for året fra siste behandling med trygdeavgift`() {
        val sisteBehandlingsresultatMedAvgift = Behandlingsresultat.forTest {
            medlemskapsperiode {
                fom = LocalDate.of(2024, 7, 1)
                tom = LocalDate.of(2025, 12, 31)
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.of(2024, 7, 1)
                    periodeTil = LocalDate.of(2024, 12, 31)
                    trygdeavgiftsbeløpMd = BigDecimal(3400)
                    trygdesats = BigDecimal(1)
                }
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.of(2025, 1, 1)
                    periodeTil = LocalDate.of(2025, 12, 31)
                    trygdeavgiftsbeløpMd = BigDecimal(3520)
                    trygdesats = BigDecimal(1)
                }
            }
        }
        val behandlingsresultat = lagÅrsavregning(2025, årsavregningInit = {
            tidligereBehandlingsresultat = sisteBehandlingsresultatMedAvgift
        })
        every {
            årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(SAKSNUMMER, 2025, any())
        } returns GjeldendeBehandlingsresultaterForÅrsavregning(sisteBehandlingsresultatMedAvgift = sisteBehandlingsresultatMedAvgift)

        sendOgHentFaktura(behandlingsresultat).run {
            startDato shouldBe LocalDate.of(2025, 1, 1)
            sluttDato shouldBe LocalDate.of(2025, 12, 31)
        }
    }

    @Test
    fun `sender faktura - manuelt beløp for et år som ikke ble avgiftsberegnet i førstegang bruker medlemskapsperioden`() {
        // Førstegang 01.07.2025–31.12.2026 beregnet bare 2026. Årsavregningen for 2025 har manuelt beløp, så ingen
        // behandling har trygdeavgiftsperioder for 2025.
        val behandlingsresultat = lagÅrsavregning(2025) {
            medlemskapsperiode {
                fom = LocalDate.of(2025, 7, 1)
                tom = LocalDate.of(2025, 12, 31)
            }
        }

        sendOgHentFaktura(behandlingsresultat).run {
            startDato shouldBe LocalDate.of(2025, 7, 1)
            sluttDato shouldBe LocalDate.of(2025, 12, 31)
        }
    }

    @Test
    fun `sender faktura - året er fjernet og forrige årsavregning hadde manuelt beløp uten trygdeavgiftsperioder`() {
        // Som over, deretter ny vurdering som fjerner 2025. Bare forrige årsavregning har perioden for 2025.
        val sisteÅrsavregning = Behandlingsresultat.forTest {
            årsavregning {
                aar = 2025
                manueltAvgiftBeloep = BigDecimal(5000)
            }
            medlemskapsperiode {
                fom = LocalDate.of(2025, 7, 1)
                tom = LocalDate.of(2025, 12, 31)
            }
        }
        val behandlingsresultat = lagÅrsavregning(2025)
        every {
            årsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning(SAKSNUMMER, 2025, any())
        } returns GjeldendeBehandlingsresultaterForÅrsavregning(sisteÅrsavregning = sisteÅrsavregning)

        sendOgHentFaktura(behandlingsresultat).run {
            startDato shouldBe LocalDate.of(2025, 7, 1)
            sluttDato shouldBe LocalDate.of(2025, 12, 31)
        }
    }

    @Test
    fun `sender faktura - flere trygdeavgiftsperioder i året gir første fra-dato og siste til-dato`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 100
            behandling {
                id = 100
                fagsak = lagFagsak()
            }
            vedtakMetadata { vedtaksdato = Instant.now() }
            årsavregning {
                aar = 2026
                beregnetAvgiftBelop = BigDecimal(42480)
                tilFaktureringBeloep = BigDecimal(1000)
            }
            medlemskapsperiode {
                fom = LocalDate.of(2026, 1, 1)
                tom = LocalDate.of(2026, 12, 31)
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.of(2026, 1, 1)
                    periodeTil = LocalDate.of(2026, 9, 29)
                    trygdeavgiftsbeløpMd = BigDecimal(3540)
                    trygdesats = BigDecimal(1)
                }
                trygdeavgiftsperiode {
                    periodeFra = LocalDate.of(2026, 9, 30)
                    periodeTil = LocalDate.of(2026, 12, 31)
                    trygdeavgiftsbeløpMd = BigDecimal(3540)
                    trygdesats = BigDecimal(1)
                }
            }
        }

        sendOgHentFaktura(behandlingsresultat).run {
            startDato shouldBe LocalDate.of(2026, 1, 1)
            sluttDato shouldBe LocalDate.of(2026, 12, 31)
        }
    }

    @Test
    fun `sender faktura - finnes ikke trygdeavgiftsperioder så dato settes fra 0101 i året til 3112 i året `() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 100
            behandling {
                id = 100
                fagsak = lagFagsak()
            }
            vedtakMetadata {
                vedtaksdato = Instant.now()
            }
            årsavregning {
                aar = 2023
                manueltAvgiftBeloep = BigDecimal(2300)
                tilFaktureringBeloep = BigDecimal(2300)
                tidligereBehandlingsresultat {
                    fakturaserieReferanse = tidligereFakturaserieRef
                    behandling {
                        type = Behandlingstyper.ÅRSAVREGNING
                    }
                }
            }
        }
        val behandling = behandlingsresultat.hentBehandling()
        val prosessinstans = lagProsessInstans { this.behandling = behandling }

        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { pdlService.finnFolkeregisterident(behandling.fagsak.hentBrukersAktørID()) } returns Optional.of("123456789")

        val fakturaDtoSlot = slot<FakturaDto>()
        every {
            faktureringskomponentenClient.lagFaktura(
                capture(fakturaDtoSlot),
                SAKSBEHANDLER
            )
        } returns NyFakturaserieResponseDto(fakturaserieRef)

        val behandlingsresultatSlot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(behandlingsresultatSlot)) } returns behandlingsresultat

        sendFakturaÅrsavregning.utfør(prosessinstans)

        fakturaDtoSlot.captured.run {
            this.fakturaserieReferanse shouldBe tidligereFakturaserieRef
            startDato shouldBe LocalDate.of(behandlingsresultat.hentÅrsavregning().aar, 1, 1)
            sluttDato shouldBe LocalDate.of(behandlingsresultat.hentÅrsavregning().aar, 12, 31)
            beskrivelse shouldBe "Årsavregning 2023"
        }
    }

    /** Vedtatt årsavregning med manuelt beløp og ingen egne perioder, slik den ser ut når året er fjernet. */
    private fun lagÅrsavregning(
        år: Int,
        årsavregningInit: ÅrsavregningTestFactory.Builder.() -> Unit = {},
        init: BehandlingsresultatTestFactory.Builder.() -> Unit = {},
    ): Behandlingsresultat = Behandlingsresultat.forTest {
        id = 100
        behandling {
            id = 100
            fagsak = lagFagsak()
        }
        vedtakMetadata { vedtaksdato = Instant.now() }
        årsavregning {
            aar = år
            manueltAvgiftBeloep = BigDecimal.ZERO
            tilFaktureringBeloep = BigDecimal(-1500)
            årsavregningInit()
        }
        init()
    }

    private fun sendOgHentFaktura(behandlingsresultat: Behandlingsresultat): FakturaDto {
        val behandling = behandlingsresultat.hentBehandling()
        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { pdlService.finnFolkeregisterident(behandling.fagsak.hentBrukersAktørID()) } returns Optional.of("123456789")
        val fakturaDtoSlot = slot<FakturaDto>()
        every { faktureringskomponentenClient.lagFaktura(capture(fakturaDtoSlot), SAKSBEHANDLER) } returns NyFakturaserieResponseDto(fakturaserieRef)
        every { behandlingsresultatService.lagre(any()) } returns behandlingsresultat

        sendFakturaÅrsavregning.utfør(lagProsessInstans { this.behandling = behandling })

        return fakturaDtoSlot.captured
    }

    private fun lagProsessInstans(init: ProsessinstansTestFactory.ProsessinstansTestBuilder.() -> Unit = {}): Prosessinstans = Prosessinstans.forTest {
        medData(ProsessDataKey.SAKSBEHANDLER, SAKSBEHANDLER)
        init()
    }

    private fun lagFagsak(): Fagsak = Fagsak.forTest {
        saksnummer = SAKSNUMMER
        gsakSaksnummer = 123L
        type = Sakstyper.FTRL
        tema = Sakstemaer.TRYGDEAVGIFT
        status = Saksstatuser.OPPRETTET
        medBruker()
    }

    companion object {
        const val SAKSNUMMER = "MEL-test"
        const val SAKSBEHANDLER = "G568493"
        const val fakturaserieRef = "GDL435389405Gf"
        const val tidligereFakturaserieRef = "763452GG"
        private val inneværendeÅr = LocalDate.now().year
        val PERIODE_START: LocalDate = LocalDate.now().withMonth(2).withDayOfMonth(1)
        val PERIODE_SLUTT: LocalDate = LocalDate.now().withMonth(10).withDayOfMonth(31)
    }
}
