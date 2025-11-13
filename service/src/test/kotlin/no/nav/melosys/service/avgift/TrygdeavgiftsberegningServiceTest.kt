package no.nav.melosys.service.avgift

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.FagsakTestFactory.BRUKER_AKTØR_ID
import no.nav.melosys.domain.avgift.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import org.junit.jupiter.api.*
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

    @MockK(relaxed = true)
    lateinit var mockBehandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var mockPersondataService: PersondataService

    private lateinit var trygdeavgiftperiodeErstatter: TrygdeavgiftperiodeErstatter

    private lateinit var trygdeavgiftMottakerService: TrygdeavgiftMottakerService

    private lateinit var trygdeavgiftsberegningService: TrygdeavgiftsberegningService

    private val unleash: FakeUnleash = FakeUnleash()


    @BeforeEach
    fun setup() {
        trygdeavgiftperiodeErstatter = spyk(TrygdeavgiftperiodeErstatter(mockBehandlingsresultatService))
        trygdeavgiftMottakerService = TrygdeavgiftMottakerService(mockBehandlingsresultatService)
        trygdeavgiftsberegningService = TrygdeavgiftsberegningService(
            mockBehandlingService,
            mockEregFasade,
            mockBehandlingsresultatService,
            trygdeavgiftperiodeErstatter,
            trygdeavgiftMottakerService,
            mockPersondataService,
            mockTrygdeavgiftConsumer,
            unleash
        )
        every { mockEregFasade.hentOrganisasjonNavn(FULLMEKTIG_ORGNR) }.returns(FULLMEKTIG_ORG_NAVN)
        every { mockPersondataService.hentSammensattNavn(FULLMEKTIGAKTØR_ID) }.returns(FULLMEKTIG_NAVN)
        every { mockPersondataService.hentSammensattNavn(BRUKER_AKTØR_ID) }.returns(BRUKER_NAVN)
        every { mockPersondataService.hentPerson(BRUKER_AKTØR_ID).fødselsdato }.returns(FØDSELSDATO)
    }


    @AfterEach
    fun `Remove RandomNumberGenerator mockks`() {
        unmockkStatic(UUID::class)
    }

    @Nested
    inner class HentTrygdeavgiftsberegning {
        @Test
        fun `ingen trygdeavgift skal returnere tom liste`() {
            val behandlingsresultat = Behandlingsresultat.forTest {
                behandling {
                    tema = Behandlingstema.YRKESAKTIV
                    type = Behandlingstyper.NY_VURDERING
                }
                type = Behandlingsresultattyper.IKKE_FASTSATT
            }

            every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
            every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)

            trygdeavgiftsberegningService.hentTrygdeavgiftsberegning(BEHANDLING_ID).shouldNotBeNull().shouldBeEmpty()
        }
    }

    @Nested
    inner class BeregnOgLagreTrygdeavgift {

        @Nested
        inner class VellykketBeregning {
            @Test
            fun `frivillig medlem skal beregne og lagre trygdeavgift`() {
                val notSoRandomUuid = UUID.randomUUID()
                mockkStatic(UUID::class)
                every { UUID.randomUUID() } returns notSoRandomUuid

                val behandlingsresultat = defaultBehandlingsresultat {
                    medlemskapsperiode {
                        id = 1L
                        fom = FOM
                        tom = TOM
                        trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        medlemskapstype = Medlemskapstyper.FRIVILLIG
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
                    }
                }

                every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
                every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)
                every { mockBehandlingsresultatService.lagre(any()) }.returns(behandlingsresultat)
                every { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }.returns(
                    listOf(
                        TrygdeavgiftsberegningResponse(
                            TrygdeavgiftsperiodeDto(
                                DatoPeriodeDto(FOM, TOM), BigDecimal.valueOf(7.9), PengerDto(BigDecimal.valueOf(790), NOK)
                            ), TrygdeavgiftsgrunnlagDto(
                                idToUUid(behandlingsresultat.medlemskapsperioder.first().id!!),
                                notSoRandomUuid,
                                notSoRandomUuid
                            )
                        )
                    )
                )
                every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)
                val trygdeavgiftperidoer = trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
                    behandlingID = BEHANDLING_ID,
                    listOf(
                        skatteforhold { skatteplikttype = Skatteplikttype.SKATTEPLIKTIG }
                    ),
                    inntektsperioder = listOf(
                        inntekt { type = Inntektskildetype.INNTEKT_FRA_UTLANDET }
                    )
                )


                trygdeavgiftperidoer
                    .single()
                    .shouldBeEqualToIgnoringFields(
                        Trygdeavgiftsperiode(
                            id = null,
                            periodeFra = FOM,
                            periodeTil = TOM,
                            trygdeavgiftsbeløpMd = Penger(BigDecimal(790), NOK.kode),
                            trygdesats = BigDecimal("7.9"),
                            grunnlagInntekstperiode = inntekt { type = Inntektskildetype.INNTEKT_FRA_UTLANDET },
                            grunnlagMedlemskapsperiode = behandlingsresultat.medlemskapsperioder.first(),
                            grunnlagHelseutgiftDekkesPeriode = null,
                            grunnlagSkatteforholdTilNorge = skatteforhold { skatteplikttype = Skatteplikttype.SKATTEPLIKTIG },
                        ),
                        ignorePrivateFields = false,
                        property = Trygdeavgiftsperiode::id
                    )
                verify { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }

                verify { trygdeavgiftperiodeErstatter.erstattTrygdeavgiftsperioder(BEHANDLING_ID, match { it.isNotEmpty() }) }

                verify(exactly = 0) { mockPersondataService.hentPerson(BRUKER_AKTØR_ID) }
                behandlingsresultat.trygdeavgiftsperioder.shouldNotBeEmpty()
            }

            @Test
            fun `pliktig medlem skal beregne og lagre trygdeavgift`() {
                val notSoRandomUuid = UUID.randomUUID()
                mockkStatic(UUID::class)
                every { UUID.randomUUID() } returns notSoRandomUuid

                val behandlingsresultat = defaultBehandlingsresultat {
                    medlemskapsperiode {
                        id = 1L
                        fom = FOM
                        tom = TOM
                        trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        medlemskapstype = Medlemskapstyper.PLIKTIG
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
                    }
                }

                every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
                every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)
                every { mockBehandlingsresultatService.lagre(any()) }.returns(behandlingsresultat)
                every { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }.returns(
                    listOf(
                        TrygdeavgiftsberegningResponse(
                            TrygdeavgiftsperiodeDto(
                                DatoPeriodeDto(FOM, TOM), BigDecimal.valueOf(7.9), PengerDto(BigDecimal.valueOf(790), NOK)
                            ), TrygdeavgiftsgrunnlagDto(
                                idToUUid(behandlingsresultat.medlemskapsperioder.first().id!!),
                                notSoRandomUuid,
                                notSoRandomUuid
                            )
                        )
                    )
                )
                every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)
                val trygdeavgiftsperioder = trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
                    BEHANDLING_ID,
                    skatteforholdsperioder = listOf(
                        skatteforhold { skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG }
                    ),
                    inntektsperioder = listOf(
                        inntekt {
                            fomDato = FOM
                            tomDato = TOM
                            type = Inntektskildetype.ARBEIDSINNTEKT
                            arbeidsgiversavgiftBetalesTilSkatt = false
                            avgiftspliktigMndInntekt = Penger(BigDecimal(10000.0))
                        }
                    )
                )


                trygdeavgiftsperioder.shouldNotBeEmpty()
                verify { trygdeavgiftperiodeErstatter.erstattTrygdeavgiftsperioder(BEHANDLING_ID, match { it.isNotEmpty() }) }
                verify(exactly = 1) { mockPersondataService.hentPerson(BRUKER_AKTØR_ID) }
                behandlingsresultat.trygdeavgiftsperioder.shouldNotBeEmpty()
            }

            @Test
            fun `pliktig medlem og skattepliktig skal beregne og lagre trygdeavgift`() {
                val notSoRandomUuid = UUID.randomUUID()
                mockkStatic(UUID::class)
                every { UUID.randomUUID() } returns notSoRandomUuid

                val behandlingsresultat = defaultBehandlingsresultat {
                    medlemskapsperiode {
                        id = 1L
                        fom = FOM
                        tom = TOM
                        trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        medlemskapstype = Medlemskapstyper.PLIKTIG
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
                    }
                }

                every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
                every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)


                trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
                    behandlingID = BEHANDLING_ID,
                    skatteforholdsperioder = listOf(
                        skatteforhold { skatteplikttype = Skatteplikttype.SKATTEPLIKTIG }
                    ),
                    inntektsperioder = emptyList()
                ).shouldNotBeEmpty()


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
            fun `skal ikke betale trygdeavgift til NAV - returnerer periode med beløp 0`() {
                val notSoRandomUuid = UUID.randomUUID()
                mockkStatic(UUID::class)
                every { UUID.randomUUID() } returns notSoRandomUuid

                val behandlingsresultat = Behandlingsresultat.forTest {
                    id = 1L
                    behandling {
                        tema = Behandlingstema.YRKESAKTIV
                        type = Behandlingstyper.NY_VURDERING
                        fagsak {
                            medBruker()
                        }
                    }
                    type = Behandlingsresultattyper.IKKE_FASTSATT

                    medlemskapsperiode {
                        id = 1L
                        fom = FOM
                        tom = TOM
                        trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        medlemskapstype = Medlemskapstyper.PLIKTIG
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
                    }
                }

                every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
                every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)

                every { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }.returns(
                    listOf(
                        TrygdeavgiftsberegningResponse(
                            TrygdeavgiftsperiodeDto(
                                DatoPeriodeDto(FOM, TOM), BigDecimal.valueOf(0), PengerDto(BigDecimal.valueOf(0.0), NOK)
                            ), TrygdeavgiftsgrunnlagDto(
                                idToUUid(behandlingsresultat.medlemskapsperioder.first().id!!),
                                notSoRandomUuid,
                                notSoRandomUuid
                            )
                        )
                    )
                )
                every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)


                val trygdeavgiftsperioder =
                    trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
                        BEHANDLING_ID,
                        skatteforholdsperioder = listOf(
                            skatteforhold { skatteplikttype = Skatteplikttype.SKATTEPLIKTIG }
                        ),
                        inntektsperioder = listOf(
                            inntekt {
                                type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                                arbeidsgiversavgiftBetalesTilSkatt = true
                                avgiftspliktigMndInntekt = Penger(BigDecimal.ZERO)
                            }
                        )
                    )


                trygdeavgiftsperioder.shouldNotBeEmpty()
                trygdeavgiftsperioder.shouldHaveSize(1)
                trygdeavgiftsperioder.forEach {
                    it.trygdesats.shouldBe(BigDecimal.ZERO)
                    it.trygdeavgiftsbeløpMd.shouldBe(Penger(0.0))
                }
            }

            @Test
            fun `pensjonist skal kunne beregne trygdeavgift`() {
                val notSoRandomUuid = UUID.randomUUID()
                mockkStatic(UUID::class)
                every { UUID.randomUUID() } returns notSoRandomUuid

                val behandlingsresultat = Behandlingsresultat.forTest {
                    id = 1L
                    behandling {
                        tema = Behandlingstema.PENSJONIST
                        type = Behandlingstyper.NY_VURDERING
                        fagsak {
                            medBruker()
                        }
                    }
                    type = Behandlingsresultattyper.IKKE_FASTSATT

                    medlemskapsperiode {
                        id = 1L
                        fom = FOM
                        tom = TOM
                        trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        medlemskapstype = Medlemskapstyper.PLIKTIG
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
                    }
                }

                every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
                every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)
                every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

                every { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }.returns(
                    listOf(
                        TrygdeavgiftsberegningResponse(
                            TrygdeavgiftsperiodeDto(
                                DatoPeriodeDto(FOM, TOM), BigDecimal.valueOf(7.9), PengerDto(BigDecimal.valueOf(790), NOK)
                            ), TrygdeavgiftsgrunnlagDto(
                                idToUUid(behandlingsresultat.medlemskapsperioder.first().id!!),
                                notSoRandomUuid,
                                notSoRandomUuid
                            )
                        )
                    )
                )


                shouldNotThrow<FunksjonellException> {
                    trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
                        BEHANDLING_ID,
                        skatteforholdsperioder = listOf(
                            skatteforhold { skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG }
                        ),
                        inntektsperioder = listOf(
                            inntekt {
                                fomDato = FOM
                                tomDato = TOM
                                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                                arbeidsgiversavgiftBetalesTilSkatt = true
                                avgiftspliktigMndInntekt = Penger(BigDecimal(0))
                            }
                        )
                    )
                }
            }
        }

        @Nested
        inner class Forskuddsfakturering {
            @Test
            fun `kalenderår tilbake i tid skal ikke forskuddsfaktureres`() {
                unleash.enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)

                val fomIFjor = FOM.minusYears(1)
                val tomIFjor = TOM.minusYears(1)
                val notSoRandomUuid = UUID.randomUUID()
                mockkStatic(UUID::class)
                every { UUID.randomUUID() } returns notSoRandomUuid

                val behandlingsresultat = defaultBehandlingsresultat {
                    behandling {
                        type = Behandlingstyper.FØRSTEGANG
                    }
                    medlemskapsperiode {
                        id = 1L
                        fom = fomIFjor
                        tom = tomIFjor
                        trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        medlemskapstype = Medlemskapstyper.FRIVILLIG
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
                    }
                }

                val skatteforhold = skatteforhold {
                    fomDato = fomIFjor
                    tomDato = tomIFjor
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }

                val inntekt = inntekt {
                    fomDato = fomIFjor
                    tomDato = tomIFjor
                    type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                    arbeidsgiversavgiftBetalesTilSkatt = false
                    avgiftspliktigMndInntekt = Penger(BigDecimal(10000.0))
                }

                every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
                every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)
                every { mockBehandlingsresultatService.lagre(any()) }.returns(behandlingsresultat)
                every { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }.returns(
                    listOf(
                        TrygdeavgiftsberegningResponse(
                            TrygdeavgiftsperiodeDto(
                                DatoPeriodeDto(fomIFjor, tomIFjor), BigDecimal.valueOf(7.9), PengerDto(BigDecimal.valueOf(790), NOK)
                            ), TrygdeavgiftsgrunnlagDto(
                                idToUUid(behandlingsresultat.medlemskapsperioder.first().id!!),
                                notSoRandomUuid,
                                notSoRandomUuid
                            )
                        )
                    )
                )
                every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)
                val trygdeavgift = trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
                    behandlingID = BEHANDLING_ID,
                    skatteforholdsperioder = listOf(skatteforhold),
                    inntektsperioder = listOf(inntekt)
                )


                trygdeavgift
                    .single()
                    .shouldBeEqualToIgnoringFields(
                        Trygdeavgiftsperiode(
                            id = null,
                            periodeFra = fomIFjor,
                            periodeTil = tomIFjor,
                            trygdeavgiftsbeløpMd = Penger(BigDecimal(790), NOK.kode),
                            trygdesats = BigDecimal("7.9"),
                            grunnlagInntekstperiode = inntekt,
                            grunnlagMedlemskapsperiode = behandlingsresultat.medlemskapsperioder.first(),
                            grunnlagHelseutgiftDekkesPeriode = null,
                            grunnlagSkatteforholdTilNorge = skatteforhold,
                        ),
                        ignorePrivateFields = false,
                        property = Trygdeavgiftsperiode::id
                    )


                verify { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }

                verify { trygdeavgiftperiodeErstatter.erstattTrygdeavgiftsperioder(BEHANDLING_ID, match { it.isNotEmpty() }) }

                verify(exactly = 0) { mockPersondataService.hentPerson(BRUKER_AKTØR_ID) }
                behandlingsresultat.trygdeavgiftsperioder.shouldNotBeEmpty()
            }

            @Test
            fun `kalenderår tilbake i tid skal forskuddsfaktureres når toggle er av`() {
                unleash.disableAll()

                val fomIFjor = FOM.minusYears(1)
                val tomIFjor = TOM.minusYears(1)
                val notSoRandomUuid = UUID.randomUUID()
                mockkStatic(UUID::class)
                every { UUID.randomUUID() } returns notSoRandomUuid

                val behandlingsresultat = defaultBehandlingsresultat {
                    medlemskapsperiode {
                        id = 1L
                        fom = fomIFjor
                        tom = tomIFjor
                        trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        medlemskapstype = Medlemskapstyper.FRIVILLIG
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
                    }
                }

                val skatteforhold = skatteforhold {
                    fomDato = fomIFjor
                    tomDato = tomIFjor
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }

                val inntekt = inntekt {
                    fomDato = fomIFjor
                    tomDato = tomIFjor
                    type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                    arbeidsgiversavgiftBetalesTilSkatt = false
                    avgiftspliktigMndInntekt = Penger(BigDecimal(10000.0))
                }

                val skatteforholdsperioder = listOf(skatteforhold)

                every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
                every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)
                every { mockBehandlingsresultatService.lagre(any()) }.returns(behandlingsresultat)
                every { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }.returns(
                    listOf(
                        TrygdeavgiftsberegningResponse(
                            TrygdeavgiftsperiodeDto(
                                DatoPeriodeDto(fomIFjor, tomIFjor), BigDecimal.valueOf(7.9), PengerDto(BigDecimal.valueOf(790), NOK)
                            ), TrygdeavgiftsgrunnlagDto(
                                idToUUid(behandlingsresultat.medlemskapsperioder.first().id!!),
                                notSoRandomUuid,
                                notSoRandomUuid
                            )
                        )
                    )
                )
                every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)


                val trygdeavgiftsperioder = trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
                    behandlingID = BEHANDLING_ID,
                    skatteforholdsperioder = skatteforholdsperioder,
                    inntektsperioder = listOf(inntekt)
                )


                trygdeavgiftsperioder
                    .single()
                    .shouldBeEqualToIgnoringFields(
                        Trygdeavgiftsperiode(
                            id = null,
                            periodeFra = fomIFjor,
                            periodeTil = tomIFjor,
                            trygdeavgiftsbeløpMd = Penger(BigDecimal(790), NOK.kode),
                            trygdesats = BigDecimal("7.9"),
                            grunnlagInntekstperiode = inntekt,
                            grunnlagMedlemskapsperiode = behandlingsresultat.medlemskapsperioder.first(),
                            grunnlagHelseutgiftDekkesPeriode = null,
                            grunnlagSkatteforholdTilNorge = skatteforhold,
                        ),
                        ignorePrivateFields = false,
                        property = Trygdeavgiftsperiode::id
                    )


                verify { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }

                verify { trygdeavgiftperiodeErstatter.erstattTrygdeavgiftsperioder(BEHANDLING_ID, match { it.isNotEmpty() }) }

                verify(exactly = 0) { mockPersondataService.hentPerson(BRUKER_AKTØR_ID) }
                behandlingsresultat.trygdeavgiftsperioder.shouldNotBeEmpty()
            }
        }

        @Nested
        inner class Validering {
            @Test
            fun `inntektsperioder dekker ikke medlemskapsperioder skal kaste feil`() {
                val behandlingsresultat = Behandlingsresultat.forTest {
                    id = 1L
                    behandling {
                        tema = Behandlingstema.YRKESAKTIV
                        type = Behandlingstyper.NY_VURDERING
                    }
                    type = Behandlingsresultattyper.IKKE_FASTSATT

                    medlemskapsperiode {
                        id = 1L
                        fom = FOM
                        tom = TOM
                        trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        medlemskapstype = Medlemskapstyper.PLIKTIG
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
                    }
                    medlemskapsperiode {
                        id = 2L
                        fom = FOM
                        tom = TOM
                        trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        medlemskapstype = Medlemskapstyper.FRIVILLIG
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
                    }
                }

                every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
                every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)

                shouldThrow<FunksjonellException> {
                    trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
                        behandlingID = BEHANDLING_ID,
                        skatteforholdsperioder = listOf(
                            skatteforhold { skatteplikttype = Skatteplikttype.SKATTEPLIKTIG }
                        ),
                        inntektsperioder = listOf(
                            inntekt {
                                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                                tomDato = TOM.minusMonths(1)
                            }
                        )
                    )
                }.message.shouldContain("Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
            }

            @Test
            fun `skatteforhold dekker ikke medlemskapsperioder skal kaste feil`() {
                val behandlingsresultat = Behandlingsresultat.forTest {
                    id = 1L
                    behandling {
                        tema = Behandlingstema.YRKESAKTIV
                        type = Behandlingstyper.NY_VURDERING
                    }
                    type = Behandlingsresultattyper.IKKE_FASTSATT

                    medlemskapsperiode {
                        id = 1L
                        fom = FOM
                        tom = TOM
                        trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        medlemskapstype = Medlemskapstyper.PLIKTIG
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
                    }
                    medlemskapsperiode {
                        id = 2L
                        fom = FOM
                        tom = TOM
                        trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        medlemskapstype = Medlemskapstyper.FRIVILLIG
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
                    }
                }

                every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
                every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)

                shouldThrow<FunksjonellException> {
                    trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
                        behandlingID = BEHANDLING_ID,
                        skatteforholdsperioder = listOf(
                            skatteforhold {
                                tomDato = TOM.minusMonths(1)
                                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                            }
                        ),
                        inntektsperioder = listOf(
                            inntekt {
                                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                                tomDato = TOM.minusMonths(1)
                            }
                        )
                    )
                }.message.shouldContain("Skatteforholdsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
            }

            @Test
            fun `pliktig medlem med flere medlemskapsperioder skal kaste feil`() {
                val behandlingsresultat = defaultBehandlingsresultat {
                    medlemskapsperiode {
                        id = 1L
                        fom = FOM
                        tom = TOM
                        trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        medlemskapstype = Medlemskapstyper.PLIKTIG
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
                    }
                    medlemskapsperiode {
                        id = 2L
                        fom = FOM
                        tom = TOM
                        trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        medlemskapstype = Medlemskapstyper.PLIKTIG
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
                    }
                }

                every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
                every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)


                assertThrows<IllegalArgumentException> {
                    trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
                        behandlingID = BEHANDLING_ID,
                        skatteforholdsperioder = listOf(
                            skatteforhold { skatteplikttype = Skatteplikttype.SKATTEPLIKTIG }
                        ),
                        inntektsperioder = emptyList()
                    )
                }.message.shouldContain("Det skal ikke være flere enn en medlem- og skatteforholdsperiode når medlemskapet er pliktig og skattepliktig")
            }

            @Test
            fun `samme skatteplikttype for alle perioder skal kaste feil`() {
                val behandlingsresultat = Behandlingsresultat.forTest {
                    id = 1L
                    behandling {
                        tema = Behandlingstema.YRKESAKTIV
                        type = Behandlingstyper.NY_VURDERING
                        fagsak {
                            medBruker()
                        }
                    }
                    type = Behandlingsresultattyper.IKKE_FASTSATT

                    medlemskapsperiode {
                        id = 1L
                        fom = FOM
                        tom = FOM
                        trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        medlemskapstype = Medlemskapstyper.PLIKTIG
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
                    }
                    medlemskapsperiode {
                        id = 2L
                    }
                }

                every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
                every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)

                shouldThrow<FunksjonellException> {
                    trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
                        BEHANDLING_ID,
                        skatteforholdsperioder = listOf(
                            skatteforhold { skatteplikttype = Skatteplikttype.SKATTEPLIKTIG },
                            skatteforhold { skatteplikttype = Skatteplikttype.SKATTEPLIKTIG }
                        ),
                        inntektsperioder = listOf(
                            inntekt {
                                type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                                arbeidsgiversavgiftBetalesTilSkatt = true
                                avgiftspliktigMndInntekt = Penger(BigDecimal.ZERO)
                            }
                        )
                    )
                }.message.shouldContain("Alle skatteforholdsperiodene har samme svar på spørsmålet om skatteplikt")

            }

            @Test
            fun `skal ikke betale trygdeavgift - feiler når beløp ikke er 0`() {
                val notSoRandomUuid = UUID.randomUUID()
                mockkStatic(UUID::class)
                every { UUID.randomUUID() } returns notSoRandomUuid

                val behandlingsresultat = Behandlingsresultat.forTest {
                    id = 1L
                    behandling {
                        tema = Behandlingstema.YRKESAKTIV
                        type = Behandlingstyper.NY_VURDERING
                        fagsak {
                            medBruker()
                        }
                    }
                    type = Behandlingsresultattyper.IKKE_FASTSATT

                    medlemskapsperiode {
                        id = 1L
                        fom = FOM
                        tom = TOM
                        trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        medlemskapstype = Medlemskapstyper.PLIKTIG
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
                    }
                }

                every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
                every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)

                every { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }.returns(
                    listOf(
                        TrygdeavgiftsberegningResponse(
                            TrygdeavgiftsperiodeDto(
                                DatoPeriodeDto(FOM, TOM), BigDecimal.valueOf(0), PengerDto(BigDecimal.valueOf(123.0), NOK)
                            ), TrygdeavgiftsgrunnlagDto(
                                idToUUid(behandlingsresultat.medlemskapsperioder.first().id!!),
                                notSoRandomUuid,
                                notSoRandomUuid
                            )
                        )
                    )
                )


                shouldThrow<IllegalStateException> {
                    trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
                        BEHANDLING_ID,
                        skatteforholdsperioder = listOf(
                            skatteforhold { skatteplikttype = Skatteplikttype.SKATTEPLIKTIG }
                        ),
                        inntektsperioder = listOf(
                            inntekt {
                                type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                                arbeidsgiversavgiftBetalesTilSkatt = true
                                avgiftspliktigMndInntekt = Penger(BigDecimal.ZERO)
                            }
                        )
                    )
                }.message.shouldContain("Trygdeavgift skal ikke betales til NAV. Beregnet trygdeavgift må derfor være 0.")

            }

            @Test
            fun `mangler medlemskapsperioder skal kaste feil`() {
                val behandlingsresultat = Behandlingsresultat.forTest {
                    id = 1L
                    behandling {
                        tema = Behandlingstema.YRKESAKTIV
                        type = Behandlingstyper.NY_VURDERING
                    }
                    type = Behandlingsresultattyper.IKKE_FASTSATT
                }

                every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
                every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)

                shouldThrow<FunksjonellException> {
                    trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
                        BEHANDLING_ID,
                        skatteforholdsperioder = listOf(
                            skatteforhold { skatteplikttype = Skatteplikttype.SKATTEPLIKTIG }
                        ),
                        inntektsperioder = listOf(
                            inntekt {
                                type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                                arbeidsgiversavgiftBetalesTilSkatt = true
                                avgiftspliktigMndInntekt = Penger(BigDecimal.ZERO)
                            }
                        )
                    )
                }.message.shouldContain("Kan ikke beregne trygdeavgift uten medlemskapsperioder")
            }

            @Test
            fun `mangler skatteforhold skal kaste feil`() {
                val behandlingsresultat = Behandlingsresultat.forTest {
                    id = 1L
                    behandling {
                        tema = Behandlingstema.YRKESAKTIV
                        type = Behandlingstyper.NY_VURDERING
                    }
                    type = Behandlingsresultattyper.IKKE_FASTSATT

                    medlemskapsperiode {
                        id = 1L
                        fom = FOM
                        tom = TOM
                        trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        medlemskapstype = Medlemskapstyper.PLIKTIG
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
                    }
                }

                every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
                every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)

                shouldThrow<FunksjonellException> {
                    trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
                        BEHANDLING_ID,
                        skatteforholdsperioder = emptyList(),
                        inntektsperioder = listOf(
                            inntekt {
                                type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                                arbeidsgiversavgiftBetalesTilSkatt = true
                                avgiftspliktigMndInntekt = Penger(BigDecimal.ZERO)
                            }
                        )
                    )
                }.message.shouldContain("Kan ikke beregne trygdeavgift uten skatteforholdTilNorge")
            }

            @Test
            fun `mangler inntektsperioder skal kaste feil`() {
                val behandlingsresultat = Behandlingsresultat.forTest {
                    id = 1L
                    behandling {
                        tema = Behandlingstema.YRKESAKTIV
                        type = Behandlingstyper.NY_VURDERING
                    }
                    type = Behandlingsresultattyper.IKKE_FASTSATT

                    medlemskapsperiode {
                        id = 1L
                        fom = FOM
                        tom = TOM
                        trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        medlemskapstype = Medlemskapstyper.PLIKTIG
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
                    }
                }

                every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
                every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)

                shouldThrow<FunksjonellException> {
                    trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
                        BEHANDLING_ID,
                        skatteforholdsperioder = listOf(
                            skatteforhold {
                                fomDato = FOM
                                tomDato = TOM
                                skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                            }
                        ),
                        inntektsperioder = emptyList()
                    )
                }.message.shouldContain("Kan ikke beregne trygdeavgift uten inntektsperioder")
            }

            @Test
            fun `mangler startdato på medlemskap skal kaste feil`() {
                val behandlingsresultat = Behandlingsresultat.forTest {
                    id = 1L
                    behandling {
                        tema = Behandlingstema.YRKESAKTIV
                        type = Behandlingstyper.NY_VURDERING
                    }
                    type = Behandlingsresultattyper.IKKE_FASTSATT

                    medlemskapsperiode {
                        id = 1L
                        fom = FOM
                        tom = TOM
                        trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        medlemskapstype = Medlemskapstyper.PLIKTIG
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
                    }
                }

                // Set fom to null to test error handling
                behandlingsresultat.medlemskapsperioder.first().fom = null

                every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
                every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)

                shouldThrow<FunksjonellException> {
                    trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
                        BEHANDLING_ID,
                        skatteforholdsperioder = listOf(
                            skatteforhold {
                                fomDato = FOM
                                tomDato = TOM
                                skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                            }
                        ),
                        inntektsperioder = listOf(
                            inntekt {
                                fomDato = FOM
                                tomDato = TOM
                                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                                arbeidsgiversavgiftBetalesTilSkatt = true
                                avgiftspliktigMndInntekt = Penger(BigDecimal(0))
                            }
                        )
                    )
                }.message.shouldContain("Det kreves en innvilget medlemskapsperiode med startdato")
            }

            @Test
            fun `ikke-pensjonist uten inntekt skal kaste feil`() {
                val behandlingsresultat = Behandlingsresultat.forTest {
                    id = 1L
                    behandling {
                        tema = Behandlingstema.YRKESAKTIV
                        type = Behandlingstyper.NY_VURDERING
                        fagsak {
                            medBruker()
                        }
                    }
                    type = Behandlingsresultattyper.IKKE_FASTSATT

                    medlemskapsperiode {
                        id = 1L
                        fom = FOM
                        tom = TOM
                        trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
                        innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                        medlemskapstype = Medlemskapstyper.PLIKTIG
                        bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
                    }
                }

                every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
                every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)

                shouldThrow<FunksjonellException> {
                    trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
                        BEHANDLING_ID,
                        skatteforholdsperioder = listOf(
                            skatteforhold { skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG }
                        ),
                        inntektsperioder = listOf(
                            inntekt {
                                fomDato = FOM
                                tomDato = TOM
                                type = Inntektskildetype.PENSJON_UFØRETRYGD
                                arbeidsgiversavgiftBetalesTilSkatt = true
                                avgiftspliktigMndInntekt = Penger(BigDecimal(0))
                            }
                        )
                    )
                }.message.shouldContain("Du må oppgi minst en annen inntekt i tillegg til pensjon/uføretrygd")
            }
        }
    }

    @Nested
    inner class FinnFakturamottakerNavn {
        @Test
        fun `uten fullmektig skal returnere bruker`() {
            val behandling = Behandling.forTest {
                tema = Behandlingstema.YRKESAKTIV
                type = Behandlingstyper.NY_VURDERING
                fagsak {
                    medBruker()
                }
            }

            every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandling)

            trygdeavgiftsberegningService.finnFakturamottakerNavn(BEHANDLING_ID).shouldBe(BRUKER_NAVN)
        }

        @Test
        fun `med fullmektig person for trygdeavgift skal returnere fullmektig`() {
            val behandling = Behandling.forTest {
                tema = Behandlingstema.YRKESAKTIV
                type = Behandlingstyper.NY_VURDERING
                fagsak {
                    aktører(
                        setOf(Aktoer().apply {
                            aktørId = BRUKER_AKTØR_ID
                            rolle = Aktoersroller.BRUKER
                        }, Aktoer().apply {
                            rolle = Aktoersroller.FULLMEKTIG
                            personIdent = FULLMEKTIGAKTØR_ID
                            fullmakter = setOf(Fullmakt().apply { type = Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT })
                        })
                    )
                }
            }

            every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandling)

            trygdeavgiftsberegningService.finnFakturamottakerNavn(BEHANDLING_ID).shouldBe(FULLMEKTIG_NAVN)
        }

        @Test
        fun `med fullmektig org for trygdeavgift skal returnere org navn`() {
            val behandling = Behandling.forTest {
                tema = Behandlingstema.YRKESAKTIV
                type = Behandlingstyper.NY_VURDERING
                fagsak {
                    aktører(
                        setOf(Aktoer().apply {
                            aktørId = BRUKER_AKTØR_ID
                            rolle = Aktoersroller.BRUKER
                        }, Aktoer().apply {
                            orgnr = FULLMEKTIG_ORGNR
                            rolle = Aktoersroller.FULLMEKTIG
                            fullmakter = setOf(Fullmakt().apply { type = Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT })
                        })
                    )
                }
            }

            every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandling)

            trygdeavgiftsberegningService.finnFakturamottakerNavn(BEHANDLING_ID).shouldBe(FULLMEKTIG_ORG_NAVN)
        }

        @Test
        fun `fullmektig uten trygdeavgift-fullmakt skal returnere bruker`() {
            val behandling = Behandling.forTest {
                tema = Behandlingstema.YRKESAKTIV
                type = Behandlingstyper.NY_VURDERING
                fagsak {
                    aktører(
                        setOf(Aktoer().apply {
                            aktørId = BRUKER_AKTØR_ID
                            rolle = Aktoersroller.BRUKER
                        }, Aktoer().apply {
                            aktørId = FULLMEKTIGAKTØR_ID
                            rolle = Aktoersroller.FULLMEKTIG
                            fullmakter = setOf(
                                Fullmakt().apply { type = Fullmaktstype.FULLMEKTIG_SØKNAD },
                                Fullmakt().apply { type = Fullmaktstype.FULLMEKTIG_ARBEIDSGIVER })
                        })
                    )
                }
            }

            every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandling)

            trygdeavgiftsberegningService.finnFakturamottakerNavn(BEHANDLING_ID).shouldBe(BRUKER_NAVN)
        }
    }

    @Nested
    inner class HentOpprinneligTrygdeavgiftsperioder {

        @Nested
        inner class MedToggleAktivert {
            @Test
            fun `perioder før inneværende år skal forkortes til 1 januar`() {
                unleash.enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)

                val opprinneligBehandling = Behandling.forTest {
                    id = 99L
                    tema = Behandlingstema.YRKESAKTIV
                }

                val opprinneligBehandlingsresultat = Behandlingsresultat.forTest {
                    id = 99L
                    behandling = opprinneligBehandling

                    medlemskapsperiode {
                        fom = LocalDate.of(2024, 6, 1)
                        tom = LocalDate.of(2026, 12, 31)

                        trygdeavgiftsperiode {
                            periodeFra = LocalDate.of(2024, 6, 1)
                            periodeTil = LocalDate.of(2026, 12, 31)
                            trygdeavgiftsbeløpMd = BigDecimal.valueOf(1000)
                            trygdesats = BigDecimal.valueOf(7.9)

                            grunnlagSkatteforholdTilNorge {
                                fomDato = LocalDate.of(2024, 6, 1)
                                tomDato = LocalDate.of(2026, 12, 31)
                                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                            }

                            grunnlagInntekstperiode {
                                fomDato = LocalDate.of(2024, 7, 1)
                                tomDato = LocalDate.of(2026, 11, 30)
                                type = Inntektskildetype.ARBEIDSINNTEKT
                                avgiftspliktigMndInntekt = Penger(BigDecimal.valueOf(50000))
                            }
                        }
                    }
                }

                val behandling = Behandling.forTest {
                    tema = Behandlingstema.YRKESAKTIV
                    type = Behandlingstyper.NY_VURDERING
                }
                behandling.opprinneligBehandling = opprinneligBehandling

                every { mockBehandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
                every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns opprinneligBehandlingsresultat
                every { mockBehandlingsresultatService.hentBehandlingsresultat(99L) } returns opprinneligBehandlingsresultat

                val result = trygdeavgiftsberegningService.hentOpprinneligTrygdeavgiftsperioder(BEHANDLING_ID)
                val førsteJanuar = LocalDate.now().withDayOfYear(1)

                with(result) {
                    skatteforholdsperioder.shouldHaveSize(1).single().run {
                        fomDato shouldBe førsteJanuar
                        tomDato shouldBe LocalDate.of(2026, 12, 31)
                    }

                    inntektsperioder.shouldHaveSize(1).single().run {
                        fomDato shouldBe førsteJanuar
                        tomDato shouldBe LocalDate.of(2026, 11, 30)
                    }
                }
            }

            @Test
            fun `skal filtrere bort perioder som slutter før inneværende år`() {
                unleash.enable("melosys.faktureringskomponenten.ikke-tidligere-perioder")

                val opprinneligBehandling = Behandling.forTest {
                    id = 99L
                    tema = Behandlingstema.YRKESAKTIV
                }

                val inneværendeÅr = LocalDate.now().year

                // Skatteforhold som slutter i fjor - skal filtreres bort
                val gammeltSkatteforhold = skatteforhold {
                    id = 1L
                    fomDato = LocalDate.of(inneværendeÅr - 2, 1, 1)
                    tomDato = LocalDate.of(inneværendeÅr - 1, 12, 31)
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }

                // Skatteforhold som slutter i år - skal beholdes
                val aktivtSkatteforhold = skatteforhold {
                    id = 2L
                    fomDato = LocalDate.of(inneværendeÅr - 1, 6, 1)
                    tomDato = LocalDate.of(inneværendeÅr, 12, 31)
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }

                // Inntektsperiode som slutter i fjor - skal filtreres bort
                val gammelInntekt = inntekt {
                    id = 1L
                    fomDato = LocalDate.of(inneværendeÅr - 2, 1, 1)
                    tomDato = LocalDate.of(inneværendeÅr - 1, 12, 31)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                    avgiftspliktigMndInntekt = Penger(BigDecimal.valueOf(40000))
                }

                // Inntektsperiode som slutter i år - skal beholdes
                val aktivInntekt = inntekt {
                    id = 2L
                    fomDato = LocalDate.of(inneværendeÅr - 1, 7, 1)
                    tomDato = LocalDate.of(inneværendeÅr, 11, 30)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                    avgiftspliktigMndInntekt = Penger(BigDecimal.valueOf(50000))
                }

                val gammelTrygdeavgiftsperiode = Trygdeavgiftsperiode(
                    periodeFra = LocalDate.of(inneværendeÅr - 2, 1, 1),
                    periodeTil = LocalDate.of(inneværendeÅr - 1, 12, 31),
                    trygdeavgiftsbeløpMd = Penger(BigDecimal.valueOf(800)),
                    trygdesats = BigDecimal.valueOf(7.9),
                    grunnlagInntekstperiode = gammelInntekt,
                    grunnlagSkatteforholdTilNorge = gammeltSkatteforhold
                )

                val aktivTrygdeavgiftsperiode = Trygdeavgiftsperiode(
                    periodeFra = LocalDate.of(inneværendeÅr - 1, 6, 1),
                    periodeTil = LocalDate.of(inneværendeÅr, 12, 31),
                    trygdeavgiftsbeløpMd = Penger(BigDecimal.valueOf(1000)),
                    trygdesats = BigDecimal.valueOf(7.9),
                    grunnlagInntekstperiode = aktivInntekt,
                    grunnlagSkatteforholdTilNorge = aktivtSkatteforhold
                )

                val opprinneligBehandlingsresultat = Behandlingsresultat.forTest {
                    id = 99L
                    behandling = opprinneligBehandling

                    medlemskapsperiode {
                        fom = LocalDate.of(inneværendeÅr - 2, 1, 1)
                        tom = LocalDate.of(inneværendeÅr - 1, 12, 31)
                    }

                    medlemskapsperiode {
                        fom = LocalDate.of(inneværendeÅr - 1, 6, 1)
                        tom = LocalDate.of(inneværendeÅr, 12, 31)
                    }
                }

                // Manually add trygdeavgiftsperioder with custom grunnlag
                val gammelMedlemskap = opprinneligBehandlingsresultat.medlemskapsperioder.first()
                val aktivtMedlemskap = opprinneligBehandlingsresultat.medlemskapsperioder.last()

                gammelTrygdeavgiftsperiode.grunnlagMedlemskapsperiode = gammelMedlemskap
                gammelMedlemskap.trygdeavgiftsperioder.add(gammelTrygdeavgiftsperiode)

                aktivTrygdeavgiftsperiode.grunnlagMedlemskapsperiode = aktivtMedlemskap
                aktivtMedlemskap.trygdeavgiftsperioder.add(aktivTrygdeavgiftsperiode)

                val behandling = Behandling.forTest {
                    tema = Behandlingstema.YRKESAKTIV
                    type = Behandlingstyper.NY_VURDERING
                }
                behandling.opprinneligBehandling = opprinneligBehandling

                every { mockBehandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
                every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns opprinneligBehandlingsresultat
                every { mockBehandlingsresultatService.hentBehandlingsresultat(99L) } returns opprinneligBehandlingsresultat

                val result = trygdeavgiftsberegningService.hentOpprinneligTrygdeavgiftsperioder(BEHANDLING_ID)

                // Kun den aktive perioden skal være med (den gamle skal være filtrert bort)
                result.skatteforholdsperioder shouldHaveSize 1
                result.inntektsperioder shouldHaveSize 1

                // Sjekk at det er den aktive perioden vi får tilbake
                val førsteJanuar = LocalDate.now().withDayOfYear(1)

                with(result) {
                    skatteforholdsperioder.single().run {
                        fomDato shouldBe førsteJanuar
                        tomDato shouldBe LocalDate.of(inneværendeÅr, 12, 31)
                    }

                    inntektsperioder.single().run {
                        fomDato shouldBe førsteJanuar
                        tomDato shouldBe LocalDate.of(inneværendeÅr, 11, 30)
                    }
                }
            }
        }

        @Nested
        inner class MedToggleDeaktivert {
            @Test
            fun `skal returnere uendrede perioder`() {
                unleash.disable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)

                val opprinneligBehandling = Behandling.forTest {
                    id = 99L
                    tema = Behandlingstema.YRKESAKTIV
                }

                val opprinneligBehandlingsresultat = Behandlingsresultat.forTest {
                    id = 99L
                    behandling = opprinneligBehandling

                    medlemskapsperiode {
                        fom = LocalDate.of(2024, 6, 1)
                        tom = LocalDate.of(2026, 12, 31)

                        trygdeavgiftsperiode {
                            periodeFra = LocalDate.of(2024, 6, 1)
                            periodeTil = LocalDate.of(2026, 12, 31)
                            trygdeavgiftsbeløpMd = BigDecimal.valueOf(1000)
                            trygdesats = BigDecimal.valueOf(7.9)

                            grunnlagSkatteforholdTilNorge {
                                fomDato = LocalDate.of(2024, 6, 1)
                                tomDato = LocalDate.of(2026, 12, 31)
                                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                            }

                            grunnlagInntekstperiode {
                                fomDato = LocalDate.of(2024, 7, 1)
                                tomDato = LocalDate.of(2026, 11, 30)
                                type = Inntektskildetype.ARBEIDSINNTEKT
                                avgiftspliktigMndInntekt = Penger(BigDecimal.valueOf(50000))
                            }
                        }
                    }
                }

                val behandling = Behandling.forTest {
                    tema = Behandlingstema.YRKESAKTIV
                    type = Behandlingstyper.NY_VURDERING
                }
                behandling.opprinneligBehandling = opprinneligBehandling

                every { mockBehandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
                every { mockBehandlingsresultatService.hentBehandlingsresultat(99L) } returns opprinneligBehandlingsresultat

                val result = trygdeavgiftsberegningService.hentOpprinneligTrygdeavgiftsperioder(BEHANDLING_ID)

                with(result) {
                    skatteforholdsperioder.shouldHaveSize(1).single().run {
                        fomDato shouldBe LocalDate.of(2024, 6, 1)
                        tomDato shouldBe LocalDate.of(2026, 12, 31)
                    }

                    inntektsperioder.shouldHaveSize(1).single().run {
                        fomDato shouldBe LocalDate.of(2024, 7, 1)
                        tomDato shouldBe LocalDate.of(2026, 11, 30)
                    }
                }
            }

            @Test
            fun `skal ikke filtrere bort gamle perioder`() {
                unleash.disable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)

                val opprinneligBehandling = Behandling.forTest {
                    id = 99L
                    tema = Behandlingstema.YRKESAKTIV
                }

                val inneværendeÅr = LocalDate.now().year

                // Skatteforhold som slutter i fjor - skal IKKE filtreres bort når toggle er av
                val gammeltSkatteforhold = skatteforhold {
                    id = 1L
                    fomDato = LocalDate.of(inneværendeÅr - 2, 1, 1)
                    tomDato = LocalDate.of(inneværendeÅr - 1, 12, 31)
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }

                // Skatteforhold som slutter i år
                val aktivtSkatteforhold = skatteforhold {
                    id = 2L
                    fomDato = LocalDate.of(2024, 6, 1)
                    tomDato = LocalDate.of(inneværendeÅr, 12, 31)
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                }

                // Inntektsperiode som slutter i fjor - skal IKKE filtreres bort når toggle er av
                val gammelInntekt = inntekt {
                    id = 1L
                    fomDato = LocalDate.of(inneværendeÅr - 2, 1, 1)
                    tomDato = LocalDate.of(inneværendeÅr - 1, 12, 31)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                    avgiftspliktigMndInntekt = Penger(BigDecimal.valueOf(40000))
                }

                // Inntektsperiode som slutter i år
                val aktivInntekt = inntekt {
                    id = 2L
                    fomDato = LocalDate.of(2024, 7, 1)
                    tomDato = LocalDate.of(inneværendeÅr, 11, 30)
                    type = Inntektskildetype.ARBEIDSINNTEKT
                    avgiftspliktigMndInntekt = Penger(BigDecimal.valueOf(50000))
                }

                val gammelTrygdeavgiftsperiode = Trygdeavgiftsperiode(
                    periodeFra = LocalDate.of(inneværendeÅr - 2, 1, 1),
                    periodeTil = LocalDate.of(inneværendeÅr - 1, 12, 31),
                    trygdeavgiftsbeløpMd = Penger(BigDecimal.valueOf(800)),
                    trygdesats = BigDecimal.valueOf(7.9),
                    grunnlagInntekstperiode = gammelInntekt,
                    grunnlagSkatteforholdTilNorge = gammeltSkatteforhold
                )

                val aktivTrygdeavgiftsperiode = Trygdeavgiftsperiode(
                    periodeFra = LocalDate.of(2024, 6, 1),
                    periodeTil = LocalDate.of(inneværendeÅr, 12, 31),
                    trygdeavgiftsbeløpMd = Penger(BigDecimal.valueOf(1000)),
                    trygdesats = BigDecimal.valueOf(7.9),
                    grunnlagInntekstperiode = aktivInntekt,
                    grunnlagSkatteforholdTilNorge = aktivtSkatteforhold
                )

                val opprinneligBehandlingsresultat = Behandlingsresultat.forTest {
                    behandling = opprinneligBehandling

                    medlemskapsperiode {
                        fom = LocalDate.of(inneværendeÅr - 2, 1, 1)
                        tom = LocalDate.of(inneværendeÅr - 1, 12, 31)
                    }

                    medlemskapsperiode {
                        fom = LocalDate.of(2024, 6, 1)
                        tom = LocalDate.of(inneværendeÅr, 12, 31)
                    }
                }

                val gammelMedlemskap = opprinneligBehandlingsresultat.medlemskapsperioder.first()
                val aktivtMedlemskap = opprinneligBehandlingsresultat.medlemskapsperioder.last()

                gammelTrygdeavgiftsperiode.grunnlagMedlemskapsperiode = gammelMedlemskap
                gammelMedlemskap.trygdeavgiftsperioder.add(gammelTrygdeavgiftsperiode)

                aktivTrygdeavgiftsperiode.grunnlagMedlemskapsperiode = aktivtMedlemskap
                aktivtMedlemskap.trygdeavgiftsperioder.add(aktivTrygdeavgiftsperiode)

                val behandling = Behandling.forTest {
                    tema = Behandlingstema.YRKESAKTIV
                    type = Behandlingstyper.NY_VURDERING
                }
                behandling.opprinneligBehandling = opprinneligBehandling

                every { mockBehandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
                every { mockBehandlingsresultatService.hentBehandlingsresultat(99L) } returns opprinneligBehandlingsresultat

                val result = trygdeavgiftsberegningService.hentOpprinneligTrygdeavgiftsperioder(BEHANDLING_ID)

                with(result) {
                    // Begge periodene skal være med når toggle er av (ingen filtrering)
                    skatteforholdsperioder shouldHaveSize 2
                    inntektsperioder shouldHaveSize 2

                    // Datoene skal være uendret (ikke justert til 1. januar)
                    skatteforholdsperioder.map { it.fomDato } shouldContainExactlyInAnyOrder listOf(
                        LocalDate.of(inneværendeÅr - 2, 1, 1),
                        LocalDate.of(2024, 6, 1)
                    )

                    inntektsperioder.map { it.fomDato } shouldContainExactlyInAnyOrder listOf(
                        LocalDate.of(inneværendeÅr - 2, 1, 1),
                        LocalDate.of(2024, 7, 1)
                    )
                }
            }
        }

        @Test
        fun `skal feile når behandlingstype ikke er NY_VURDERING`() {
            val behandling = Behandling.forTest {
                tema = Behandlingstema.YRKESAKTIV
                type = Behandlingstyper.FØRSTEGANG
            }

            every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandling)

            shouldThrow<IllegalStateException> {
                trygdeavgiftsberegningService.hentOpprinneligTrygdeavgiftsperioder(BEHANDLING_ID)
            }.message.shouldContain("Behandling med id 1 er ikke av type NY_VURDERING")
        }

        @Test
        fun `uten opprinnelig behandling skal returnere tomt grunnlag`() {
            val behandling = Behandling.forTest {
                tema = Behandlingstema.YRKESAKTIV
                type = Behandlingstyper.NY_VURDERING
            }

            every { mockBehandlingService.hentBehandling(BEHANDLING_ID) } returns behandling

            val result = trygdeavgiftsberegningService.hentOpprinneligTrygdeavgiftsperioder(BEHANDLING_ID)

            with(result) {
                skatteforholdsperioder.shouldBeEmpty()
                inntektsperioder.shouldBeEmpty()
            }
        }
    }

    private fun idToUUid(id: Long): UUID = UUID.nameUUIDFromBytes(id.toString().toByteArray())

    private fun skatteforhold(init: TrygdeavgiftsperiodeTestFactory.SkatteforholdTilNorgeBuilder.() -> Unit): SkatteforholdTilNorge =
        TrygdeavgiftsperiodeTestFactory.SkatteforholdTilNorgeBuilder().apply {
            fomDato = FOM
            tomDato = TOM
            init()
        }.build()

    private fun inntekt(init: TrygdeavgiftsperiodeTestFactory.InntektsperiodeBuilder.() -> Unit): Inntektsperiode =
        TrygdeavgiftsperiodeTestFactory.InntektsperiodeBuilder().apply {
            fomDato = FOM
            tomDato = TOM
            init()
        }.build()

    private fun defaultBehandlingsresultat(init: BehandlingsresultatTestFactory.Builder.() -> Unit): Behandlingsresultat =
        Behandlingsresultat.forTest {
            id = 1L
            behandling {
                tema = Behandlingstema.YRKESAKTIV
                type = Behandlingstyper.NY_VURDERING
                fagsak {
                    medBruker()
                }
            }
            type = Behandlingsresultattyper.IKKE_FASTSATT

            init()
        }


    companion object {
        private val FOM: LocalDate = LocalDate.now()
        private val TOM: LocalDate = LocalDate.now().plusMonths(2)
        private const val BEHANDLING_ID: Long = 1L
        private const val FULLMEKTIGAKTØR_ID: String = "123456789"
        private const val FULLMEKTIG_NAVN: String = "Herr Fullmektig"
        private const val FULLMEKTIG_ORGNR: String = "888888888"
        private const val FULLMEKTIG_ORG_NAVN: String = "Aksjeselskap AS"
        private const val BRUKER_NAVN: String = "Bruker Etternavn"
        private val FØDSELSDATO: LocalDate = LocalDate.of(2020, 1, 1)

    }
}
