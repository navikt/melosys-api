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
    fun `inngangsSteg returnerer riktig steg`() {
        sendPoppHendelseÅrsavregning.inngangsSteg() shouldBe ProsessSteg.SEND_POPP_HENDELSE_AARSAVREGNING
    }

    @Test
    fun `utfør sender POPP-hendelse for FTRL årsavregning`() {
        val behandlingId = 123L
        val fnr = "12345678901"

        val behandlingsresultat = behandlingsresultatForTest {
            behandling {
                id = behandlingId
                fagsak {
                    type = Sakstyper.FTRL
                    medBruker()
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
                    lagTrygdeavgiftsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG, this)
                )
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            behandling {
                id = behandlingId
                fagsak {
                    type = Sakstyper.FTRL
                    medBruker()
                }
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns behandlingsresultat
        every { persondataService.hentFolkeregisterident(FagsakTestFactory.BRUKER_AKTØR_ID) } returns fnr
        every { årsavregningService.finnÅrsavregningerPåFagsak(FagsakTestFactory.SAKSNUMMER, 2023, null) } returns emptyList()

        val capturedEvent = slot<PensjonsopptjeningHendelse>()
        every { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(capture(capturedEvent)) } just Runs


        sendPoppHendelseÅrsavregning.utfør(prosessinstans)


        verify { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(any()) }

        val hendelse = capturedEvent.captured
        hendelse.fnr shouldBe fnr
        hendelse.pgi shouldBe 50000L
        hendelse.inntektsAr shouldBe 2023
        hendelse.endringstype shouldBe Endringstype.NY_INNTEKT
        hendelse.melosysBehandlingID shouldBe behandlingId.toString()
    }

    @Test
    fun `utfør sender ikke hendelse for ikke-FTRL sak`() {
        val behandlingId = 123L

        val behandlingsresultat = behandlingsresultatForTest {
            behandling {
                id = behandlingId
                fagsak {
                    type = Sakstyper.EU_EOS  // Ikke FTRL
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


        sendPoppHendelseÅrsavregning.utfør(prosessinstans)


        verify(exactly = 0) { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(any()) }
    }

    @Test
    fun `utfør bestemmer OPPDATERING rapporttype når tidligere årsavregning eksisterer`() {
        val behandlingId = 123L
        val fnr = "12345678901"

        val behandlingsresultat = behandlingsresultatForTest {
            behandling {
                id = behandlingId
                fagsak {
                    type = Sakstyper.FTRL
                    medBruker()
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
                    lagTrygdeavgiftsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG, this)
                )
            }
        }

        val previousÅrsavregning = Årsavregning.forTest {
            id = 100L  // Annen ID
            aar = 2023  // Samme år
        }

        val prosessinstans = Prosessinstans.forTest {
            behandling {
                id = behandlingId
                fagsak = behandlingsresultat.hentBehandling().fagsak
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns behandlingsresultat
        every { persondataService.hentFolkeregisterident(FagsakTestFactory.BRUKER_AKTØR_ID) } returns fnr
        every { årsavregningService.finnÅrsavregningerPåFagsak(FagsakTestFactory.SAKSNUMMER, 2023, null) } returns listOf(previousÅrsavregning)

        val capturedEvent = slot<PensjonsopptjeningHendelse>()
        every { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(capture(capturedEvent)) } just Runs


        sendPoppHendelseÅrsavregning.utfør(prosessinstans)


        val hendelse = capturedEvent.captured
        hendelse.endringstype shouldBe Endringstype.OPPDATERING
        hendelse.pgi shouldBe 60000L
    }

    @Test
    fun `utfør bruker manuelt beløp når tilgjengelig`() {
        val behandlingId = 123L
        val fnr = "12345678901"

        val behandlingsresultat = behandlingsresultatForTest {
            behandling {
                id = behandlingId
                fagsak {
                    type = Sakstyper.FTRL
                    medBruker()
                }
            }
            årsavregning {
                id = behandlingId
                aar = 2023
                beregnetAvgiftBelop = BigDecimal("50000")
                manueltAvgiftBeloep = BigDecimal("75000")  // Manuelt beløp satt
            }
            vedtakMetadata {
                vedtaksdato = Instant.parse("2024-01-15T00:00:00Z")
            }
            medlemskapsperiode {
                fom = LocalDate.of(2023, 1, 1)
                tom = LocalDate.of(2023, 12, 31)
                trygdeavgiftsperioder = setOf(
                    lagTrygdeavgiftsperiode(Skatteplikttype.IKKE_SKATTEPLIKTIG, this)
                )
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            behandling {
                id = behandlingId
                fagsak = behandlingsresultat.hentBehandling().fagsak
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns behandlingsresultat
        every { persondataService.hentFolkeregisterident(FagsakTestFactory.BRUKER_AKTØR_ID) } returns fnr
        every { årsavregningService.finnÅrsavregningerPåFagsak(FagsakTestFactory.SAKSNUMMER, 2023, null) } returns emptyList()

        val capturedEvent = slot<PensjonsopptjeningHendelse>()
        every { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(capture(capturedEvent)) } just Runs


        sendPoppHendelseÅrsavregning.utfør(prosessinstans)


        val hendelse = capturedEvent.captured
        hendelse.pgi shouldBe 75000L  // Skal bruke manuelt beløp
    }

    @Test
    fun `utfør sender ikke hendelse når feature toggle er deaktivert`() {
        val behandlingId = 123L

        val prosessinstans = Prosessinstans.forTest {
            behandling {
                id = behandlingId
            }
        }

        // Deaktiver alle toggles
        fakeUnleash.disableAll()


        sendPoppHendelseÅrsavregning.utfør(prosessinstans)


        verify(exactly = 0) { behandlingsresultatService.hentBehandlingsresultat(any()) }
        verify(exactly = 0) { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(any()) }
    }

    @Test
    fun `utfør sender ikke hendelse når bruker er skattepliktig til Norge`() {
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
                    lagTrygdeavgiftsperiode(Skatteplikttype.SKATTEPLIKTIG, this)
                )
            }
        }

        val prosessinstans = Prosessinstans.forTest {
            behandling {
                id = behandlingId
                fagsak = behandlingsresultat.hentBehandling().fagsak
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns behandlingsresultat


        sendPoppHendelseÅrsavregning.utfør(prosessinstans)


        verify(exactly = 0) { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(any()) }
    }

    @Test
    fun `utfør sender ikke hendelse når vi ikke har trygdeavgiftsperioder`() {
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
            // Ingen trygdeavgiftsperioder - vil føre til at utledSkatteplikttype() Skatteplikttype.SKATTEPLIKTIG
        }

        val prosessinstans = Prosessinstans.forTest {
            behandling {
                id = behandlingId
                fagsak = behandlingsresultat.hentBehandling().fagsak
            }
        }

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingId) } returns behandlingsresultat


        sendPoppHendelseÅrsavregning.utfør(prosessinstans)


        verify(exactly = 0) { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(any()) }
    }

    // Hjelpefunksjon for å opprette Trygdeavgiftsperiode med skatteplikttype
    private fun lagTrygdeavgiftsperiode(
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
