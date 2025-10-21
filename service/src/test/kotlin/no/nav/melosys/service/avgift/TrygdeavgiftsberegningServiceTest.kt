package no.nav.melosys.service.avgift

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.FagsakTestFactory.BRUKER_AKTØR_ID
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

    @Test
    fun hentTrygdeavgiftsberegning_ingenTrygdeavgift_returnerTomListe() {
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

    @Test
    fun beregnTrygdeavgift_skalBetaleTrygeavgiftFrivilligMedlem_beregnerOgLagrerTrygdeavgift() {
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
                        idToUUid(behandlingsresultat.medlemskapsperioder.first().id),
                        notSoRandomUuid,
                        notSoRandomUuid
                    )
                )
            )
        )
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)
        val skatteforhold = SkatteforholdTilNorge().apply {
            fomDato = FOM
            tomDato = TOM
            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
        }

        val inntekt = Inntektsperiode().apply {
            fomDato = FOM
            tomDato = TOM
            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
            isArbeidsgiversavgiftBetalesTilSkatt = false
            avgiftspliktigMndInntekt = Penger(BigDecimal(10000.0))
        }

        val skatteforholdsperioder = listOf(skatteforhold)


        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, listOf(inntekt))
            .shouldNotBeNull()
            .shouldNotBeEmpty()
            .shouldContainExactly(
                Trygdeavgiftsperiode(
                    id = null,
                    periodeFra = FOM,
                    periodeTil = TOM,
                    trygdeavgiftsbeløpMd = Penger(BigDecimal(790), NOK.kode),
                    trygdesats = BigDecimal("7.9"),
                    grunnlagInntekstperiode = inntekt,
                    grunnlagMedlemskapsperiode = behandlingsresultat.medlemskapsperioder.first(),
                    grunnlagHelseutgiftDekkesPeriode = null,
                    grunnlagSkatteforholdTilNorge = skatteforhold,
                    forskuddsvisFaktura = true
                )
            )


        verify { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }

        verify { trygdeavgiftperiodeErstatter.erstattTrygdeavgiftsperioder(BEHANDLING_ID, match { it.isNotEmpty() }) }

        verify(exactly = 0) { mockPersondataService.hentPerson(BRUKER_AKTØR_ID) }
        behandlingsresultat.trygdeavgiftsperioder.shouldNotBeEmpty()
    }

    @Test
    fun beregnTrygdeavgift_kalenderårTilbakeITid_skalIkkeForskuddsFaktureres() {
        unleash.enable(ToggleName.MELOSYS_FAKTURERINGSKOMPONENTEN_IKKE_TIDLIGERE_PERIODER)

        val fomIFjor = FOM.minusYears(1)
        val tomIFjor = TOM.minusYears(1)
        val notSoRandomUuid = UUID.randomUUID()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns notSoRandomUuid

        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                tema = Behandlingstema.YRKESAKTIV
                type = Behandlingstyper.FØRSTEGANG
                fagsak {
                    medBruker()
                }
            }
            type = Behandlingsresultattyper.IKKE_FASTSATT

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

        val skatteforhold = SkatteforholdTilNorge().apply {
            fomDato = fomIFjor
            tomDato = tomIFjor
            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
        }

        val inntekt = Inntektsperiode().apply {
            fomDato = fomIFjor
            tomDato = tomIFjor
            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
            isArbeidsgiversavgiftBetalesTilSkatt = false
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
                        idToUUid(behandlingsresultat.medlemskapsperioder.first().id),
                        notSoRandomUuid,
                        notSoRandomUuid
                    )
                )
            )
        )
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)


        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, listOf(inntekt))
            .shouldNotBeNull()
            .shouldNotBeEmpty()
            .shouldContainExactly(
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
                    forskuddsvisFaktura = false
                )
            )


        verify { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }

        verify { trygdeavgiftperiodeErstatter.erstattTrygdeavgiftsperioder(BEHANDLING_ID, match { it.isNotEmpty() }) }

        verify(exactly = 0) { mockPersondataService.hentPerson(BRUKER_AKTØR_ID) }
        behandlingsresultat.trygdeavgiftsperioder.shouldNotBeEmpty()
    }

    @Test
    fun beregnTrygdeavgift_kalenderårTilbakeITid_skalForskuddsFaktureres_toggleAv() {
        unleash.disableAll()

        val fomIFjor = FOM.minusYears(1)
        val tomIFjor = TOM.minusYears(1)
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
                fom = fomIFjor
                tom = tomIFjor
                trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
            }
        }

        val skatteforhold = SkatteforholdTilNorge().apply {
            fomDato = fomIFjor
            tomDato = tomIFjor
            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
        }

        val inntekt = Inntektsperiode().apply {
            fomDato = fomIFjor
            tomDato = tomIFjor
            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
            isArbeidsgiversavgiftBetalesTilSkatt = false
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
                        idToUUid(behandlingsresultat.medlemskapsperioder.first().id),
                        notSoRandomUuid,
                        notSoRandomUuid
                    )
                )
            )
        )
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)


        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, listOf(inntekt))
            .shouldNotBeNull()
            .shouldNotBeEmpty()
            .shouldContainExactly(
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
                    forskuddsvisFaktura = true
                )
            )


        verify { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }

        verify { trygdeavgiftperiodeErstatter.erstattTrygdeavgiftsperioder(BEHANDLING_ID, match { it.isNotEmpty() }) }

        verify(exactly = 0) { mockPersondataService.hentPerson(BRUKER_AKTØR_ID) }
        behandlingsresultat.trygdeavgiftsperioder.shouldNotBeEmpty()
    }

    @Test
    fun beregnTrygdeavgift_inntekstperioderDekkerIkkeInnvilgedeMedlemskapsperioder_kasterFeil() {
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
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

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
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                avgiftspliktigMndInntekt = Penger(BigDecimal(10000.0))
                isArbeidsgiversavgiftBetalesTilSkatt = false
            }
        )


        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
        }.message.shouldContain("Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

    @Test
    fun beregnTrygdeavgift_skatteforholdTilNorgeDekkerIkkeInnvilgedeMedlemskapsperioder_kasterFeil() {
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
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

        val skatteforholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = FOM
                tomDato = TOM.minusMonths(1)
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        )

        val inntektsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = FOM
                tomDato = TOM.minusMonths(1)
                type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                avgiftspliktigMndInntekt = Penger(BigDecimal(10000.0))
                isArbeidsgiversavgiftBetalesTilSkatt = false
            }
        )


        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
        }.message.shouldContain("Skatteforholdsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

    @Test
    fun beregnTrygdeavgift_skalBetaleTrygeavgiftPliktigMedlem_beregnerOgLagrerTrygdeavgift() {
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
                skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
            }
        )

        val inntektsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = FOM
                tomDato = TOM
                type = Inntektskildetype.ARBEIDSINNTEKT
                isArbeidsgiversavgiftBetalesTilSkatt = false
                avgiftspliktigMndInntekt = Penger(BigDecimal(10000.0))
            }
        )

        every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
        every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)
        every { mockBehandlingsresultatService.lagre(any()) }.returns(behandlingsresultat)
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
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)


        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
            .shouldNotBeNull().shouldNotBeEmpty()

        verify { trygdeavgiftperiodeErstatter.erstattTrygdeavgiftsperioder(BEHANDLING_ID, match { it.isNotEmpty() }) }

        verify(exactly = 1) { mockPersondataService.hentPerson(BRUKER_AKTØR_ID) }
        behandlingsresultat.trygdeavgiftsperioder.shouldNotBeEmpty()
    }

    // Tester for pliktig medlem og skattepliktig::

    @Test
    fun `beregnTrygdeavgift for pliktig medlem og skattepliktig skal ikke godta flere medlemskapsperioder`() {
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

        val skatteforholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = FOM
                tomDato = TOM
                skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
            }
        )

        every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
        every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)

        assertThrows<IllegalArgumentException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(
                BEHANDLING_ID,
                skatteforholdsperioder,
                emptyList()
            )
        }.message.shouldContain("Det skal ikke være flere enn en medlem- og skatteforholdsperiode når medlemskapet er pliktig og skattepliktig")
    }

    @Test
    fun `beregnTrygdeavgift for pliktig medlem og skattepliktig skal beregne og lagre trygdeavgift`() {
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

        every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
        every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)

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

