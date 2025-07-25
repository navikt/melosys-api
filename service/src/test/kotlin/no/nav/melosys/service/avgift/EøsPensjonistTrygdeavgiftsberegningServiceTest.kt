package no.nav.melosys.service.avgift

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldNotThrow
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
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.FagsakTestFactory.BRUKER_AKTØR_ID
import no.nav.melosys.domain.Fullmakt
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.dto.DatoPeriodeDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.EøsPensjonistTrygdeavgiftsberegningRequest
import no.nav.melosys.integrasjon.trygdeavgift.dto.EøsPensjonistTrygdeavgiftsberegningResponse
import no.nav.melosys.integrasjon.trygdeavgift.dto.EøsPensjonistTrygdeavgiftsgrunnlagDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.NOK
import no.nav.melosys.integrasjon.trygdeavgift.dto.PengerDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningRequest
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningResponse
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsgrunnlagDto
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsperiodeDto
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService
import no.nav.melosys.service.persondata.PersondataService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockKExtension::class)
internal class EøsPensjonistTrygdeavgiftsberegningServiceTest {

    @MockK
    private lateinit var mockBehandlingService: BehandlingService

    @MockK
    private lateinit var mockEregFasade: EregFasade

    @MockK
    private lateinit var mockTrygdeavgiftConsumer: TrygdeavgiftConsumer

    @MockK(relaxed = true)
    lateinit var mockBehandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var mockPersondataService: PersondataService

    @MockK(relaxed = true)
    private lateinit var helseutgiftDekkesPeriodeService: HelseutgiftDekkesPeriodeService


    private lateinit var trygdeavgiftperiodeErstatter: TrygdeavgiftperiodeErstatter

    private lateinit var trygdeavgiftMottakerService: TrygdeavgiftMottakerService

    private lateinit var trygdeavgiftsberegningService: EøsPensjonistTrygdeavgiftsberegningService



    private lateinit var behandling: Behandling
    private lateinit var behandlingsresultat: Behandlingsresultat
    private lateinit var helseutgiftDekkesPeriode: HelseutgiftDekkesPeriode

    private val unleash: FakeUnleash = FakeUnleash()

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
        unleash.enableAll()
        trygdeavgiftperiodeErstatter = spyk(TrygdeavgiftperiodeErstatter(mockBehandlingsresultatService))
        trygdeavgiftMottakerService = TrygdeavgiftMottakerService(mockBehandlingsresultatService)
        trygdeavgiftsberegningService = EøsPensjonistTrygdeavgiftsberegningService(
            mockBehandlingService,
            mockEregFasade,
            mockBehandlingsresultatService,
            trygdeavgiftperiodeErstatter,
            trygdeavgiftMottakerService,
            helseutgiftDekkesPeriodeService,
            mockPersondataService,
            mockTrygdeavgiftConsumer,
            unleash
        )
        behandling = Behandling().apply {
            id = 1L
            tema = Behandlingstema.PENSJONIST
        }
        behandlingsresultat = Behandlingsresultat().apply {
            id = 1L
            behandling = this@EøsPensjonistTrygdeavgiftsberegningServiceTest.behandling
            type = Behandlingsresultattyper.IKKE_FASTSATT
        }

        helseutgiftDekkesPeriode = HelseutgiftDekkesPeriode(
            behandlingsresultat = behandlingsresultat,
            fomDato = FOM,
            tomDato = TOM,
            bostedLandkode = Land_iso2.DK
        )

