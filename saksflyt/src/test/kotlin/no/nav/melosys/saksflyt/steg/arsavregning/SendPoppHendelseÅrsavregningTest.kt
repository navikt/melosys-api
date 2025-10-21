package no.nav.melosys.saksflyt.steg.arsavregning

import io.getunleash.FakeUnleash
import io.kotest.matchers.shouldBe
import io.mockk.*
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.Årsavregning
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.behandling
import no.nav.melosys.domain.årsavregning
import no.nav.melosys.domain.vedtakMetadata
import no.nav.melosys.domain.medlemskapsperiode
import no.nav.melosys.integrasjon.hendelser.KafkaPensjonsopptjeningHendelseProducer
import no.nav.melosys.integrasjon.hendelser.PensjonsopptjeningHendelse
import no.nav.melosys.integrasjon.hendelser.PensjonsopptjeningHendelse.*
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling as prosessinstansBehandling
import no.nav.melosys.saksflytapi.domain.forTest as prosessinstansForTest
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
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                fagsak {
                    type = Sakstyper.FTRL
                    medBruker()
                }
            }
            årsavregning {
                aar = 2023
                beregnetAvgiftBelop = BigDecimal("50000")
            }
            vedtakMetadata {
                vedtaksdato = Instant.parse("2024-01-15T00:00:00Z")
            }
            medlemskapsperiode {
                fom = LocalDate.of(2023, 1, 1)
                tom = LocalDate.of(2023, 12, 31)
                trygdeavgiftsperiode {
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
        }

        val prosessinstans = Prosessinstans.prosessinstansForTest {
            behandling = behandlingsresultat.hentBehandling()
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BehandlingTestFactory.BEHANDLING_ID) } returns behandlingsresultat
        every { persondataService.hentFolkeregisterident(FagsakTestFactory.BRUKER_AKTØR_ID) } returns FagsakTestFactory.BRUKER_AKTØR_ID
        every { årsavregningService.finnÅrsavregningerPåFagsak(FagsakTestFactory.SAKSNUMMER, 2023, null) } returns emptyList()

        val capturedEvent = slot<PensjonsopptjeningHendelse>()
        every { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(capture(capturedEvent)) } just Runs


        sendPoppHendelseÅrsavregning.utfør(prosessinstans)


        verify { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(any()) }


        with(capturedEvent.captured) {
            hendelsesId shouldBe PensjonsopptjeningHendelse.genererHendelsesId(BehandlingTestFactory.BEHANDLING_ID, 2023)
            fnr shouldBe FagsakTestFactory.BRUKER_AKTØR_ID
            pgi shouldBe 50000L
            inntektsAr shouldBe 2023
            endringstype shouldBe Endringstype.NY_INNTEKT
            melosysBehandlingID shouldBe BehandlingTestFactory.BEHANDLING_ID.toString()
        }
    }

    @Test
    fun `utfør sender ikke hendelse for ikke-FTRL sak`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                fagsak {
                    type = Sakstyper.EU_EOS  // Ikke FTRL
                    medBruker()
                }
            }
            årsavregning {
                aar = 2023
            }
        }

        val prosessinstans = Prosessinstans.prosessinstansForTest {
            behandling = behandlingsresultat.hentBehandling()
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BehandlingTestFactory.BEHANDLING_ID) } returns behandlingsresultat


        sendPoppHendelseÅrsavregning.utfør(prosessinstans)


        verify(exactly = 0) { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(any()) }
    }

    @Test
    fun `utfør bestemmer OPPDATERING rapporttype når tidligere årsavregning eksisterer`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                fagsak {
                    type = Sakstyper.FTRL
                    medBruker()
                }
            }
            årsavregning {
                aar = 2023
                beregnetAvgiftBelop = BigDecimal("60000")
            }
            vedtakMetadata {
                vedtaksdato = Instant.parse("2024-01-15T00:00:00Z")
            }
            medlemskapsperiode {
                fom = LocalDate.of(2023, 1, 1)
                tom = LocalDate.of(2023, 12, 31)
                trygdeavgiftsperiode {
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
        }

        val previousÅrsavregning = Årsavregning.forTest {
            id = 100L  // Annen ID
            aar = 2023  // Samme år
        }

        val prosessinstans = Prosessinstans.prosessinstansForTest {
            behandling = behandlingsresultat.hentBehandling()
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BehandlingTestFactory.BEHANDLING_ID) } returns behandlingsresultat
        every { persondataService.hentFolkeregisterident(FagsakTestFactory.BRUKER_AKTØR_ID) } returns FagsakTestFactory.BRUKER_AKTØR_ID
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
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
                fagsak {
                    type = Sakstyper.FTRL
                    medBruker()
                }
            }
            årsavregning {
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
                trygdeavgiftsperiode {
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                    }
                }
            }
        }

        val prosessinstans = Prosessinstans.prosessinstansForTest {
            behandling = behandlingsresultat.hentBehandling()
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BehandlingTestFactory.BEHANDLING_ID) } returns behandlingsresultat
        every { persondataService.hentFolkeregisterident(FagsakTestFactory.BRUKER_AKTØR_ID) } returns FagsakTestFactory.BRUKER_AKTØR_ID
        every { årsavregningService.finnÅrsavregningerPåFagsak(FagsakTestFactory.SAKSNUMMER, 2023, null) } returns emptyList()

        val capturedEvent = slot<PensjonsopptjeningHendelse>()
        every { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(capture(capturedEvent)) } just Runs


        sendPoppHendelseÅrsavregning.utfør(prosessinstans)


        val hendelse = capturedEvent.captured
        hendelse.pgi shouldBe 75000L  // Skal bruke manuelt beløp
    }

    @Test
    fun `utfør sender ikke hendelse når feature toggle er deaktivert`() {
        val prosessinstans = Prosessinstans.prosessinstansForTest {
            prosessinstansBehandling { }
        }

        // Deaktiver alle toggles
        fakeUnleash.disableAll()


        sendPoppHendelseÅrsavregning.utfør(prosessinstans)


        verify(exactly = 0) { behandlingsresultatService.hentBehandlingsresultat(any()) }
        verify(exactly = 0) { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(any()) }
    }

    @Test
    fun `utfør sender ikke hendelse når bruker er skattepliktig til Norge`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
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
                trygdeavgiftsperiode {
                    grunnlagSkatteforholdTilNorge {
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    }
                }
            }
        }

        val prosessinstans = Prosessinstans.prosessinstansForTest {
            behandling = behandlingsresultat.hentBehandling()
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BehandlingTestFactory.BEHANDLING_ID) } returns behandlingsresultat


        sendPoppHendelseÅrsavregning.utfør(prosessinstans)


        verify(exactly = 0) { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(any()) }
    }

    @Test
    fun `utfør sender ikke hendelse når vi ikke har trygdeavgiftsperioder`() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling {
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

        val prosessinstans = Prosessinstans.prosessinstansForTest {
            behandling = behandlingsresultat.hentBehandling()
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BehandlingTestFactory.BEHANDLING_ID) } returns behandlingsresultat


        sendPoppHendelseÅrsavregning.utfør(prosessinstans)


        verify(exactly = 0) { kafkaPensjonsopptjeningHendelseProducer.sendPensjonsopptjeningHendelse(any()) }
    }
}