//                trygdeavgiftsperiode { // Dette er ikke nødvedig for grønn test. Er det riktig?
//                    periodeFra = FOM
//                    periodeTil = TOM
//                    trygdeavgiftsbeløpMd = 790.0.toBigDecimal()
//                    trygdesats = 7.9.toBigDecimal()
//                }
            }
        }

        every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
        every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)

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

        // Add trygdeavgiftsperiode to second medlemskapsperiode
//        behandlingsresultat.medlemskapsperioder.last().trygdeavgiftsperioder.add(
//            Trygdeavgiftsperiode(
//                id = 1L,
//                periodeFra = FOM,
//                periodeTil = TOM,
//                trygdeavgiftsbeløpMd = Penger(790.0),
//                trygdesats = BigDecimal.valueOf(7.9)
//            )
//        )

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

        every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
        every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)
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

        // Add trygdeavgiftsperiode to the existing medlemskapsperiode
        behandlingsresultat.medlemskapsperioder.first().trygdeavgiftsperioder.add(
            Trygdeavgiftsperiode(
                id = 1L,
                periodeFra = FOM,
                periodeTil = TOM,
                trygdeavgiftsbeløpMd = Penger(790.0),
                trygdesats = BigDecimal.valueOf(7.9)
            )
        )

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
    fun beregnTrygdeavgift_manglerMedlemskapsperioder_kasterFeil() {
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
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

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


        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
        }.message.shouldContain("Kan ikke beregne trygdeavgift uten medlemskapsperioder")
    }

    @Test
    fun beregnTrygdeavgift_manglerSkatteforholdINorge_kasterFeil() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                tema = Behandlingstema.YRKESAKTIV
                type = Behandlingstyper.NY_VURDERING
            }
            type = Behandlingsresultattyper.IKKE_FASTSATT

            medlemskapsperiode {
                id = 1L
                fom = fom
                tom = tom
                trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.PLIKTIG
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
            }
        }

        every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
        every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

        val inntektsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = fom
                tomDato = tom
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
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                tema = Behandlingstema.YRKESAKTIV
                type = Behandlingstyper.NY_VURDERING
            }
            type = Behandlingsresultattyper.IKKE_FASTSATT

            medlemskapsperiode {
                id = 1L
                fom = fom
                tom = tom
                trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.PLIKTIG
                bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
            }
        }

        every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
        every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandlingsresultat.behandling)
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

        val skatteforholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = fom
                tomDato = tom
                skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
            }
        )


        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, emptyList())
        }.message.shouldContain("Kan ikke beregne trygdeavgift uten inntektsperioder")
    }

    @Test
    fun beregnTrygdeavgift_manglerStartDatoPåMedlemskap_kasterFeil() {
        val behandlingsresultat = Behandlingsresultat.forTest {
            id = 1L
            behandling {
                tema = Behandlingstema.YRKESAKTIV
                type = Behandlingstyper.NY_VURDERING
            }
            type = Behandlingsresultattyper.IKKE_FASTSATT

            medlemskapsperiode {
                id = 1L
                fom = fom
                tom = tom
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
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

        val skatteforholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = fom
                tomDato = tom
                skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
            }
        )

        val inntektsperioder = listOf(
            Inntektsperiode().apply {
                fomDato = fom
                tomDato = tom
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
    fun finnFakturamottaker_harFullmektigPersonForTrygdeavgift_mottakerErFullmektigPerson() {
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
    fun finnFakturamottaker_harFullmektigOrgForTrygdeavgift_mottakerErFullmektigOrg() {
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
    fun finnFakturamottaker_harFullmektigMenIkkeForTrygdeavgift_brukerErFullmektig() {
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

    private fun idToUUid(id: Long): UUID = UUID.nameUUIDFromBytes(id.toString().toByteArray())

    @Test
    fun `hentOpprinneligTrygdeavgiftsperioder med toggle på og perioder før inneværende år skal forkorte perioder til 1 januar`() {
        unleash.enable("melosys.faktureringskomponenten.ikke-tidligere-perioder")

        val opprinneligBehandling = Behandling.forTest {
            id = 99L
            tema = Behandlingstema.YRKESAKTIV
        }

        val skatteforhold = SkatteforholdTilNorge().apply {
            fomDato = LocalDate.of(2024, 6, 1)
            tomDato = LocalDate.of(2026, 12, 31)
            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
        }

        val inntektsperiode = Inntektsperiode().apply {
            fomDato = LocalDate.of(2024, 7, 1)
            tomDato = LocalDate.of(2026, 11, 30)
            type = Inntektskildetype.ARBEIDSINNTEKT
            avgiftspliktigMndInntekt = Penger(BigDecimal.valueOf(50000))
        }

        val opprinneligBehandlingsresultat = Behandlingsresultat.forTest {
            id = 99L
            behandling = opprinneligBehandling

            medlemskapsperiode {
                fom = LocalDate.of(2024, 6, 1)
                tom = LocalDate.of(2026, 12, 31)
            }
        }

        // Manually add trygdeavgiftsperiode with custom grunnlag since DSL doesn't support all properties
        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            periodeFra = LocalDate.of(2024, 6, 1),
            periodeTil = LocalDate.of(2026, 12, 31),
            trygdeavgiftsbeløpMd = Penger(BigDecimal.valueOf(1000)),
            trygdesats = BigDecimal.valueOf(7.9),
            grunnlagInntekstperiode = inntektsperiode,
            grunnlagSkatteforholdTilNorge = skatteforhold,
            grunnlagMedlemskapsperiode = opprinneligBehandlingsresultat.medlemskapsperioder.first()
        )
        opprinneligBehandlingsresultat.medlemskapsperioder.first().trygdeavgiftsperioder.add(trygdeavgiftsperiode)

        val behandling = Behandling.forTest {
            tema = Behandlingstema.YRKESAKTIV
            type = Behandlingstyper.NY_VURDERING
        }
        behandling.opprinneligBehandling = opprinneligBehandling

        every { mockBehandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { mockBehandlingsresultatService.hentBehandlingsresultat(99L) } returns opprinneligBehandlingsresultat

        val result = trygdeavgiftsberegningService.hentOpprinneligTrygdeavgiftsperioder(BEHANDLING_ID)

        result.skatteforholdsperioder shouldHaveSize 1
        result.inntektsperioder shouldHaveSize 1

        val førsteJanuar = LocalDate.now().withDayOfYear(1)
        result.skatteforholdsperioder[0].fomDato shouldBe førsteJanuar
        result.skatteforholdsperioder[0].tomDato shouldBe LocalDate.of(2026, 12, 31)

        result.inntektsperioder[0].fomDato shouldBe førsteJanuar
        result.inntektsperioder[0].tomDato shouldBe LocalDate.of(2026, 11, 30)
    }

    @Test
    fun `hentOpprinneligTrygdeavgiftsperioder skal feile når behandlingstype ikke er NY_VURDERING`() {
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
    fun `hentOpprinneligTrygdeavgiftsperioder med toggle av skal returnere uendrede perioder`() {
        unleash.disable("melosys.faktureringskomponenten.ikke-tidligere-perioder")

        val opprinneligBehandling = Behandling.forTest {
            id = 99L
            tema = Behandlingstema.YRKESAKTIV
        }

        val skatteforhold = SkatteforholdTilNorge().apply {
            fomDato = LocalDate.of(2024, 6, 1)
            tomDato = LocalDate.of(2026, 12, 31)
            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
        }

        val inntektsperiode = Inntektsperiode().apply {
            fomDato = LocalDate.of(2024, 7, 1)
            tomDato = LocalDate.of(2026, 11, 30)
            type = Inntektskildetype.ARBEIDSINNTEKT
            avgiftspliktigMndInntekt = Penger(BigDecimal.valueOf(50000))
        }

        val opprinneligBehandlingsresultat = Behandlingsresultat.forTest {
            id = 99L
            behandling = opprinneligBehandling

            medlemskapsperiode {
                fom = LocalDate.of(2024, 6, 1)
                tom = LocalDate.of(2026, 12, 31)
            }
        }

        // Manually add trygdeavgiftsperiode with custom grunnlag since DSL doesn't support all properties
        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            periodeFra = LocalDate.of(2024, 6, 1),
            periodeTil = LocalDate.of(2026, 12, 31),
            trygdeavgiftsbeløpMd = Penger(BigDecimal.valueOf(1000)),
            trygdesats = BigDecimal.valueOf(7.9),
            grunnlagInntekstperiode = inntektsperiode,
            grunnlagSkatteforholdTilNorge = skatteforhold,
            grunnlagMedlemskapsperiode = opprinneligBehandlingsresultat.medlemskapsperioder.first()
        )
        opprinneligBehandlingsresultat.medlemskapsperioder.first().trygdeavgiftsperioder.add(trygdeavgiftsperiode)

        val behandling = Behandling.forTest {
            tema = Behandlingstema.YRKESAKTIV
            type = Behandlingstyper.NY_VURDERING
        }
        behandling.opprinneligBehandling = opprinneligBehandling

        every { mockBehandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { mockBehandlingsresultatService.hentBehandlingsresultat(99L) } returns opprinneligBehandlingsresultat

        val result = trygdeavgiftsberegningService.hentOpprinneligTrygdeavgiftsperioder(BEHANDLING_ID)

        result.skatteforholdsperioder shouldHaveSize 1
        result.inntektsperioder shouldHaveSize 1

        result.skatteforholdsperioder[0].fomDato shouldBe LocalDate.of(2024, 6, 1)
        result.skatteforholdsperioder[0].tomDato shouldBe LocalDate.of(2026, 12, 31)

        result.inntektsperioder[0].fomDato shouldBe LocalDate.of(2024, 7, 1)
        result.inntektsperioder[0].tomDato shouldBe LocalDate.of(2026, 11, 30)
    }

    @Test
    fun `hentOpprinneligTrygdeavgiftsperioder uten opprinnelig behandling skal returnere tomt grunnlag`() {
        val behandling = Behandling.forTest {
            tema = Behandlingstema.YRKESAKTIV
            type = Behandlingstyper.NY_VURDERING
        }

        every { mockBehandlingService.hentBehandling(BEHANDLING_ID) } returns behandling

        val result = trygdeavgiftsberegningService.hentOpprinneligTrygdeavgiftsperioder(BEHANDLING_ID)

        result.skatteforholdsperioder.shouldBeEmpty()
        result.inntektsperioder.shouldBeEmpty()
    }

    @Test
    fun `hentOpprinneligTrygdeavgiftsperioder skal filtrere bort perioder som slutter før inneværende år`() {
        unleash.enable("melosys.faktureringskomponenten.ikke-tidligere-perioder")

        val opprinneligBehandling = Behandling.forTest {
            id = 99L
            tema = Behandlingstema.YRKESAKTIV
        }

        val inneværendeÅr = LocalDate.now().year

        // Skatteforhold som slutter i fjor - skal filtreres bort
        val gammeltSkatteforhold = SkatteforholdTilNorge().apply {
            id = 1L
            fomDato = LocalDate.of(inneværendeÅr - 2, 1, 1)
            tomDato = LocalDate.of(inneværendeÅr - 1, 12, 31)
            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
        }

        // Skatteforhold som slutter i år - skal beholdes
        val aktivtSkatteforhold = SkatteforholdTilNorge().apply {
            id = 2L
            fomDato = LocalDate.of(inneværendeÅr - 1, 6, 1)
            tomDato = LocalDate.of(inneværendeÅr, 12, 31)
            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
        }

        // Inntektsperiode som slutter i fjor - skal filtreres bort
        val gammelInntekt = Inntektsperiode().apply {
            id = 1L
            fomDato = LocalDate.of(inneværendeÅr - 2, 1, 1)
            tomDato = LocalDate.of(inneværendeÅr - 1, 12, 31)
            type = Inntektskildetype.ARBEIDSINNTEKT
            avgiftspliktigMndInntekt = Penger(BigDecimal.valueOf(40000))
        }

        // Inntektsperiode som slutter i år - skal beholdes
        val aktivInntekt = Inntektsperiode().apply {
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
        every { mockBehandlingsresultatService.hentBehandlingsresultat(99L) } returns opprinneligBehandlingsresultat

        val result = trygdeavgiftsberegningService.hentOpprinneligTrygdeavgiftsperioder(BEHANDLING_ID)

        // Kun den aktive perioden skal være med (den gamle skal være filtrert bort)
        result.skatteforholdsperioder shouldHaveSize 1
        result.inntektsperioder shouldHaveSize 1

        // Sjekk at det er den aktive perioden vi får tilbake
        val førsteJanuar = LocalDate.now().withDayOfYear(1)
        result.skatteforholdsperioder[0].fomDato shouldBe førsteJanuar
        result.skatteforholdsperioder[0].tomDato shouldBe LocalDate.of(inneværendeÅr, 12, 31)

        result.inntektsperioder[0].fomDato shouldBe førsteJanuar
        result.inntektsperioder[0].tomDato shouldBe LocalDate.of(inneværendeÅr, 11, 30)
    }

    @Test
    fun `hentOpprinneligTrygdeavgiftsperioder skal ikke filtrere bort perioder som slutter før inneværende år når toggle er av`() {
        unleash.disable("melosys.faktureringskomponenten.ikke-tidligere-perioder")

        val opprinneligBehandling = Behandling.forTest {
            id = 99L
            tema = Behandlingstema.YRKESAKTIV
        }

        val inneværendeÅr = LocalDate.now().year

        // Skatteforhold som slutter i fjor - skal IKKE filtreres bort når toggle er av
        val gammeltSkatteforhold = SkatteforholdTilNorge().apply {
            id = 1L
            fomDato = LocalDate.of(inneværendeÅr - 2, 1, 1)
            tomDato = LocalDate.of(inneværendeÅr - 1, 12, 31)
            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
        }

        // Skatteforhold som slutter i år
        val aktivtSkatteforhold = SkatteforholdTilNorge().apply {
            id = 2L
            fomDato = LocalDate.of(2024, 6, 1)
            tomDato = LocalDate.of(inneværendeÅr, 12, 31)
            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
        }

        // Inntektsperiode som slutter i fjor - skal IKKE filtreres bort når toggle er av
        val gammelInntekt = Inntektsperiode().apply {
            id = 1L
            fomDato = LocalDate.of(inneværendeÅr - 2, 1, 1)
            tomDato = LocalDate.of(inneværendeÅr - 1, 12, 31)
            type = Inntektskildetype.ARBEIDSINNTEKT
            avgiftspliktigMndInntekt = Penger(BigDecimal.valueOf(40000))
        }

        // Inntektsperiode som slutter i år
        val aktivInntekt = Inntektsperiode().apply {
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
            id = 99L
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
        every { mockBehandlingsresultatService.hentBehandlingsresultat(99L) } returns opprinneligBehandlingsresultat

        val result = trygdeavgiftsberegningService.hentOpprinneligTrygdeavgiftsperioder(BEHANDLING_ID)

        // Begge periodene skal være med når toggle er av (ingen filtrering)
        result.skatteforholdsperioder shouldHaveSize 2
        result.inntektsperioder shouldHaveSize 2

        // Datoene skal være uendret (ikke justert til 1. januar)
        result.skatteforholdsperioder.any { it.fomDato == LocalDate.of(inneværendeÅr - 2, 1, 1) } shouldBe true
        result.skatteforholdsperioder.any { it.fomDato == LocalDate.of(2024, 6, 1) } shouldBe true

        result.inntektsperioder.any { it.fomDato == LocalDate.of(inneværendeÅr - 2, 1, 1) } shouldBe true
        result.inntektsperioder.any { it.fomDato == LocalDate.of(2024, 7, 1) } shouldBe true
    }

    companion object {
        private val FOM: LocalDate = LocalDate.now()
        private val TOM: LocalDate = LocalDate.now().plusMonths(2)
        private val BEHANDLING_ID: Long = 1L
        private val FULLMEKTIGAKTØR_ID: String = "123456789"
        private val FULLMEKTIG_NAVN: String = "Herr Fullmektig"
        private val FULLMEKTIG_ORGNR: String = "888888888"
        private val FULLMEKTIG_ORG_NAVN: String = "Aksjeselskap AS"
        private val BRUKER_NAVN: String = "Bruker Etternavn"
        private val FØDSELSDATO: LocalDate = LocalDate.of(2020, 1, 1)

    }
}