        every { mockEregFasade.hentOrganisasjonNavn(FULLMEKTIG_ORGNR) }.returns(FULLMEKTIG_ORG_NAVN)
        every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
        every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandling)
        every { mockPersondataService.hentSammensattNavn(FULLMEKTIG_AKTØR_ID) }.returns(FULLMEKTIG_NAVN)
        every { mockPersondataService.hentSammensattNavn(BRUKER_AKTØR_ID) }.returns(BRUKER_NAVN)
        every { mockPersondataService.hentPerson(BRUKER_AKTØR_ID).fødselsdato }.returns(FØDSELSDATO)
        every { helseutgiftDekkesPeriodeService.hentHelseutgiftDekkesPeriode(BEHANDLING_ID) }.returns(helseutgiftDekkesPeriode)

    }


    @AfterEach
    fun `Remove RandomNumberGenerator mockks`() {
        unmockkStatic(UUID::class)
    }

    @Test
    fun `beregnTrygdeavgift - Inntektsperioder dekker ikke helseutgift dekkes periode - kasterFeil`() {
        val skatteforholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = FOM
                tomDato = TOM
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        )

        val inntektsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = FOM
                tomDato = TOM.minusMonths(1)
                type = Inntektskildetype.PENSJON
                avgiftspliktigMndInntekt = Penger(BigDecimal(10000.0))
                isArbeidsgiversavgiftBetalesTilSkatt = false
            }
        )


        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
        }.message.shouldContain("Skatteforholdsperioden(e) du har lagt inn dekker ikke hele helseutgift dekkes periode")
    }

    @Test
    fun `beregnTrygdeavgift - EØS pensjonist skal betale Trygdeavgift - beregnerOgLagrerTrygdeavgift`() {
        behandling.apply {
            fagsak = FagsakTestFactory.builder().medBruker().build()
        }

        val skatteforholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = FOM
                tomDato = TOM
                skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
            }
        )

        val inntektsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = FOM
                tomDato = TOM
                type = Inntektskildetype.PENSJON
                isArbeidsgiversavgiftBetalesTilSkatt = false
                avgiftspliktigMndInntekt = Penger(BigDecimal(10000.0))
            }
        )

        val notSoRandomUuid = UUID.randomUUID()
        val datoPeriodeDto = DatoPeriodeDto(FOM,TOM)
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns notSoRandomUuid

        every { mockBehandlingsresultatService.lagre(any()) }.returns(behandlingsresultat)
        every { mockTrygdeavgiftConsumer.beregnTrygdeavgiftEosPensjonist(ofType(EøsPensjonistTrygdeavgiftsberegningRequest::class)) }.returns(
            listOf(
                EøsPensjonistTrygdeavgiftsberegningResponse(
                    TrygdeavgiftsperiodeDto(
                        DatoPeriodeDto(FOM, TOM), BigDecimal.valueOf(7.9), PengerDto(BigDecimal.valueOf(790), NOK)
                    ), EøsPensjonistTrygdeavgiftsgrunnlagDto(
                        datoPeriodeDto,
                        notSoRandomUuid,
                        notSoRandomUuid
                    )
                )
            )
        )
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
            .shouldNotBeNull().shouldNotBeEmpty()

        verify { trygdeavgiftperiodeErstatter.erstattEøsPensjonistTrygdeavgiftsperioder(BEHANDLING_ID, match { it.isNotEmpty() }) }

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
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
            }
        }

        val skatteforholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = FOM
                tomDato = TOM
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        )
        val notSoRandomUuid = UUID.randomUUID()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns notSoRandomUuid


        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, emptyList())
            .shouldNotBeNull().shouldNotBeEmpty()

        verify(exactly = 0) { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }
        verify(exactly = 0) { mockPersondataService.hentPerson(BRUKER_AKTØR_ID) }

        verify {
            trygdeavgiftperiodeErstatter.erstattTrygdeavgiftsperioder(BEHANDLING_ID, match { it.isNotEmpty() })
        }


        behandlingsresultat.trygdeavgiftsperioder.shouldHaveSize(1)
        behandlingsresultat.trygdeavgiftsperioder.first().apply {
            periodeFra.shouldBe(FOM)
            periodeTil.shouldBe(TOM)
            trygdesats.shouldBe(BigDecimal.ZERO)
            trygdeavgiftsbeløpMd.shouldBe(Penger(BigDecimal.ZERO))
            grunnlagSkatteforholdTilNorge.shouldNotBeNull().run {
                fomDato.shouldBe(FOM)
                tomDato.shouldBe(TOM)
                skatteplikttype.shouldBe(Skatteplikttype.SKATTEPLIKTIG)
            }
            grunnlagInntekstperiode.shouldBeNull()
            grunnlagMedlemskapsperiode.shouldBe(behandlingsresultat.medlemskapsperioder.first())
        }
    }

    @Test
    fun beregnTrygdeavgift_skalIkkeBetaleTrygdeavgiftTilNav_sletterEksisterendeTrygdeavgiftOgReturnererTrygdeavgiftsperiodeMedBelop0() {
        val notSoRandomUuid = UUID.randomUUID()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns notSoRandomUuid

        behandling.apply {
            fagsak = FagsakTestFactory.builder().medBruker().build()
        }

        behandlingsresultat.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 1L
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
            trygdeavgiftsperioder.add(
                Trygdeavgiftsperiode(
                    periodeFra = FOM,
                    periodeTil = TOM,
                    trygdeavgiftsbeløpMd = Penger(790.0),
                    trygdesats = BigDecimal.valueOf(7.9)
                )
            )
        })

        val skatteforholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = FOM
                tomDato = TOM
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        )

        val inntektsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = FOM
                tomDato = TOM
                type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                isArbeidsgiversavgiftBetalesTilSkatt = true
                avgiftspliktigMndInntekt = Penger(BigDecimal(0))
            }
        )
        every { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }.returns(
            listOf(
                TrygdeavgiftsberegningResponse(
                    TrygdeavgiftsperiodeDto(
                        DatoPeriodeDto(FOM, TOM), BigDecimal.valueOf(0), PengerDto(BigDecimal.valueOf(0.0), NOK)
                    ), TrygdeavgiftsgrunnlagDto(
                        idToUUid(behandlingsresultat.medlemskapsperioder.first().id),
                        notSoRandomUuid,
                        notSoRandomUuid
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
            trygdeavgiftsperioder.add(
                Trygdeavgiftsperiode(
                    id = 1L,
                    periodeFra = FOM,
                    periodeTil = TOM,
                    trygdeavgiftsbeløpMd = Penger(790.0),
                    trygdesats = BigDecimal.valueOf(7.9)
                )
            )
        })

        val skatteforholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = FOM
                tomDato = TOM
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            },
            SkatteforholdTilNorge().apply {
                fomDato = FOM
                tomDato = TOM
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        )

        val inntektsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = FOM
                tomDato = TOM
                type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                isArbeidsgiversavgiftBetalesTilSkatt = true
                avgiftspliktigMndInntekt = Penger(BigDecimal(0))
            }
        )
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)


        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
        }.message.shouldContain("Alle skatteforholdsperiodene har samme svar på spørsmålet om skatteplikt")

    }

    @Test
    fun beregnTrygdeavgift_skalIkkeBetaleTrygdeavgiftTilNav_FeilerNarTrygdeavgiftIkkeErBeløp0() {
        val notSoRandomUuid = UUID.randomUUID()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns notSoRandomUuid

        behandling.apply {
            fagsak = FagsakTestFactory.builder().medBruker().build()
        }

        behandlingsresultat.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 2L
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
            trygdeavgiftsperioder.add(
                Trygdeavgiftsperiode(
                    id = 1L,
                    periodeFra = FOM,
                    periodeTil = TOM,
                    trygdeavgiftsbeløpMd = Penger(790.0),
                    trygdesats = BigDecimal.valueOf(7.9)
                )
            )
        })

        val skatteforholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = FOM
                tomDato = TOM
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        )

        val inntektsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = FOM
                tomDato = TOM
                type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                isArbeidsgiversavgiftBetalesTilSkatt = true
                avgiftspliktigMndInntekt = Penger(BigDecimal(0))
            }
        )

        every { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }.returns(
            listOf(
                TrygdeavgiftsberegningResponse(
                    TrygdeavgiftsperiodeDto(
                        DatoPeriodeDto(FOM, TOM), BigDecimal.valueOf(0), PengerDto(BigDecimal.valueOf(123.0), NOK)
                    ), TrygdeavgiftsgrunnlagDto(
                        idToUUid(behandlingsresultat.medlemskapsperioder.first().id),
                        notSoRandomUuid,
                        notSoRandomUuid
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
    fun beregnTrygdeavgift_manglerSkatteforholdINorge_kasterFeil() {
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

        val inntektsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = FOM
                tomDato = TOM
                type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                isArbeidsgiversavgiftBetalesTilSkatt = true
                avgiftspliktigMndInntekt = Penger(BigDecimal(0))
            }
        )


        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, emptyList(), inntektsperioder)
        }.message.shouldContain("Kan ikke beregne trygdeavgift uten skatteforholdTilNorge")
    }

    @Test
    fun beregnTrygdeavgift_manglerInntektsperioder_kasterFeil() {
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

        val skatteforholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = FOM
                tomDato = TOM
                skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
            }
        )


        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, emptyList())
        }.message.shouldContain("Kan ikke beregne trygdeavgift uten inntektsperioder")
    }

    @Test
    fun beregnTrygdeavgift_manglerStartDatoPåMedlemskap_kasterFeil() {
        val medlemskapsperiode = behandlingsresultat.medlemskapsperioder.first()
        medlemskapsperiode.fom = null
        medlemskapsperiode.bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

        val skatteforholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = FOM
                tomDato = TOM
                skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
            }
        )

        val inntektsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = FOM
                tomDato = TOM
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                isArbeidsgiversavgiftBetalesTilSkatt = true
                avgiftspliktigMndInntekt = Penger(BigDecimal(0))
            }
        )


        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
        }.message.shouldContain("Det kreves en innvilget medlemskapsperiode med startdato")
    }

    @Test
    fun beregnTrygdeavgift_erPensjonist_kasterIkkeFeil() {
        val medlemskapsperiode = behandlingsresultat.medlemskapsperioder.first()
        medlemskapsperiode.fom = FOM
        medlemskapsperiode.bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
        val behandling1 = Behandling().apply {
            tema = Behandlingstema.PENSJONIST
            fagsak = FagsakTestFactory.builder().medBruker().build()

        }

        behandlingsresultat.apply {
            behandling = behandling1

        }
        every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandling1)

        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

        val skatteforholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = FOM
                tomDato = TOM
                skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
            }
        )

        val inntektsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = FOM
                tomDato = TOM
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                isArbeidsgiversavgiftBetalesTilSkatt = true
                avgiftspliktigMndInntekt = Penger(BigDecimal(0))
            }
        )
        val notSoRandomUuid = UUID.randomUUID()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns notSoRandomUuid

        every { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }.returns(
            listOf(
                TrygdeavgiftsberegningResponse(
                    TrygdeavgiftsperiodeDto(
                        DatoPeriodeDto(FOM, TOM), BigDecimal.valueOf(7.9), PengerDto(BigDecimal.valueOf(790), NOK)
                    ), TrygdeavgiftsgrunnlagDto(
                        idToUUid(behandlingsresultat.medlemskapsperioder.first().id),
                        notSoRandomUuid,
                        notSoRandomUuid
                    )
                )
            )
        )

        shouldNotThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
        }
    }

    @Test
    fun beregnTrygdeavgift_erIkkePensjonist_kasterFeil() {
        val medlemskapsperiode = behandlingsresultat.medlemskapsperioder.first()
        medlemskapsperiode.fom = FOM
        medlemskapsperiode.bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
        val behandling1 = Behandling().apply {
            tema = Behandlingstema.YRKESAKTIV
            fagsak = FagsakTestFactory.builder().medBruker().build()
        }

        behandlingsresultat.apply {
            behandling = behandling1

        }
        every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandling1)

        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

        val skatteforholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = FOM
                tomDato = TOM
                skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
            }
        )

        val inntektsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = FOM
                tomDato = TOM
                type = Inntektskildetype.PENSJON_UFØRETRYGD
                isArbeidsgiversavgiftBetalesTilSkatt = true
                avgiftspliktigMndInntekt = Penger(BigDecimal(0))
            }
        )

        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
        }.message.shouldContain("Du må oppgi minst en annen inntekt i tillegg til pensjon/uføretrygd")
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
                    fullmakter = setOf(
                        Fullmakt().apply { type = Fullmaktstype.FULLMEKTIG_SØKNAD },
                        Fullmakt().apply { type = Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER })
                })
            ).build()
        }
        trygdeavgiftsberegningService.finnFakturamottakerNavn(BEHANDLING_ID).shouldBe(BRUKER_NAVN)
    }

    private fun idToUUid(id: Long): UUID = UUID.nameUUIDFromBytes(id.toString().toByteArray())
}
