package no.nav.melosys.saksflyt.steg.arsavregning

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandlingsresultat
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
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenClient
import no.nav.melosys.integrasjon.faktureringskomponenten.NyFakturaserieResponseDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaDto
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.ProsessinstansTestFactory
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
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

    private lateinit var sendFakturaÅrsavregning: SendFakturaÅrsavregning

    @BeforeEach
    fun setUp() {
        sendFakturaÅrsavregning = SendFakturaÅrsavregning(
            behandlingService,
            behandlingsresultatService,
            faktureringskomponentenClient,
            pdlService
        )
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
                aar = 2023
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
                aar = 2023
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
    fun `sender faktura - dato hentes fra tidligere behandlingsgrunnlag`() {
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
                tilFaktureringBeloep = BigDecimal(2300)
                tidligereBehandlingsresultat {
                    fakturaserieReferanse = tidligereFakturaserieRef
                    behandling {
                        type = Behandlingstyper.ÅRSAVREGNING
                        fagsak {
                            type = Sakstyper.FTRL
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
