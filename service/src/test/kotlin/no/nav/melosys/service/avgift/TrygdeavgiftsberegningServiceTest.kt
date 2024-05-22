package no.nav.melosys.service.avgift

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
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
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import no.nav.melosys.service.MedlemAvFolketrygdenService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService.Companion.idToUUID
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService.Companion.toUUID
import no.nav.melosys.service.avgift.dto.InntektskildeRequest
import no.nav.melosys.service.avgift.dto.OppdaterTrygdeavgiftsgrunnlagRequest
import no.nav.melosys.service.avgift.dto.SkatteforholdTilNorgeRequest
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataService
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
    private lateinit var mockMedlemAvFolketrygdenService: MedlemAvFolketrygdenService

    @MockK
    private lateinit var mockTrygdeavgiftConsumer: TrygdeavgiftConsumer

    @MockK
    private lateinit var mockPersondataService: PersondataService

    private var trygdeavgiftMottakerService: TrygdeavgiftMottakerService = TrygdeavgiftMottakerService()

    private lateinit var trygdeavgiftsberegningService: TrygdeavgiftsberegningService

    private lateinit var medlemAvFolketrygden: MedlemAvFolketrygden
    private lateinit var behandling: Behandling

    private val FOM: LocalDate = LocalDate.now()
    private val TOM: LocalDate = LocalDate.now().plusMonths(1)
    private val BEHANDLING_ID: Long = 1291
    private val FULLMEKTIG_AKTØR_ID: String = "123456789"
    private val FULLMEKTIG_NAVN: String = "Herr Fullmektig"
    private val FULLMEKTIG_ORGNR: String = "888888888"
    private val FULLMEKTIG_ORG_NAVN: String = "Aksjeselskap AS"
    private val BRUKER_NAVN: String = "Bruker Etternavn"
    private val FØDSELSDATO: LocalDate = LocalDate.of(2020, 1, 1)


    @BeforeEach
    fun setup() {
        trygdeavgiftsberegningService =
            TrygdeavgiftsberegningService(
                mockBehandlingService,
                mockEregFasade,
                mockMedlemAvFolketrygdenService,
                trygdeavgiftMottakerService,
                mockPersondataService,
                mockTrygdeavgiftConsumer,
            )
        medlemAvFolketrygden = MedlemAvFolketrygden()
        behandling = Behandling()
        medlemAvFolketrygden.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 1L
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.PLIKTIG
        })
        every { mockEregFasade.hentOrganisasjonNavn(FULLMEKTIG_ORGNR) }.returns(FULLMEKTIG_ORG_NAVN)
        every { mockMedlemAvFolketrygdenService.hentMedlemAvFolketrygden(BEHANDLING_ID) }.returns(medlemAvFolketrygden)
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
        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift()
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder = null
        every { mockMedlemAvFolketrygdenService.finnMedlemAvFolketrygden(BEHANDLING_ID) }.returns(Optional.of(medlemAvFolketrygden))


        trygdeavgiftsberegningService.hentTrygdeavgiftsberegning(BEHANDLING_ID)
            .shouldNotBeNull()
            .shouldBeEmpty()
    }

    @Test
    fun hentTrygdeavgiftsberegning_ingenFastsattTrygdeavgift_returnerTomListe() {
        medlemAvFolketrygden.fastsattTrygdeavgift = null
        every { mockMedlemAvFolketrygdenService.finnMedlemAvFolketrygden(BEHANDLING_ID) }.returns(Optional.of(medlemAvFolketrygden))


        trygdeavgiftsberegningService.hentTrygdeavgiftsberegning(BEHANDLING_ID)
            .shouldNotBeNull()
            .shouldBeEmpty()
    }

    @Test
    fun hentTrygdeavgiftsberegning_ingenMedlemAvFolketrygden_returnerTomListe() {
        every { mockMedlemAvFolketrygdenService.finnMedlemAvFolketrygden(BEHANDLING_ID) }.returns(Optional.empty())


        trygdeavgiftsberegningService.hentTrygdeavgiftsberegning(BEHANDLING_ID)
            .shouldNotBeNull()
            .shouldBeEmpty()
    }

    @Test
    fun beregnTrygdeavgift_skalBetaleTrygeavgiftFrivilligMedlem_beregnerOgLagrerTrygdeavgift() {
        behandling.apply {
            fagsak = FagsakTestFactory.builder().medBruker().build()
        }
        medlemAvFolketrygden.medlemskapsperioder = listOf(Medlemskapsperiode().apply {
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

        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift()
        val oppdateringRequest = OppdaterTrygdeavgiftsgrunnlagRequest(
            listOf(
                SkatteforholdTilNorgeRequest(
                    FOM,
                    TOM,
                    Skatteplikttype.IKKE_SKATTEPLIKTIG //Endre til SKATTEPLIKTIG
                )
            ),
            listOf(
                InntektskildeRequest(
                    Inntektskildetype.INNTEKT_FRA_UTLANDET,
                    false,
                    BigDecimal(10000.0),
                    FOM,
                    TOM
                )
            )
        )

        every { mockMedlemAvFolketrygdenService.lagre(any()) }.returns(medlemAvFolketrygden)
        every { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }
            .returns(
                listOf(
                    TrygdeavgiftsberegningResponse(
                        TrygdeavgiftsperiodeDto(
                            DatoPeriodeDto(FOM, TOM),
                            BigDecimal.valueOf(7.9),
                            PengerDto(BigDecimal.valueOf(790), NOK)
                        ),
                        TrygdeavgiftsgrunnlagDto(
                            notSoRandomUuid,
                            oppdateringRequest.skatteforholdTilNorgeList.first().toUUID(),
                            notSoRandomUuid
                        )
                    )
                )
            )
        every { mockMedlemAvFolketrygdenService.lagreOgFlush(medlemAvFolketrygden) }.returns(medlemAvFolketrygden)


        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, oppdateringRequest)
            .shouldNotBeNull()
            .shouldNotBeEmpty()
            .forEach {
                it.apply {
                    trygdesats = BigDecimal.valueOf(7.9)
                    trygdeavgiftsbeløpMd = Penger(790.0)
                }
            }


        verify { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }
        verify { mockMedlemAvFolketrygdenService.lagreOgFlush(medlemAvFolketrygden) }
        verify(exactly = 0) { mockPersondataService.hentPerson(BRUKER_AKTØR_ID) }
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.shouldNotBeEmpty()
    }

    @Test
    fun beregnTrygdeavgift_inntekstperioderDekkerIkkeInnvilgedeMedlemskapsperioder_kasterFeil() {
        medlemAvFolketrygden.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 1L
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
        })
        every { mockMedlemAvFolketrygdenService.lagreOgFlush(medlemAvFolketrygden) }.returns(medlemAvFolketrygden)

        val oppdateringRequest = OppdaterTrygdeavgiftsgrunnlagRequest(
            listOf(
                SkatteforholdTilNorgeRequest(
                    FOM,
                    TOM,
                    Skatteplikttype.SKATTEPLIKTIG
                )
            ),
            listOf(
                InntektskildeRequest(
                    Inntektskildetype.INNTEKT_FRA_UTLANDET,
                    false,
                    BigDecimal(10000.0),
                    FOM,
                    TOM.minusMonths(1)
                )
            )
        )

        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, oppdateringRequest)
        }.message.shouldContain("Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

    @Test
    fun beregnTrygdeavgift_skatteforholdTilNorgeDekkerIkkeInnvilgedeMedlemskapsperioder_kasterFeil() {
        medlemAvFolketrygden.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 1L
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
        })
        every { mockMedlemAvFolketrygdenService.lagreOgFlush(medlemAvFolketrygden) }.returns(medlemAvFolketrygden)

        val oppdateringRequest = OppdaterTrygdeavgiftsgrunnlagRequest(
            listOf(
                SkatteforholdTilNorgeRequest(
                    FOM,
                    TOM.minusMonths(1),
                    Skatteplikttype.SKATTEPLIKTIG
                )
            ),
            listOf(
                InntektskildeRequest(
                    Inntektskildetype.INNTEKT_FRA_UTLANDET,
                    false,
                    BigDecimal(10000.0),
                    FOM,
                    TOM.minusMonths(1)
                )
            )
        )

        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, oppdateringRequest)
        }.message.shouldContain("Skatteforholdsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }


    @Test
    fun beregnTrygdeavgift_skalBetaleTrygeavgiftPliktigMedlem_beregnerOgLagrerTrygdeavgift() {
        behandling.apply {
            fagsak = FagsakTestFactory.builder().medBruker().build()
        }

        val oppdateringRequest = OppdaterTrygdeavgiftsgrunnlagRequest(
            listOf(
                SkatteforholdTilNorgeRequest(
                    FOM,
                    TOM,
                    Skatteplikttype.IKKE_SKATTEPLIKTIG
                )
            ),
            listOf(
                InntektskildeRequest(
                    Inntektskildetype.ARBEIDSINNTEKT,
                    false,
                    BigDecimal(10000.0),
                    FOM,
                    TOM
                )
            )
        )
        val notSoRandomUuid = UUID.randomUUID()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns notSoRandomUuid

        every { mockMedlemAvFolketrygdenService.lagre(any()) }.returns(medlemAvFolketrygden)
        every { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }
            .returns(
                listOf(
                    TrygdeavgiftsberegningResponse(
                        TrygdeavgiftsperiodeDto(
                            DatoPeriodeDto(FOM, TOM),
                            BigDecimal.valueOf(7.9),
                            PengerDto(BigDecimal.valueOf(790), NOK)
                        ),
                        TrygdeavgiftsgrunnlagDto(
                            notSoRandomUuid,
                            oppdateringRequest.skatteforholdTilNorgeList.first().toUUID(),
                            notSoRandomUuid                        )
                    )
                )
            )
        every { mockMedlemAvFolketrygdenService.lagreOgFlush(medlemAvFolketrygden) }.returns(medlemAvFolketrygden)


        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, oppdateringRequest)
            .shouldNotBeNull()
            .shouldNotBeEmpty()
            .forEach {
                it.apply {
                    trygdesats = BigDecimal.valueOf(7.9)
                    trygdeavgiftsbeløpMd = Penger(790.0)
                }
            }


        verify { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }
        verify { mockMedlemAvFolketrygdenService.lagreOgFlush(medlemAvFolketrygden) }
        verify(exactly = 1) { mockPersondataService.hentPerson(BRUKER_AKTØR_ID) }
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.shouldNotBeEmpty()
    }

    @Test
    fun beregnTrygdeavgift_skalIkkeBetaleTrygdeavgiftTilNav_sletterEksisterendeTrygdeavgiftOgReturnererTrygdeavgiftsperiodeMedBelop0() {
        behandling.apply {
            fagsak = FagsakTestFactory.builder().medBruker().build()
        }

        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
            medlemAvFolketrygden = medlemAvFolketrygden
            trygdeavgiftsperioder = hashSetOf(
                Trygdeavgiftsperiode().apply {
                    periodeFra = FOM
                    periodeTil = TOM
                    trygdeavgiftsbeløpMd = Penger(790.0)
                    trygdesats = BigDecimal.valueOf(7.9)
                }
            )
        }

        val oppdateringRequest = OppdaterTrygdeavgiftsgrunnlagRequest(
            listOf(
                SkatteforholdTilNorgeRequest(
                    FOM,
                    TOM,
                    Skatteplikttype.SKATTEPLIKTIG
                )
            ),
            listOf(
                InntektskildeRequest(
                    Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE,
                    true,
                    BigDecimal(0),
                    FOM,
                    TOM
                )
            )
        )
        every { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }
            .returns(
                listOf(
                    TrygdeavgiftsberegningResponse(
                        TrygdeavgiftsperiodeDto(
                            DatoPeriodeDto(FOM, TOM),
                            BigDecimal.valueOf(0),
                            PengerDto(BigDecimal.valueOf(0.0), NOK)
                        ),
                        TrygdeavgiftsgrunnlagDto(
                            medlemAvFolketrygden.medlemskapsperioder.first().idToUUID(),
                            oppdateringRequest.skatteforholdTilNorgeList.first().toUUID(),
                            UUID.randomUUID()
                        )
                    )
                )
            )
        every { mockMedlemAvFolketrygdenService.lagreOgFlush(medlemAvFolketrygden) }.returns(medlemAvFolketrygden)


        val trygdeavgiftsperioder = trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, oppdateringRequest)


        trygdeavgiftsperioder.shouldNotBeNull()
        trygdeavgiftsperioder.shouldNotBeEmpty()
        trygdeavgiftsperioder.shouldHaveSize(1)
        trygdeavgiftsperioder.forEach {
            it.trygdesats.shouldBe(BigDecimal.ZERO)
            it.trygdeavgiftsbeløpMd.shouldBe(Penger(0.0))
        }
    }


    @Test
    fun beregnTrygdeavgift_skalIkkeBetaleTrygdeavgiftTilNav_FeilerNarTrygdeavgiftIkkeErBeløp0() {
        behandling.apply {
            fagsak = FagsakTestFactory.builder().medBruker().build()
        }

        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
            medlemAvFolketrygden = medlemAvFolketrygden
            trygdeavgiftsperioder = hashSetOf(
                Trygdeavgiftsperiode().apply {
                    periodeFra = FOM
                    periodeTil = TOM
                    trygdeavgiftsbeløpMd = Penger(790.0)
                    trygdesats = BigDecimal.valueOf(7.9)
                }
            )
        }

        val oppdateringRequest = OppdaterTrygdeavgiftsgrunnlagRequest(
            listOf(
                SkatteforholdTilNorgeRequest(
                    FOM,
                    TOM,
                    Skatteplikttype.SKATTEPLIKTIG
                )
            ),
            listOf(
                InntektskildeRequest(
                    Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE,
                    true,
                    BigDecimal(0),
                    FOM,
                    TOM
                )
            )
        )
        every { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }
            .returns(
                listOf(
                    TrygdeavgiftsberegningResponse(
                        TrygdeavgiftsperiodeDto(
                            DatoPeriodeDto(FOM, TOM),
                            BigDecimal.valueOf(0),
                            PengerDto(BigDecimal.valueOf(123.0), NOK)
                        ),
                        TrygdeavgiftsgrunnlagDto(
                            medlemAvFolketrygden.medlemskapsperioder.first().idToUUID(),
                            oppdateringRequest.skatteforholdTilNorgeList.first().toUUID(),
                            UUID.randomUUID()
                        )
                    )
                )
            )
        every { mockMedlemAvFolketrygdenService.lagreOgFlush(medlemAvFolketrygden) }.returns(medlemAvFolketrygden)

        shouldThrow<IllegalStateException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, oppdateringRequest)
        }.message.shouldContain("Trygdeavgift skal ikke betales til NAV. Beregnet trygdeavgift må derfor være 0.")

    }

    @Test
    fun beregnTrygdeavgift_manglerMedlemskapsperioder_kasterFeil() {
        medlemAvFolketrygden.medlemskapsperioder = emptyList()
        every { mockMedlemAvFolketrygdenService.lagreOgFlush(medlemAvFolketrygden) }.returns(medlemAvFolketrygden)
        val oppdateringRequest = OppdaterTrygdeavgiftsgrunnlagRequest(
            listOf(
                SkatteforholdTilNorgeRequest(
                    FOM,
                    TOM,
                    Skatteplikttype.SKATTEPLIKTIG
                )
            ),
            listOf(
                InntektskildeRequest(
                    Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE,
                    true,
                    BigDecimal(0),
                    FOM,
                    TOM
                )
            )
        )

        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, oppdateringRequest)
        }.message.shouldContain("Kan ikke beregne trygdeavgift uten medlemskapsperioder")
    }

    @Test
    fun beregnTrygdeavgift_manglerSkatteforholdINorge_kasterFeil() {
        every { mockMedlemAvFolketrygdenService.lagreOgFlush(medlemAvFolketrygden) }.returns(medlemAvFolketrygden)
        val oppdateringRequest = OppdaterTrygdeavgiftsgrunnlagRequest(
            listOf(),
            listOf(
                InntektskildeRequest(
                    Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE,
                    true,
                    BigDecimal(0),
                    FOM,
                    TOM
                )
            )
        )
        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, oppdateringRequest)
        }.message.shouldContain("Kan ikke beregne trygdeavgift uten skatteforholdTilNorge")
    }

    @Test
    fun beregnTrygdeavgift_manglerInntektsperioder_kasterFeil() {
        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift()
        every { mockMedlemAvFolketrygdenService.lagreOgFlush(medlemAvFolketrygden) }.returns(medlemAvFolketrygden)
        val oppdateringRequest = OppdaterTrygdeavgiftsgrunnlagRequest(
            listOf(
                SkatteforholdTilNorgeRequest(
                    FOM,
                    TOM,
                    Skatteplikttype.SKATTEPLIKTIG
                )
            ),
            listOf()
        )

        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, oppdateringRequest)
        }.message.shouldContain("Kan ikke beregne trygdeavgift uten inntektsperioder")
    }

    @Test
    fun beregnTrygdeavgift_manglerStartDatoPåMedlemskap_kasterFeil() {
        medlemAvFolketrygden.medlemskapsperioder = listOf(Medlemskapsperiode())
        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift()
        every { mockMedlemAvFolketrygdenService.lagreOgFlush(medlemAvFolketrygden) }.returns(medlemAvFolketrygden)

        val oppdateringRequest = OppdaterTrygdeavgiftsgrunnlagRequest(
            listOf(
                SkatteforholdTilNorgeRequest(
                    FOM,
                    TOM,
                    Skatteplikttype.IKKE_SKATTEPLIKTIG
                )
            ),
            listOf(
                InntektskildeRequest(
                    Inntektskildetype.INNTEKT_FRA_UTLANDET,
                    true,
                    BigDecimal(0),
                    FOM,
                    TOM
                )
            )
        )

        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID, oppdateringRequest)
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
            fagsak = FagsakTestFactory.builder()
                .aktører(
                    setOf(
                        Aktoer().apply {
                            aktørId = BRUKER_AKTØR_ID
                            rolle = Aktoersroller.BRUKER
                        },
                        Aktoer().apply {
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
                setOf(
                    Aktoer().apply {
                        aktørId = BRUKER_AKTØR_ID
                        rolle = Aktoersroller.BRUKER
                    },
                    Aktoer().apply {
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
                setOf(
                    Aktoer().apply {
                        aktørId = BRUKER_AKTØR_ID
                        rolle = Aktoersroller.BRUKER
                    },
                    Aktoer().apply {
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
