package no.nav.melosys.service.avgift

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.FagsakTestFactory.BRUKER_AKTØR_ID
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import no.nav.melosys.integrasjon.trygdeavgift.dto.MedlemskapsperiodeDto.Companion.idToUUID
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import no.nav.melosys.service.saksbehandling.lagBehandlingsresultat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
internal class TrygdeavgiftsberegningServiceTest {

    @MockK
    private lateinit var mockBehandlingService: BehandlingService

    @MockK
    private lateinit var mockEregFasade: EregFasade

    @MockK
    private lateinit var mockTrygdeavgiftConsumer: TrygdeavgiftConsumer

    @MockK
    lateinit var mockBehandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var mockPersondataService: PersondataService

    private lateinit var trygdeavgiftMottakerService: TrygdeavgiftMottakerService

    private lateinit var trygdeavgiftsberegningService: TrygdeavgiftsberegningService

    private lateinit var behandling: Behandling
    private lateinit var behandlingsresultat: Behandlingsresultat

    private val FOM: LocalDate = LocalDate.now()
    private val TOM: LocalDate = LocalDate.now().plusMonths(2)
    private val BEHANDLING_ID: Long = 1L
    private val FULLMEKTIG_AKTØR_ID: String = "123456789"
    private val FULLMEKTIG_NAVN: String = "Herr Fullmektig"
    private val FULLMEKTIG_ORGNR: String = "888888888"
    private val FULLMEKTIG_ORG_NAVN: String = "Aksjeselskap AS"
    private val BRUKER_NAVN: String = "Bruker Etternavn"
    private val FØDSELSDATO: LocalDate = LocalDate.of(2020, 1, 1)


