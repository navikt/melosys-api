package no.nav.melosys.saksflyt.steg.arsavregning

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.integrasjon.faktureringskomponenten.FaktureringskomponentenConsumer
import no.nav.melosys.integrasjon.faktureringskomponenten.NyFakturaserieResponseDto
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaDto
import no.nav.melosys.saksflyt.TestdataFactory.lagBruker
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class SendFakturaÅrsavregningTest {

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var pdlService: PersondataService

    @MockK
    private lateinit var faktureringskomponentenConsumer: FaktureringskomponentenConsumer

    private lateinit var sendFakturaÅrsavregning: SendFakturaÅrsavregning

    @BeforeEach
    fun setUp() {
        sendFakturaÅrsavregning = SendFakturaÅrsavregning(
            behandlingService,
            behandlingsresultatService,
            faktureringskomponentenConsumer,
            pdlService
        )
    }

    @Test
    fun `sender ikke faktura når faktureringsbelop er mindre enn 100`() {
        val behandling = Behandling.forTest {
            id = 100
        }
        val behandlingsresultat = Behandlingsresultat().apply {
            årsavregning = Årsavregning().apply {
                aar = 2023
                tilFaktureringBeloep = BigDecimal(99)
            }
        }
        val prosessinstans = lagProsessInstans {
            this.behandling = behandling
        }

        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat

        sendFakturaÅrsavregning.utfør(prosessinstans)

        verify(exactly = 0) { faktureringskomponentenConsumer.lagFaktura(any(), any()) }
    }

    @Test
    fun `sender faktura når belop er større eller lik 100`() {
        val behandling = Behandling.forTest {
            id = 100
            fagsak = lagFagsak()
        }
        val behandlingsresultat = Behandlingsresultat().apply {
            id = 100
            this.behandling = behandling
            vedtakMetadata = VedtakMetadata().apply {
                vedtaksdato = Instant.now()
            }
            årsavregning = Årsavregning().apply {
                aar = 2023
                beregnetAvgiftBelop = BigDecimal(2300)
                tilFaktureringBeloep = BigDecimal(2300)
                tidligereBehandlingsresultat = Behandlingsresultat().apply {
                    this.fakturaserieReferanse = tidligereFakturaserieRef
                    this.behandling = Behandling.forTest {
                        type = Behandlingstyper.ÅRSAVREGNING
                    }
                }
            }
            medlemskapsperioder = listOf(
                lagMedlemskapsPeriode {
                    trygdeavgiftsperioder = setOf(
                        lagTrygdeavgiftsperiode()
                    )
                }
            )
        }
        val prosessinstans = lagProsessInstans {
            this.behandling = behandling
        }

        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { pdlService.finnFolkeregisterident(behandling.fagsak.hentBrukersAktørID()) } returns Optional.of("123456789")

        val fakturaDtoSlot = slot<FakturaDto>()
        every {
            faktureringskomponentenConsumer.lagFaktura(
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
            beskrivelse shouldBe """Medlemskapsperiode 01.02.2025 - 31.10.2025, endelig beregnet trygdeavgift ${behandlingsresultat.årsavregning.beregnetAvgiftBelop} - """ +
                """forskuddsvis fakturert trygdeavgift ${behandlingsresultat.årsavregning.tidligereFakturertBeloep ?: 0}"""
        }

        behandlingsresultatSlot.captured.run {
            this.fakturaserieReferanse shouldBe fakturaserieRef
        }
    }

    @Test
    fun `sender faktura - dato hentes fra tidligere behandlingsgrunnlag`() {
        val behandling = Behandling.forTest {
            id = 100
            fagsak = lagFagsak()
        }
        val behandlingsresultat = Behandlingsresultat().apply {
            id = 100
            this.behandling = behandling
            vedtakMetadata = VedtakMetadata().apply {
                vedtaksdato = Instant.now()
            }
            årsavregning = Årsavregning().apply {
                aar = 2023
                tilFaktureringBeloep = BigDecimal(2300)
                tidligereBehandlingsresultat = Behandlingsresultat().apply {
                    this.fakturaserieReferanse = tidligereFakturaserieRef
                    this.behandling = Behandling.forTest {
                        type = Behandlingstyper.ÅRSAVREGNING
                    }
                    medlemskapsperioder = listOf(
                        lagMedlemskapsPeriode {
                            trygdeavgiftsperioder = setOf(
                                lagTrygdeavgiftsperiode()
                            )
                        }
                    )
                }
            }
        }
        val prosessinstans = lagProsessInstans {
            this.behandling = behandling
        }

        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { pdlService.finnFolkeregisterident(behandling.fagsak.hentBrukersAktørID()) } returns Optional.of("123456789")

        val fakturaDtoSlot = slot<FakturaDto>()
        every {
            faktureringskomponentenConsumer.lagFaktura(
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
        val behandling = Behandling.forTest {
            id = 100
            fagsak = lagFagsak()
        }
        val behandlingsresultat = Behandlingsresultat().apply {
            id = 100
            this.behandling = behandling
            vedtakMetadata = VedtakMetadata().apply {
                vedtaksdato = Instant.now()
            }
            årsavregning = Årsavregning().apply {
                aar = 2023
                manueltAvgiftBeloep = BigDecimal(2300)
                tilFaktureringBeloep = BigDecimal(2300)
                tidligereBehandlingsresultat = Behandlingsresultat().apply {
                    this.fakturaserieReferanse = tidligereFakturaserieRef
                    this.behandling = Behandling.forTest {
                        type = Behandlingstyper.ÅRSAVREGNING
                    }
                }
            }
        }
        val prosessinstans = lagProsessInstans {
            this.behandling = behandling
        }

        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { pdlService.finnFolkeregisterident(behandling.fagsak.hentBrukersAktørID()) } returns Optional.of("123456789")

        val fakturaDtoSlot = slot<FakturaDto>()
        every {
            faktureringskomponentenConsumer.lagFaktura(
                capture(fakturaDtoSlot),
                SAKSBEHANDLER
            )
        } returns NyFakturaserieResponseDto(fakturaserieRef)

        val behandlingsresultatSlot = slot<Behandlingsresultat>()
        every { behandlingsresultatService.lagre(capture(behandlingsresultatSlot)) } returns behandlingsresultat

        sendFakturaÅrsavregning.utfør(prosessinstans)

        fakturaDtoSlot.captured.run {
            this.fakturaserieReferanse shouldBe tidligereFakturaserieRef
            startDato shouldBe LocalDate.of(behandlingsresultat.årsavregning.aar, 1, 1)
            sluttDato shouldBe LocalDate.of(behandlingsresultat.årsavregning.aar, 12, 31)
            beskrivelse shouldBe "Årsavregning 2023"
        }
    }

    private fun lagProsessInstans(block: Prosessinstans.() -> Unit = {}): Prosessinstans = Prosessinstans().apply {
        setData(ProsessDataKey.SAKSBEHANDLER, SAKSBEHANDLER)
        block()
    }

    private fun lagFagsak(block: Fagsak.() -> Unit = {}): Fagsak = Fagsak(
        SAKSNUMMER,
        123L,
        Sakstyper.EU_EOS,
        Sakstemaer.TRYGDEAVGIFT,
        Saksstatuser.OPPRETTET,
        null,
        mutableSetOf(lagBruker()),
        mutableListOf()
    ).apply {
        block()
    }

    private fun lagMedlemskapsPeriode(block: Medlemskapsperiode.() -> Unit = {}) = Medlemskapsperiode(
    ).apply {
        block()
    }

    private fun lagTrygdeavgiftsperiode(block: Trygdeavgiftsperiode.() -> Unit = {}): Trygdeavgiftsperiode {
        return Trygdeavgiftsperiode(
            id = 1,
            periodeFra = PERIODE_START,
            periodeTil = PERIODE_SLUTT,
            trygdeavgiftsbeløpMd = Penger(BigDecimal(100), "NOK"),
            trygdesats = BigDecimal(1),
        ).apply {
            block()
        }
    }


    companion object {
        const val SAKSNUMMER = "MEL-test"
        const val SAKSBEHANDLER = "G568493"
        const val fakturaserieRef = "GDL435389405Gf"
        const val tidligereFakturaserieRef = "763452GG"
        val PERIODE_START = LocalDate.now().withMonth(2).withDayOfMonth(1)
        val PERIODE_SLUTT = LocalDate.now().withMonth(10).withDayOfMonth(31)
    }
}
