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
import no.nav.melosys.exception.FunksjonellException
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

    private lateinit var behandling: Behandling
    private lateinit var behandlingsresultat: Behandlingsresultat

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
        unleash.resetAll()
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
        behandling = Behandling.forTest {
            tema = Behandlingstema.YRKESAKTIV
        }
        behandlingsresultat = Behandlingsresultat().apply {
            id = 1L
            behandling = this@TrygdeavgiftsberegningServiceTest.behandling
            type = Behandlingsresultattyper.IKKE_FASTSATT
        }

        behandlingsresultat.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 1L
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.PLIKTIG
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
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
            fagsak = Fagsak.forTest { medBruker() }
        }
        behandlingsresultat.medlemskapsperioder = listOf(Medlemskapsperiode().apply {
            id = 1L
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
        })
        val notSoRandomUuid = UUID.randomUUID()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns notSoRandomUuid

        val inntekt = Inntektsperiode().apply {
            fomDato = FOM
            tomDato = TOM
            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
            isArbeidsgiversavgiftBetalesTilSkatt = false
            avgiftspliktigMndInntekt = Penger(BigDecimal(10000.0))
        }

        val skatteforhold = SkatteforholdTilNorge().apply {
            fomDato = FOM
            tomDato = TOM
            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
        }


        val skatteforholdsperioder = listOf(skatteforhold)

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
        unleash.enable("melosys.faktureringskomponenten.ikke-tidligere-perioder")

        val fomIFjor = FOM.minusYears(1)
        val tomIFjor = TOM.minusYears(1)
        behandling.apply {
            fagsak = Fagsak.forTest { medBruker() }
        }
        behandlingsresultat.medlemskapsperioder = listOf(Medlemskapsperiode().apply {
            id = 1L
            fom = fomIFjor
            tom = tomIFjor
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
        })
        val notSoRandomUuid = UUID.randomUUID()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns notSoRandomUuid

        val inntekt = Inntektsperiode().apply {
            fomDato = fomIFjor
            tomDato = tomIFjor
            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
            isArbeidsgiversavgiftBetalesTilSkatt = false
            avgiftspliktigMndInntekt = Penger(BigDecimal(10000.0))
        }

        val skatteforhold = SkatteforholdTilNorge().apply {
            fomDato = fomIFjor
            tomDato = tomIFjor
            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
        }


        val skatteforholdsperioder = listOf(skatteforhold)

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
        behandling.apply {
            fagsak = Fagsak.forTest { medBruker() }
        }
        behandlingsresultat.medlemskapsperioder = listOf(Medlemskapsperiode().apply {
            id = 1L
            fom = fomIFjor
            tom = tomIFjor
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
        })
        val notSoRandomUuid = UUID.randomUUID()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns notSoRandomUuid

        val inntekt = Inntektsperiode().apply {
            fomDato = fomIFjor
            tomDato = tomIFjor
            type = Inntektskildetype.INNTEKT_FRA_UTLANDET
            isArbeidsgiversavgiftBetalesTilSkatt = false
            avgiftspliktigMndInntekt = Penger(BigDecimal(10000.0))
        }

        val skatteforhold = SkatteforholdTilNorge().apply {
            fomDato = fomIFjor
            tomDato = tomIFjor
            skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
        }


        val skatteforholdsperioder = listOf(skatteforhold)

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
        behandlingsresultat.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 1L
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
        })
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
        behandlingsresultat.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 1L
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
        })
        every { mockBehandlingsresultatService.lagreOgFlush(behandlingsresultat) }.returns(behandlingsresultat)

        val skatteforholdsperioder = listOf(
            SkatteforholdTilNorge().apply {
                fomDato = FOM
                tomDato = TOM.minusMonths(1)
                Skatteplikttype.SKATTEPLIKTIG

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
        behandling.apply {
            fagsak = Fagsak.forTest { medBruker() }
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
        behandling.apply {
            fagsak = Fagsak.forTest { medBruker() }
        }

        behandlingsresultat.addMedlemskapsperiode(Medlemskapsperiode().apply {
            id = 1L
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.PLIKTIG
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
        })
        behandlingsresultat.addMedlemskapsperiode(Medlemskapsperiode().apply {
            id = 2L
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.PLIKTIG
            bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_3_ANDRE_LEDD
        })


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
        behandling.apply {
            fagsak = Fagsak.forTest { medBruker() }
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
            fagsak = Fagsak.forTest { medBruker() }
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
            fagsak = Fagsak.forTest { medBruker() }
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
            fagsak = Fagsak.forTest { medBruker() }
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
    fun beregnTrygdeavgift_manglerMedlemskapsperioder_kasterFeil() {
        behandlingsresultat.medlemskapsperioder = emptyList()
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
        val behandling1 = Behandling.forTest {
            tema = Behandlingstema.PENSJONIST
            fagsak = Fagsak.forTest { medBruker() }

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
        val behandling1 = Behandling.forTest {
            tema = Behandlingstema.YRKESAKTIV
            fagsak = Fagsak.forTest { medBruker() }
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
            fagsak = Fagsak.forTest { medBruker() }
        }
        trygdeavgiftsberegningService.finnFakturamottakerNavn(BEHANDLING_ID).shouldBe(BRUKER_NAVN)
    }

    @Test
    fun finnFakturamottaker_harFullmektigPersonForTrygdeavgift_mottakerErFullmektigPerson() {
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
    fun finnFakturamottaker_harFullmektigOrgForTrygdeavgift_mottakerErFullmektigOrg() {
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
    fun finnFakturamottaker_harFullmektigMenIkkeForTrygdeavgift_brukerErFullmektig() {
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

    private fun idToUUid(id: Long): UUID = UUID.nameUUIDFromBytes(id.toString().toByteArray())

    @Test
    fun `hentOpprinneligTrygdeavgiftsperioder med toggle på og perioder før inneværende år skal forkorte perioder til 1 januar`() {
        unleash.enable("melosys.faktureringskomponenten.ikke-tidligere-perioder")

        val opprinneligBehandling = Behandling.forTest { tema = Behandlingstema.YRKESAKTIV }
        opprinneligBehandling.id = 99L

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

        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            periodeFra = LocalDate.of(2024, 6, 1),
            periodeTil = LocalDate.of(2026, 12, 31),
            trygdeavgiftsbeløpMd = Penger(BigDecimal.valueOf(1000)),
            trygdesats = BigDecimal.valueOf(7.9),
            grunnlagInntekstperiode = inntektsperiode,
            grunnlagSkatteforholdTilNorge = skatteforhold
        )

        val medlemskapsperiode = Medlemskapsperiode().apply {
            fom = LocalDate.of(2024, 6, 1)
            tom = LocalDate.of(2026, 12, 31)
        }
        trygdeavgiftsperiode.grunnlagMedlemskapsperiode = medlemskapsperiode
        medlemskapsperiode.trygdeavgiftsperioder.add(trygdeavgiftsperiode)

        val opprinneligBehandlingsresultat = Behandlingsresultat().apply {
            id = 99L
            behandling = opprinneligBehandling
            medlemskapsperioder = mutableListOf(medlemskapsperiode)
        }
        medlemskapsperiode.behandlingsresultat = opprinneligBehandlingsresultat

        behandling.opprinneligBehandling = opprinneligBehandling

        every { mockBehandlingService.hentBehandling(BEHANDLING_ID) } returns behandling
        every { mockBehandlingsresultatService.hentBehandlingsresultat(99L) } returns opprinneligBehandlingsresultat

        val result = trygdeavgiftsberegningService.hentOpprinneligTrygdeavgiftsperioder(BEHANDLING_ID)

        result.skatteforholdsperioder shouldHaveSize 1
        result.inntektsperioder shouldHaveSize 1

        val første1Januar = LocalDate.now().withDayOfYear(1)
        result.skatteforholdsperioder[0].fomDato shouldBe første1Januar
        result.skatteforholdsperioder[0].tomDato shouldBe LocalDate.of(2026, 12, 31)

        result.inntektsperioder[0].fomDato shouldBe første1Januar
        result.inntektsperioder[0].tomDato shouldBe LocalDate.of(2026, 11, 30)
    }

    @Test
    fun `hentOpprinneligTrygdeavgiftsperioder med toggle av skal returnere uendrede perioder`() {
        unleash.disable("melosys.faktureringskomponenten.ikke-tidligere-perioder")

        val opprinneligBehandling = Behandling.forTest { tema = Behandlingstema.YRKESAKTIV }
        opprinneligBehandling.id = 99L

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

        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            periodeFra = LocalDate.of(2024, 6, 1),
            periodeTil = LocalDate.of(2026, 12, 31),
            trygdeavgiftsbeløpMd = Penger(BigDecimal.valueOf(1000)),
            trygdesats = BigDecimal.valueOf(7.9),
            grunnlagInntekstperiode = inntektsperiode,
            grunnlagSkatteforholdTilNorge = skatteforhold
        )

        val medlemskapsperiode = Medlemskapsperiode().apply {
            fom = LocalDate.of(2024, 6, 1)
            tom = LocalDate.of(2026, 12, 31)
        }
        trygdeavgiftsperiode.grunnlagMedlemskapsperiode = medlemskapsperiode
        medlemskapsperiode.trygdeavgiftsperioder.add(trygdeavgiftsperiode)

        val opprinneligBehandlingsresultat = Behandlingsresultat().apply {
            id = 99L
            behandling = opprinneligBehandling
            medlemskapsperioder = mutableListOf(medlemskapsperiode)
        }
        medlemskapsperiode.behandlingsresultat = opprinneligBehandlingsresultat

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
        behandling.opprinneligBehandling = null

        every { mockBehandlingService.hentBehandling(BEHANDLING_ID) } returns behandling

        val result = trygdeavgiftsberegningService.hentOpprinneligTrygdeavgiftsperioder(BEHANDLING_ID)

        result.skatteforholdsperioder.shouldBeEmpty()
        result.inntektsperioder.shouldBeEmpty()
    }
}