    @BeforeEach
    fun setup() {
        trygdeavgiftMottakerService = TrygdeavgiftMottakerService(mockBehandlingsresultatService)
        trygdeavgiftsberegningService = TrygdeavgiftsberegningService(
            mockBehandlingService,
            mockEregFasade,
            mockBehandlingsresultatService,
            trygdeavgiftMottakerService,
            mockPersondataService,
            mockTrygdeavgiftConsumer,
        )
        behandlingsresultat = lagBehandlingsresultat(Behandlingsresultattyper.IKKE_FASTSATT).get()

        behandling = Behandling()
        behandlingsresultat.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 1L
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.PLIKTIG
        })
        every { mockEregFasade.hentOrganisasjonNavn(FULLMEKTIG_ORGNR) }.returns(FULLMEKTIG_ORG_NAVN)
        every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
        every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandling)
        every { mockPersondataService.hentSammensattNavn(FULLMEKTIG_AKTØR_ID) }.returns(FULLMEKTIG_NAVN)
        every { mockPersondataService.hentSammensattNavn(BRUKER_AKTØR_ID) }.returns(BRUKER_NAVN)
        every { mockPersondataService.hentPerson(BRUKER_AKTØR_ID).fødselsdato }.returns(FØDSELSDATO)
    }


    @AfterEach
    fun `Remove RandomNumberGenerator mockks`() {
        unmockkStatic(UUID::class)
    }

    @Test
    fun hentTrygdeavgiftsberegning_ingenTrygdeavgift_returnerTomListe() {
        behandlingsresultat.clearTrygdeavgiftsperioder()

        trygdeavgiftsberegningService.hentTrygdeavgiftsberegning(BEHANDLING_ID).shouldNotBeNull().shouldBeEmpty()
    }

    @Test
    fun beregnTrygdeavgift_skalBetaleTrygeavgiftFrivilligMedlem_beregnerOgLagrerTrygdeavgift() {
        behandling.apply {
            fagsak = FagsakTestFactory.builder().medBruker().build()
        }
        behandlingsresultat.medlemskapsperioder = listOf(Medlemskapsperiode().apply {
            id = 1L
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
        })
        val notSoRandomUuid = UUID.randomUUID()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns notSoRandomUuid

        val inntektsperioder = listOf(
            InntektsperiodeDto(
                id = UUID.randomUUID(),
                DatoPeriodeDto(FOM, TOM),
                Inntektskildetype.INNTEKT_FRA_UTLANDET,
                arbeidsgiverBetalerAvgift = false,
                PengerDto(BigDecimal(10000.0)),
                true
            )
        )

        val skatteforholdsperioder = listOf(
            SkatteforholdsperiodeDto(
                id = UUID.randomUUID(),
                DatoPeriodeDto(FOM, TOM),
                Skatteplikttype.SKATTEPLIKTIG
            )
        )

        every { mockBehandlingsresultatService.lagre(any()) }.returns(behandlingsresultat)
        every { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }.returns(
            listOf(
                TrygdeavgiftsberegningResponse(
                    TrygdeavgiftsperiodeDto(
                        DatoPeriodeDto(FOM, TOM), BigDecimal.valueOf(7.9), PengerDto(BigDecimal.valueOf(790), NOK)
                    ), TrygdeavgiftsgrunnlagDto(
                        behandlingsresultat.medlemskapsperioder.first().idToUUID(),
                        skatteforholdsperioder.first().id,
                        notSoRandomUuid
                    )
                )
            )
        )
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)


        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
            .shouldNotBeNull()
            .shouldNotBeEmpty()


        verify { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }
        verify { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }
        verify(exactly = 0) { mockPersondataService.hentPerson(BRUKER_AKTØR_ID) }
        behandlingsresultat.trygdeavgiftsperioder.shouldNotBeEmpty()
    }


    @Test
    fun beregnTrygdeavgift_inntekstperioderDekkerIkkeInnvilgedeMedlemskapsperioder_kasterFeil() {
        behandlingsresultat.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 1L
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
        })
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

        val skatteforholdsperioder = listOf(
            SkatteforholdsperiodeDto(
                id = UUID.randomUUID(),
                DatoPeriodeDto(FOM, TOM),
                Skatteplikttype.SKATTEPLIKTIG
            )
        )

        val inntektsperioder = listOf(
            InntektsperiodeDto(
                id = UUID.randomUUID(),
                DatoPeriodeDto(FOM, TOM.minusMonths(1)),
                Inntektskildetype.INNTEKT_FRA_UTLANDET,
                arbeidsgiverBetalerAvgift = false,
                PengerDto(BigDecimal(10000.0)),
                true
            )
        )


        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
        }.message.shouldContain("Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

    @Test
    fun beregnTrygdeavgift_skatteforholdTilNorgeDekkerIkkeInnvilgedeMedlemskapsperioder_kasterFeil() {
        behandlingsresultat.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 1L
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
        })
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

        val skatteforholdsperioder = listOf(
            SkatteforholdsperiodeDto(
                id = UUID.randomUUID(),
                DatoPeriodeDto(FOM, TOM.minusMonths(1)),
                Skatteplikttype.SKATTEPLIKTIG
            )
        )

        val inntektsperioder = listOf(
            InntektsperiodeDto(
                id = UUID.randomUUID(),
                DatoPeriodeDto(FOM, TOM.minusMonths(1)),
                Inntektskildetype.INNTEKT_FRA_UTLANDET,
                arbeidsgiverBetalerAvgift = false,
                PengerDto(BigDecimal(10000.0)),
                true
            )
        )


        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
        }.message.shouldContain("Skatteforholdsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

    @Test
    fun beregnTrygdeavgift_skalBetaleTrygeavgiftPliktigMedlem_beregnerOgLagrerTrygdeavgift() {
        behandling.apply {
            fagsak = FagsakTestFactory.builder().medBruker().build()
        }
        behandlingsresultat.apply {
            Medlemskapsperiode().apply {
                id = 1L
                fom = FOM
                tom = TOM
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.PLIKTIG
            }
        }

        val skatteforholdsperioder = listOf(
            SkatteforholdsperiodeDto(
                id = UUID.randomUUID(),
                DatoPeriodeDto(FOM, TOM),
                Skatteplikttype.IKKE_SKATTEPLIKTIG
            )
        )

        val inntektsperioder = listOf(
            InntektsperiodeDto(
                id = UUID.randomUUID(),
                DatoPeriodeDto(FOM, TOM),
                Inntektskildetype.ARBEIDSINNTEKT,
                arbeidsgiverBetalerAvgift = false,
                PengerDto(BigDecimal(10000.0)),
                true
            )
        )

        val notSoRandomUuid = UUID.randomUUID()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns notSoRandomUuid

        every { mockBehandlingsresultatService.lagre(any()) }.returns(behandlingsresultat)
        every { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }.returns(
            listOf(
                TrygdeavgiftsberegningResponse(
                    TrygdeavgiftsperiodeDto(
                        DatoPeriodeDto(FOM, TOM), BigDecimal.valueOf(7.9), PengerDto(BigDecimal.valueOf(790), NOK)
                    ), TrygdeavgiftsgrunnlagDto(
                        behandlingsresultat.medlemskapsperioder.first().idToUUID(),
                        skatteforholdsperioder.first().id,
                        notSoRandomUuid
                    )
                )
            )
        )
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)


        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
            .shouldNotBeNull().shouldNotBeEmpty()


        verify { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }
        verify { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }
        verify(exactly = 1) { mockPersondataService.hentPerson(BRUKER_AKTØR_ID) }
        behandlingsresultat.trygdeavgiftsperioder.shouldNotBeEmpty()
    }

    @Test
    fun `beregnTrygdeavgift for pliktig medlem og skattepliktig skal beregne og lagre trygdeavgift`() {
        behandling.apply {
            fagsak = FagsakTestFactory.builder().medBruker().build()
        }
        behandlingsresultat.apply {
            Medlemskapsperiode().apply {
                id = 1L
                fom = FOM
                tom = TOM
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.PLIKTIG
            }
        }

        val skatteforholdsperioder = listOf(
            SkatteforholdsperiodeDto(
                id = UUID.randomUUID(),
                DatoPeriodeDto(FOM, TOM),
                Skatteplikttype.SKATTEPLIKTIG
            )
        )
        val notSoRandomUuid = UUID.randomUUID()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns notSoRandomUuid

        every { mockBehandlingsresultatService.lagre(any()) }.returns(behandlingsresultat)
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)


        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, emptyList())
            .shouldNotBeNull().shouldNotBeEmpty()

        verify { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }
        verify(exactly = 0) { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }
        verify(exactly = 0) { mockPersondataService.hentPerson(BRUKER_AKTØR_ID) }
        behandlingsresultat.trygdeavgiftsperioder.shouldHaveSize(1)
        behandlingsresultat.trygdeavgiftsperioder.first().apply {
            periodeFra.shouldBe(FOM)
            periodeTil.shouldBe(TOM)
            trygdesats.shouldBe(BigDecimal.ZERO)
            trygdeavgiftsbeløpMd.shouldBe(Penger(BigDecimal.ZERO))
            grunnlagSkatteforholdTilNorge.apply {
                fomDato.shouldBe(FOM)
                tomDato.shouldBe(TOM)
                skatteplikttype.shouldBe(Skatteplikttype.SKATTEPLIKTIG)
            }
            grunnlagInntekstperiode.shouldBeNull()
            grunnlagMedlemskapsperiode.shouldBe(behandlingsresultat.medlemskapsperioder.first())
        }
    }

    @Test
    fun `beregnTrygdeavgift for pliktig medlem og skattepliktig skal beregne og lagre flere trygdeavgift når det er flere medlemskapsperioder`() {
        behandling.apply {
            fagsak = FagsakTestFactory.builder().medBruker().build()
        }
        behandlingsresultat.apply {
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                id = 1L
                fom = LocalDate.of(2021, 1, 1)
                tom = LocalDate.of(2021, 2, 1)
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.PLIKTIG
            }, Medlemskapsperiode().apply {
                id = 2L
                fom = LocalDate.of(2021, 2, 2)
                tom = LocalDate.of(2021, 2, 28)
                trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.PLIKTIG
            })
        }

        val skatteforholdsperioder = listOf(
            SkatteforholdsperiodeDto(
                id = UUID.randomUUID(),
                DatoPeriodeDto(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 28)),
                Skatteplikttype.SKATTEPLIKTIG
            )
        )
        val notSoRandomUuid = UUID.randomUUID()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns notSoRandomUuid

        every { mockBehandlingsresultatService.lagre(any()) }.returns(behandlingsresultat)
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)


        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, emptyList())
            .shouldNotBeNull().shouldNotBeEmpty()


        verify { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }
        verify(exactly = 0) { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }
        verify(exactly = 0) { mockPersondataService.hentPerson(BRUKER_AKTØR_ID) }
        behandlingsresultat.trygdeavgiftsperioder.shouldHaveSize(2)
        behandlingsresultat.trygdeavgiftsperioder.sortedBy { it.periodeFra }.first().apply {
            periodeFra.shouldBe(LocalDate.of(2021, 1, 1))
            periodeTil.shouldBe(LocalDate.of(2021, 2, 1))
            trygdesats.shouldBe(BigDecimal.ZERO)
            trygdeavgiftsbeløpMd.shouldBe(Penger(BigDecimal.ZERO))
            grunnlagSkatteforholdTilNorge.apply {
                fomDato.shouldBe(LocalDate.of(2021, 1, 1))
                tomDato.shouldBe(LocalDate.of(2021, 2, 28))
                skatteplikttype.shouldBe(Skatteplikttype.SKATTEPLIKTIG)
            }
            grunnlagInntekstperiode.shouldBeNull()
            grunnlagMedlemskapsperiode.shouldBe(behandlingsresultat.medlemskapsperioder.first())
        }
        behandlingsresultat.trygdeavgiftsperioder.sortedBy { it.periodeFra }.last().apply {
            periodeFra.shouldBe(LocalDate.of(2021, 2, 2))
            periodeTil.shouldBe(LocalDate.of(2021, 2, 28))
            trygdesats.shouldBe(BigDecimal.ZERO)
            trygdeavgiftsbeløpMd.shouldBe(Penger(BigDecimal.ZERO))
            grunnlagSkatteforholdTilNorge.apply {
                fomDato.shouldBe(LocalDate.of(2021, 1, 1))
                tomDato.shouldBe(LocalDate.of(2021, 2, 28))
                skatteplikttype.shouldBe(Skatteplikttype.SKATTEPLIKTIG)
            }
            grunnlagInntekstperiode.shouldBeNull()
            grunnlagMedlemskapsperiode.shouldBe(behandlingsresultat.medlemskapsperioder.last())
        }
    }

    @Test
    fun beregnTrygdeavgift_skalIkkeBetaleTrygdeavgiftTilNav_sletterEksisterendeTrygdeavgiftOgReturnererTrygdeavgiftsperiodeMedBelop0() {
        val inntektsKildeId = UUID.randomUUID()
        behandling.apply {
            fagsak = FagsakTestFactory.builder().medBruker().build()
        }

        behandlingsresultat.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 1L
            trygdeavgiftsperioder.add(Trygdeavgiftsperiode().apply {
                periodeFra = FOM
                periodeTil = TOM
                trygdeavgiftsbeløpMd = Penger(790.0)
                trygdesats = BigDecimal.valueOf(7.9)
            })
        })

        val skatteforholdsperioder = listOf(
            SkatteforholdsperiodeDto(
                id = UUID.randomUUID(),
                DatoPeriodeDto(FOM, TOM),
                Skatteplikttype.SKATTEPLIKTIG
            )
        )

        val inntektsperioder = listOf(
            InntektsperiodeDto(
                id = inntektsKildeId,
                DatoPeriodeDto(FOM, TOM),
                Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE,
                arbeidsgiverBetalerAvgift = true,
                PengerDto(BigDecimal(0)),
                true
            )
        )
        every { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }.returns(
            listOf(
                TrygdeavgiftsberegningResponse(
                    TrygdeavgiftsperiodeDto(
                        DatoPeriodeDto(FOM, TOM), BigDecimal.valueOf(0), PengerDto(BigDecimal.valueOf(0.0), NOK)
                    ), TrygdeavgiftsgrunnlagDto(
                        behandlingsresultat.medlemskapsperioder.first().idToUUID(),
                        skatteforholdsperioder.first().id,
                        inntektsKildeId
                    )
                )
            )
        )
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)


        val trygdeavgiftsperioder =
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)


        trygdeavgiftsperioder.shouldNotBeNull()
        trygdeavgiftsperioder.shouldNotBeEmpty()
        trygdeavgiftsperioder.shouldHaveSize(1)
        trygdeavgiftsperioder.forEach {
            it.trygdesats.shouldBe(BigDecimal.ZERO)
            it.trygdeavgiftsbeløpMd.shouldBe(Penger(0.0))
        }
    }

    @Test
    fun `beregnTrygdeavgift feiler fordi alle skatteforholdsperioder har samme skatteplikttype`() {
        behandling.apply {
            fagsak = FagsakTestFactory.builder().medBruker().build()
        }

        behandlingsresultat.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 2L
            trygdeavgiftsperioder.add(Trygdeavgiftsperiode().apply {
                id = 1L
                periodeFra = FOM
                periodeTil = TOM
                trygdeavgiftsbeløpMd = Penger(790.0)
                trygdesats = BigDecimal.valueOf(7.9)
            })
        })

        val skatteforholdsperioder = listOf(
            SkatteforholdsperiodeDto(
                id = UUID.randomUUID(),
                DatoPeriodeDto(FOM, TOM),
                Skatteplikttype.SKATTEPLIKTIG
            ), SkatteforholdsperiodeDto(
                UUID.randomUUID(),
                DatoPeriodeDto(FOM, TOM),
                Skatteplikttype.SKATTEPLIKTIG
            )
        )

        val inntektsperioder = listOf(
            InntektsperiodeDto(
                id = UUID.randomUUID(),
                DatoPeriodeDto(FOM, TOM),
                Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE,
                arbeidsgiverBetalerAvgift = true,
                PengerDto(BigDecimal(0)),
                true
            )
        )
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)


        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
        }.message.shouldContain("Alle skatteforholdsperiodene har samme svar på spørsmålet om skatteplikt")

    }

    @Test
    fun beregnTrygdeavgift_skalIkkeBetaleTrygdeavgiftTilNav_FeilerNarTrygdeavgiftIkkeErBeløp0() {
        behandling.apply {
            fagsak = FagsakTestFactory.builder().medBruker().build()
        }

        behandlingsresultat.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 2L
            trygdeavgiftsperioder.add(Trygdeavgiftsperiode().apply {
                id = 1L
                periodeFra = FOM
                periodeTil = TOM
                trygdeavgiftsbeløpMd = Penger(790.0)
                trygdesats = BigDecimal.valueOf(7.9)
            })
        })

        val skatteforholdsperioder = listOf(
            SkatteforholdsperiodeDto(
                id = UUID.randomUUID(),
                DatoPeriodeDto(FOM, TOM),
                Skatteplikttype.SKATTEPLIKTIG
            )
        )

        val inntektsperioder = listOf(
            InntektsperiodeDto(
                id = UUID.randomUUID(),
                DatoPeriodeDto(FOM, TOM),
                Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE,
                arbeidsgiverBetalerAvgift = true,
                PengerDto(BigDecimal(0)),
                true
            )
        )
        every { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }.returns(
            listOf(
                TrygdeavgiftsberegningResponse(
                    TrygdeavgiftsperiodeDto(
                        DatoPeriodeDto(FOM, TOM), BigDecimal.valueOf(0), PengerDto(BigDecimal.valueOf(123.0), NOK)
                    ), TrygdeavgiftsgrunnlagDto(
                        behandlingsresultat.medlemskapsperioder.first().idToUUID(),
                        skatteforholdsperioder.first().id,
                        UUID.randomUUID()
                    )
                )
            )
        )
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)


        shouldThrow<IllegalStateException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
        }.message.shouldContain("Trygdeavgift skal ikke betales til NAV. Beregnet trygdeavgift må derfor være 0.")

    }


    @Test
    fun beregnTrygdeavgift_manglerMedlemskapsperioder_kasterFeil() {
        behandlingsresultat.medlemskapsperioder = emptyList()
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

        val skatteforholdsperioder = listOf(
            SkatteforholdsperiodeDto(
                id = UUID.randomUUID(),
                DatoPeriodeDto(FOM, TOM),
                Skatteplikttype.SKATTEPLIKTIG
            )
        )

        val inntektsperioder = listOf(
            InntektsperiodeDto(
                id = UUID.randomUUID(),
                DatoPeriodeDto(FOM, TOM),
                Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE,
                arbeidsgiverBetalerAvgift = true,
                PengerDto(BigDecimal(0)),
                true
            )
        )


        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
        }.message.shouldContain("Kan ikke beregne trygdeavgift uten medlemskapsperioder")
    }

    @Test
    fun beregnTrygdeavgift_manglerSkatteforholdINorge_kasterFeil() {
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

        val inntektsperioder = listOf(
            InntektsperiodeDto(
                id = UUID.randomUUID(),
                DatoPeriodeDto(FOM, TOM),
                Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE,
                arbeidsgiverBetalerAvgift = true,
                PengerDto(BigDecimal(0)),
                true
            )
        )


        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, emptyList(), inntektsperioder)
        }.message.shouldContain("Kan ikke beregne trygdeavgift uten skatteforholdTilNorge")
    }

    @Test
    fun beregnTrygdeavgift_manglerInntektsperioder_kasterFeil() {
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

        val skatteforholdsperioder = listOf(
            SkatteforholdsperiodeDto(
                id = UUID.randomUUID(),
                DatoPeriodeDto(FOM, TOM),
                Skatteplikttype.IKKE_SKATTEPLIKTIG
            )
        )


        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, emptyList())
        }.message.shouldContain("Kan ikke beregne trygdeavgift uten inntektsperioder")
    }

    @Test
    fun beregnTrygdeavgift_manglerStartDatoPåMedlemskap_kasterFeil() {
        behandlingsresultat.medlemskapsperioder.first().fom = null
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

        val skatteforholdsperioder = listOf(
            SkatteforholdsperiodeDto(
                id = UUID.randomUUID(),
                DatoPeriodeDto(FOM, TOM),
                Skatteplikttype.IKKE_SKATTEPLIKTIG
            )
        )

        val inntektsperioder = listOf(
            InntektsperiodeDto(
                id = UUID.randomUUID(),
                DatoPeriodeDto(FOM, TOM),
                Inntektskildetype.INNTEKT_FRA_UTLANDET,
                arbeidsgiverBetalerAvgift = true,
                PengerDto(BigDecimal(0)),
                true
            )
        )


        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
        }.message.shouldContain("Klarte ikke finne startdatoen på medlemskapet")
    }


    @Test
    fun finnFakturamottaker_harIkkeFullmektig_mottakerErBruker() {
        behandling.apply {
            fagsak = FagsakTestFactory.builder().medBruker().build()
        }
        trygdeavgiftsberegningService.finnFakturamottakerNavn(BEHANDLING_ID).shouldBe(BRUKER_NAVN)
    }

    @Test
    fun finnFakturamottaker_harFullmektigPersonForTrygdeavgift_mottakerErFullmektigPerson() {
        behandling.apply {
            fagsak = FagsakTestFactory.builder().aktører(
                setOf(Aktoer().apply {
                    aktørId = BRUKER_AKTØR_ID
                    rolle = Aktoersroller.BRUKER
                }, Aktoer().apply {
                    rolle = Aktoersroller.FULLMEKTIG
                    personIdent = FULLMEKTIG_AKTØR_ID
                    fullmakter = setOf(Fullmakt().apply { type = Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT })
                })
            ).build()
        }
        trygdeavgiftsberegningService.finnFakturamottakerNavn(BEHANDLING_ID).shouldBe(FULLMEKTIG_NAVN)
    }

    @Test
    fun finnFakturamottaker_harFullmektigOrgForTrygdeavgift_mottakerErFullmektigOrg() {
        behandling.apply {
            fagsak = FagsakTestFactory.builder().aktører(
                setOf(Aktoer().apply {
                    aktørId = BRUKER_AKTØR_ID
                    rolle = Aktoersroller.BRUKER
                }, Aktoer().apply {
                    orgnr = FULLMEKTIG_ORGNR
                    rolle = Aktoersroller.FULLMEKTIG
                    fullmakter = setOf(Fullmakt().apply { type = Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT })
                })
            ).build()
        }
        trygdeavgiftsberegningService.finnFakturamottakerNavn(BEHANDLING_ID).shouldBe(FULLMEKTIG_ORG_NAVN)
    }

    @Test
    fun finnFakturamottaker_harFullmektigMenIkkeForTrygdeavgift_brukerErFullmektig() {
        behandling.apply {
            fagsak = FagsakTestFactory.builder().aktører(
                setOf(Aktoer().apply {
                    aktørId = BRUKER_AKTØR_ID
                    rolle = Aktoersroller.BRUKER
                }, Aktoer().apply {
                    aktørId = FULLMEKTIG_AKTØR_ID
                    rolle = Aktoersroller.FULLMEKTIG
                    fullmakter = setOf(Fullmakt().apply { type = Fullmaktstype.FULLMEKTIG_SØKNAD },
                        Fullmakt().apply { type = Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER })
                })
            ).build()
        }
        trygdeavgiftsberegningService.finnFakturamottakerNavn(BEHANDLING_ID).shouldBe(BRUKER_NAVN)
    }
}
