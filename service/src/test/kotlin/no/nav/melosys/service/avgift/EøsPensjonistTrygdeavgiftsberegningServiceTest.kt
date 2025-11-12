package no.nav.melosys.service.avgift

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
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
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService
import no.nav.melosys.service.persondata.PersondataService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

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
        behandling = Behandling.forTest {
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

        behandlingsresultat.apply {
            helseutgiftDekkesPeriode = this@EøsPensjonistTrygdeavgiftsberegningServiceTest.helseutgiftDekkesPeriode
        }

        every { mockEregFasade.hentOrganisasjonNavn(FULLMEKTIG_ORGNR) }.returns(FULLMEKTIG_ORG_NAVN)
        every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
        every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandling)
        every { mockPersondataService.hentSammensattNavn(FULLMEKTIG_AKTØR_ID) }.returns(FULLMEKTIG_NAVN)
        every { mockPersondataService.hentSammensattNavn(BRUKER_AKTØR_ID) }.returns(BRUKER_NAVN)
        every { mockPersondataService.hentPerson(BRUKER_AKTØR_ID).fødselsdato }.returns(FØDSELSDATO)
        every { helseutgiftDekkesPeriodeService.finnHelseutgiftDekkesPeriode(BEHANDLING_ID) }.returns(helseutgiftDekkesPeriode)

    }


    @AfterEach
    fun `Remove RandomNumberGenerator mockks`() {
        unmockkStatic(UUID::class)
    }

    @Test
    fun `hentTrygdeavgiftsberegning - Ingen trygdeavgift - Returner tom liste`() {
        behandlingsresultat.clearTrygdeavgiftsperioderHelseutgiftPeriode()

        trygdeavgiftsberegningService.hentTrygdeavgiftsberegning(BEHANDLING_ID).shouldNotBeNull().shouldBeEmpty()
    }

    @Test
    fun `beregnTrygdeavgift - Skatteforholdsperioden dekker ikke helseutgift dekkes periode - kaster feil`() {
        val skatteforholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = FOM
                tomDato = TOM.minusMonths(1)
                skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
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
        }.message.shouldContain("Skatteforholdsperioden(e) du har lagt inn dekker ikke hele helseutgift periode")
    }

    @Test
    fun `beregnTrygdeavgift - Inntektsperioder dekker ikke helseutgift dekkes periode - kaster feil`() {
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
                tomDato = TOM.minusMonths(1)
                type = Inntektskildetype.PENSJON
                avgiftspliktigMndInntekt = Penger(BigDecimal(10000.0))
                isArbeidsgiversavgiftBetalesTilSkatt = false
            }
        )


        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, skatteforholdsperioder, inntektsperioder)
        }.message.shouldContain("Inntektsperioden(e) du har lagt inn dekker ikke hele helseutgift periode")
    }

    @Test
    fun `beregnTrygdeavgift - EØS pensjonist skal betale Trygdeavgift`() {
        behandling.apply {
            fagsak = Fagsak.forTest { medBruker() }
        }

        val skatteforholdsperiode = SkatteforholdTilNorge().apply {
            fomDato = FOM
            tomDato = TOM
            skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
        }

        val inntektsperiode = Inntektsperiode().apply {
            fomDato = FOM
            tomDato = TOM
            type = Inntektskildetype.PENSJON
            isArbeidsgiversavgiftBetalesTilSkatt = false
            avgiftspliktigMndInntekt = Penger(BigDecimal(10000.0))
        }


        val notSoRandomUuid = UUID.randomUUID()
        val datoPeriodeDto = DatoPeriodeDto(FOM, TOM)
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

        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, listOf(skatteforholdsperiode), listOf(inntektsperiode))
            .single()
            .shouldBeEqualToIgnoringFields(
                Trygdeavgiftsperiode(
                    id = null,
                    periodeFra = FOM,
                    periodeTil = TOM,
                    trygdeavgiftsbeløpMd = Penger(BigDecimal(790), NOK.kode),
                    trygdesats = BigDecimal("7.9"),
                    grunnlagInntekstperiode = inntektsperiode,
                    grunnlagHelseutgiftDekkesPeriode = behandlingsresultat.helseutgiftDekkesPeriode,
                    grunnlagSkatteforholdTilNorge = skatteforholdsperiode,
                    forskuddsvisFaktura = true
                ),
                ignorePrivateFields = false,
                property = Trygdeavgiftsperiode::grunnlagMedlemskapsperiodeNotNull
            )

        verify { trygdeavgiftperiodeErstatter.erstattEøsPensjonistTrygdeavgiftsperioder(BEHANDLING_ID, match { it.isNotEmpty() }) }

        verify(exactly = 1) { mockPersondataService.hentPerson(BRUKER_AKTØR_ID) }
        behandlingsresultat.hentHelseutgiftDekkesPeriode().trygdeavgiftsperioder.shouldNotBeEmpty()
    }

    @Test
    fun `beregnTrygdeavgift - EØS pensjonist skal betale Trygdeavgift - tidligere kalenderår skal ikke forskuddsfaktureres`() {
        val fomIFjor = FOM.minusYears(1)
        val tomIFjor = TOM.minusYears(1)
        behandling.apply {
            fagsak = Fagsak.forTest { medBruker() }
        }

        val skatteforholdsperiode = SkatteforholdTilNorge().apply {
            fomDato = fomIFjor
            tomDato = tomIFjor
            skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
        }

        val inntektsperiode = Inntektsperiode().apply {
            fomDato = fomIFjor
            tomDato = tomIFjor
            type = Inntektskildetype.PENSJON
            isArbeidsgiversavgiftBetalesTilSkatt = false
            avgiftspliktigMndInntekt = Penger(BigDecimal(10000.0))
        }

        helseutgiftDekkesPeriode.apply {
            fomDato = fomIFjor
            tomDato = tomIFjor
        }


        val notSoRandomUuid = UUID.randomUUID()
        val datoPeriodeDto = DatoPeriodeDto(fomIFjor, tomIFjor)
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns notSoRandomUuid

        every { mockBehandlingsresultatService.lagre(any()) }.returns(behandlingsresultat)
        every { mockTrygdeavgiftConsumer.beregnTrygdeavgiftEosPensjonist(ofType(EøsPensjonistTrygdeavgiftsberegningRequest::class)) }.returns(
            listOf(
                EøsPensjonistTrygdeavgiftsberegningResponse(
                    TrygdeavgiftsperiodeDto(
                        DatoPeriodeDto(fomIFjor, tomIFjor), BigDecimal.valueOf(7.9), PengerDto(BigDecimal.valueOf(790), NOK)
                    ), EøsPensjonistTrygdeavgiftsgrunnlagDto(
                        datoPeriodeDto,
                        notSoRandomUuid,
                        notSoRandomUuid
                    )
                )
            )
        )
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, listOf(skatteforholdsperiode), listOf(inntektsperiode))
            .single()
            .shouldBeEqualToIgnoringFields(
                other = Trygdeavgiftsperiode(
                    id = null,
                    periodeFra = fomIFjor,
                    periodeTil = tomIFjor,
                    trygdeavgiftsbeløpMd = Penger(BigDecimal(790), NOK.kode),
                    trygdesats = BigDecimal("7.9"),
                    grunnlagInntekstperiode = inntektsperiode,
                    grunnlagHelseutgiftDekkesPeriode = behandlingsresultat.helseutgiftDekkesPeriode,
                    grunnlagSkatteforholdTilNorge = skatteforholdsperiode,
                    forskuddsvisFaktura = false
                ),
                ignorePrivateFields = false,
                property = Trygdeavgiftsperiode::grunnlagMedlemskapsperiodeNotNull
            )

        verify { trygdeavgiftperiodeErstatter.erstattEøsPensjonistTrygdeavgiftsperioder(BEHANDLING_ID, match { it.isNotEmpty() }) }

        verify(exactly = 1) { mockPersondataService.hentPerson(BRUKER_AKTØR_ID) }
        behandlingsresultat.hentHelseutgiftDekkesPeriode().trygdeavgiftsperioder.shouldNotBeEmpty()
    }

    @Test
    fun `beregnTrygdeavgift - EØS pensjonist skal betale Trygdeavgift - tidligere kalenderår skal forskuddsfaktureres - toggleAv`() {
        unleash.disableAll()
        val fomIFjor = FOM.minusYears(1)
        val tomIFjor = TOM.minusYears(1)
        behandling.apply {
            fagsak = Fagsak.forTest { medBruker() }
        }

        val skatteforholdsperiode = SkatteforholdTilNorge().apply {
            fomDato = fomIFjor
            tomDato = tomIFjor
            skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
        }

        val inntektsperiode = Inntektsperiode().apply {
            fomDato = fomIFjor
            tomDato = tomIFjor
            type = Inntektskildetype.PENSJON
            isArbeidsgiversavgiftBetalesTilSkatt = false
            avgiftspliktigMndInntekt = Penger(BigDecimal(10000.0))
        }

        helseutgiftDekkesPeriode.apply {
            fomDato = fomIFjor
            tomDato = tomIFjor
        }


        val notSoRandomUuid = UUID.randomUUID()
        val datoPeriodeDto = DatoPeriodeDto(fomIFjor, tomIFjor)
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns notSoRandomUuid

        every { mockBehandlingsresultatService.lagre(any()) }.returns(behandlingsresultat)
        every { mockTrygdeavgiftConsumer.beregnTrygdeavgiftEosPensjonist(ofType(EøsPensjonistTrygdeavgiftsberegningRequest::class)) }.returns(
            listOf(
                EøsPensjonistTrygdeavgiftsberegningResponse(
                    TrygdeavgiftsperiodeDto(
                        DatoPeriodeDto(fomIFjor, tomIFjor), BigDecimal.valueOf(7.9), PengerDto(BigDecimal.valueOf(790), NOK)
                    ), EøsPensjonistTrygdeavgiftsgrunnlagDto(
                        datoPeriodeDto,
                        notSoRandomUuid,
                        notSoRandomUuid
                    )
                )
            )
        )
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, listOf(skatteforholdsperiode), listOf(inntektsperiode))
            .single().shouldBeEqualToIgnoringFields(
                other = Trygdeavgiftsperiode(
                    id = null,
                    periodeFra = fomIFjor,
                    periodeTil = tomIFjor,
                    trygdeavgiftsbeløpMd = Penger(BigDecimal(790), NOK.kode),
                    trygdesats = BigDecimal("7.9"),
                    grunnlagInntekstperiode = inntektsperiode,
                    grunnlagHelseutgiftDekkesPeriode = behandlingsresultat.helseutgiftDekkesPeriode,
                    grunnlagSkatteforholdTilNorge = skatteforholdsperiode,
                    forskuddsvisFaktura = true
                ),
                ignorePrivateFields = false,
                property = Trygdeavgiftsperiode::grunnlagMedlemskapsperiodeNotNull
            )

        verify { trygdeavgiftperiodeErstatter.erstattEøsPensjonistTrygdeavgiftsperioder(BEHANDLING_ID, match { it.isNotEmpty() }) }

        verify(exactly = 1) { mockPersondataService.hentPerson(BRUKER_AKTØR_ID) }
        behandlingsresultat.hentHelseutgiftDekkesPeriode().trygdeavgiftsperioder.shouldNotBeEmpty()
    }

    @Test
    fun `beregnTrygdeavgift - skal ikke betale trygdeavgift til nav, sletter eksisterende trygdeavgift og returnerer trygdeavgiftsperiode med beløp 0`() {
        val notSoRandomUuid = UUID.randomUUID()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns notSoRandomUuid
        val datoPeriodeDto = DatoPeriodeDto(FOM, TOM)

        behandling.apply {
            fagsak = Fagsak.forTest { medBruker() }
        }

        behandlingsresultat.hentHelseutgiftDekkesPeriode()
            .trygdeavgiftsperioder.add(
                Trygdeavgiftsperiode(
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
                type = Inntektskildetype.PENSJON
                isArbeidsgiversavgiftBetalesTilSkatt = true
                avgiftspliktigMndInntekt = Penger(BigDecimal(0))
            }
        )


        every { mockTrygdeavgiftConsumer.beregnTrygdeavgiftEosPensjonist(ofType(EøsPensjonistTrygdeavgiftsberegningRequest::class)) }.returns(
            listOf(
                EøsPensjonistTrygdeavgiftsberegningResponse(
                    TrygdeavgiftsperiodeDto(
                        DatoPeriodeDto(FOM, TOM), BigDecimal.valueOf(0), PengerDto(BigDecimal.valueOf(0.0), NOK)
                    ), EøsPensjonistTrygdeavgiftsgrunnlagDto(
                        datoPeriodeDto,
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
    fun `beregnTrygdeavgift - Feiler fordi alle skatteforholdsperioder har samme skatteplikttype`() {
        behandling.apply {
            fagsak = Fagsak.forTest { medBruker() }
        }

        behandlingsresultat.hentHelseutgiftDekkesPeriode()
            .trygdeavgiftsperioder.add(
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
    fun `beregnTrygdeavgift - Skal ikke betale trygdeavgift til nav - Feiler når trygdeavgift ikke er beløp 0`() {
        val notSoRandomUuid = UUID.randomUUID()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns notSoRandomUuid
        val datoPeriodeDto = DatoPeriodeDto(FOM, TOM)

        behandling.apply {
            fagsak = Fagsak.forTest { medBruker() }
        }

        behandlingsresultat.hentHelseutgiftDekkesPeriode()
            .trygdeavgiftsperioder.add(
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

        every { mockTrygdeavgiftConsumer.beregnTrygdeavgiftEosPensjonist(ofType(EøsPensjonistTrygdeavgiftsberegningRequest::class)) }.returns(
            listOf(
                EøsPensjonistTrygdeavgiftsberegningResponse(
                    TrygdeavgiftsperiodeDto(
                        DatoPeriodeDto(FOM, TOM), BigDecimal.valueOf(0), PengerDto(BigDecimal.valueOf(123.0), NOK)
                    ), EøsPensjonistTrygdeavgiftsgrunnlagDto(
                        datoPeriodeDto,
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
    fun `beregnTrygdeavgift - Mangler skatteforhold i Norge - Kaster feil`() {
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
    fun `beregnTrygdeavgift - Mangler inntektsperioder - Kaster feil`() {
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
    fun `finnFakturamottaker - Har ikke fullmektig - Mottaker er bruker`() {
        behandling.apply {
            fagsak = Fagsak.forTest { medBruker() }
        }
        trygdeavgiftsberegningService.finnFakturamottakerNavn(BEHANDLING_ID).shouldBe(BRUKER_NAVN)
    }

    @Test
    fun `finnFakturamottaker - Har fullmektig person for trygdeavgift - Mottaker er fullmektig person`() {
        behandling.apply {
            fagsak = Fagsak.forTest {
                aktører(
                    setOf(Aktoer().apply {
                        aktørId = BRUKER_AKTØR_ID
                        rolle = Aktoersroller.BRUKER
                    }, Aktoer().apply {
                        rolle = Aktoersroller.FULLMEKTIG
                        personIdent = FULLMEKTIG_AKTØR_ID
                        fullmakter = setOf(Fullmakt().apply { type = Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT })
                    })
                )
            }
        }
        trygdeavgiftsberegningService.finnFakturamottakerNavn(BEHANDLING_ID).shouldBe(FULLMEKTIG_NAVN)
    }

    @Test
    fun `finnFakturamottaker - Har fullmektig org for trygdeavgift - Mottaker er fullmektig org`() {
        behandling.apply {
            fagsak = Fagsak.forTest {
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
        trygdeavgiftsberegningService.finnFakturamottakerNavn(BEHANDLING_ID).shouldBe(FULLMEKTIG_ORG_NAVN)
    }

    @Test
    fun `finnFakturamottaker - Har fullmektig men ikke for trygdeavgift - Bruker er fullmektig`() {
        behandling.apply {
            fagsak = Fagsak.forTest {
                aktører(
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
                )
            }
        }
        trygdeavgiftsberegningService.finnFakturamottakerNavn(BEHANDLING_ID).shouldBe(BRUKER_NAVN)
    }

}
