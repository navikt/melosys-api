package no.nav.melosys.service.avgift

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.avgift.*
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.ereg.EregFasade
import no.nav.melosys.integrasjon.trygdeavgift.TrygdeavgiftConsumer
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import no.nav.melosys.service.MedlemAvFolketrygdenService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataService
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
    private val BRUKER_AKTØR_ID: String = "987654321"
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
        every { mockEregFasade.hentOrganisasjonNavn(FULLMEKTIG_ORGNR)}.returns(FULLMEKTIG_ORG_NAVN)
        every { mockMedlemAvFolketrygdenService.hentMedlemAvFolketrygden(BEHANDLING_ID) }.returns(medlemAvFolketrygden)
        every { mockBehandlingService.hentBehandling(BEHANDLING_ID) }.returns(behandling)
        every { mockPersondataService.hentSammensattNavn(FULLMEKTIG_AKTØR_ID) }.returns(FULLMEKTIG_NAVN)
        every { mockPersondataService.hentSammensattNavn(BRUKER_AKTØR_ID) }.returns(BRUKER_NAVN)
        every { mockPersondataService.hentPerson(BRUKER_AKTØR_ID).fødselsdato }.returns(FØDSELSDATO)
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

        medlemAvFolketrygden.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 1L
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.FRIVILLIG
        })
        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                skatteforholdTilNorge = listOf(SkatteforholdTilNorge().apply {
                    id = 1L
                    fomDato = FOM
                    tomDato = TOM
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                })
                inntektsperioder = listOf(Inntektsperiode().apply {
                    id = 1L
                    fomDato = FOM
                    tomDato = TOM
                    type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                    isArbeidsgiversavgiftBetalesTilSkatt = false
                    avgiftspliktigInntektMnd = Penger(10000.0)
                })
            }
        }
        medlemAvFolketrygden.fastsattTrygdeavgift.medlemAvFolketrygden = medlemAvFolketrygden
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
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            UUID.randomUUID()
                        )
                    )
                )
            )


        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID)
            .shouldNotBeNull()
            .shouldNotBeEmpty()
            .forEach {
                it.apply {
                    trygdesats = BigDecimal.valueOf(7.9)
                    trygdeavgiftsbeløpMd = Penger(790.0)
                }
            }
        verify { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }
        verify { mockMedlemAvFolketrygdenService.lagre(medlemAvFolketrygden) }
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

        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                skatteforholdTilNorge = listOf(SkatteforholdTilNorge().apply {
                    id = 1L
                    fomDato = FOM
                    tomDato = TOM
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                })
                inntektsperioder = listOf(Inntektsperiode().apply {
                    id = 1L
                    fomDato = FOM
                    tomDato = TOM.minusMonths(1)
                    type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                    isArbeidsgiversavgiftBetalesTilSkatt = false
                    avgiftspliktigInntektMnd = Penger(10000.0)
                })
            }
        }

        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID)
        }.message.shouldContain("Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }


    @Test
    fun beregnTrygdeavgift_skalBetaleTrygeavgiftPliktigMedlem_beregnerOgLagrerTrygdeavgift() {
        behandling.apply {
            fagsak = Fagsak().apply {
                aktører = setOf(
                    Aktoer().apply {
                        aktørId = BRUKER_AKTØR_ID
                        rolle = Aktoersroller.BRUKER
                    }
                )
            }
        }

        medlemAvFolketrygden.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 1L
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.FULL_DEKNING_FTRL
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            medlemskapstype = Medlemskapstyper.PLIKTIG
        })
        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                skatteforholdTilNorge = listOf(SkatteforholdTilNorge().apply {
                    id = 1L
                    fomDato = FOM
                    tomDato = TOM
                    skatteplikttype = Skatteplikttype.IKKE_SKATTEPLIKTIG
                })
                inntektsperioder = listOf(Inntektsperiode().apply {
                    id = 1L
                    fomDato = FOM
                    tomDato = TOM
                    type = Inntektskildetype.ARBEIDSINNTEKT
                    isArbeidsgiversavgiftBetalesTilSkatt = false
                    avgiftspliktigInntektMnd = Penger(10000.0)
                })
            }
        }
        medlemAvFolketrygden.fastsattTrygdeavgift.medlemAvFolketrygden = medlemAvFolketrygden
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
                            UUID.randomUUID(),
                            UUID.randomUUID(),
                            UUID.randomUUID()
                        )
                    )
                )
            )


        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID)
            .shouldNotBeNull()
            .shouldNotBeEmpty()
            .forEach {
                it.apply {
                    trygdesats = BigDecimal.valueOf(7.9)
                    trygdeavgiftsbeløpMd = Penger(790.0)
                }
            }
        verify { mockTrygdeavgiftConsumer.beregnTrygdeavgift(ofType(TrygdeavgiftsberegningRequest::class)) }
        verify { mockMedlemAvFolketrygdenService.lagre(medlemAvFolketrygden) }
        verify(exactly = 1) { mockPersondataService.hentPerson(BRUKER_AKTØR_ID) }
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.shouldNotBeEmpty()
    }

    @Test
    fun beregnTrygdeavgift_skalIkkeBetaleTrygdeavgiftTilNav_sletterEksisterendeTrygdeavgiftOgReturnererTomListe() {
        medlemAvFolketrygden.medlemskapsperioder.add(Medlemskapsperiode().apply {
            id = 1L
            fom = FOM
            tom = TOM
            trygdedekning = Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        })
        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                skatteforholdTilNorge = listOf(SkatteforholdTilNorge().apply {
                    id = 1L
                    fomDato = FOM
                    tomDato = TOM
                    skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                })
                inntektsperioder = listOf(Inntektsperiode().apply {
                    id = 1L
                    fomDato = FOM
                    tomDato = TOM
                    type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                    isArbeidsgiversavgiftBetalesTilSkatt = true
                    avgiftspliktigInntektMnd = null
                })
            }
            trygdeavgiftsperioder.add(Trygdeavgiftsperiode())
        }


        medlemAvFolketrygden.fastsattTrygdeavgift.medlemAvFolketrygden = medlemAvFolketrygden
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.shouldNotBeEmpty()
        trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID)
            .shouldNotBeNull()
            .shouldBeEmpty()
        medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.shouldBeEmpty()
    }

    @Test
    fun beregnTrygdeavgift_manglerMedlemskapsperioder_kasterFeil() {
        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID)
        }.message.shouldContain("Kan ikke beregne trygdeavgift uten medlemskapsperioder")
    }

    @Test
    fun beregnTrygdeavgift_manglerFastsattTrygdeavgift_kasterFeil() {
        medlemAvFolketrygden.medlemskapsperioder.add(Medlemskapsperiode())

        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID)
        }.message.shouldContain("Kan ikke beregne trygdeavgift uten fastsattTrygdeavgift")
    }

    @Test
    fun beregnTrygdeavgift_manglerTrygdeavgiftsgrunnlag_kasterFeil() {
        medlemAvFolketrygden.medlemskapsperioder.add(Medlemskapsperiode())
        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift()

        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID)
        }.message.shouldContain("Kan ikke beregne trygdeavgift uten trygdeavgiftsgrunnlag")
    }

    @Test
    fun beregnTrygdeavgift_manglerSkatteforholdINorge_kasterFeil() {
        medlemAvFolketrygden.medlemskapsperioder.add(Medlemskapsperiode())
        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag()
        }

        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID)
        }.message.shouldContain("Kan ikke beregne trygdeavgift uten skatteforholdTilNorge")
    }

    @Test
    fun beregnTrygdeavgift_manglerInntektsperioder_kasterFeil() {
        medlemAvFolketrygden.medlemskapsperioder.add(Medlemskapsperiode())
        medlemAvFolketrygden.fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
            trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                skatteforholdTilNorge.add(SkatteforholdTilNorge())
            }
        }

        shouldThrow<FunksjonellException> {
            trygdeavgiftsberegningService.beregnOgLagreTrygdeavgift(BEHANDLING_ID)
        }.message.shouldContain("Kan ikke beregne trygdeavgift uten inntektsperioder")
    }

    @Test
    fun finnFakturamottaker_harIkkeFullmektig_mottakerErBruker() {
        behandling.apply {
            fagsak = Fagsak().apply {
                aktører = setOf(
                    Aktoer().apply {
                        aktørId = BRUKER_AKTØR_ID
                        rolle = Aktoersroller.BRUKER
                    }
                )
            }
        }
        trygdeavgiftsberegningService.finnFakturamottakerNavn(BEHANDLING_ID).shouldBe(BRUKER_NAVN)
    }

    @Test
    fun finnFakturamottaker_harFullmektigPersonForTrygdeavgift_mottakerErFullmektigPerson() {
        behandling.apply {
            fagsak = Fagsak().apply {
                aktører = setOf(
                    Aktoer().apply {
                        aktørId = BRUKER_AKTØR_ID
                        rolle = Aktoersroller.BRUKER
                    },
                    Aktoer().apply {
                        rolle = Aktoersroller.FULLMEKTIG
                        personIdent = FULLMEKTIG_AKTØR_ID
                        fullmakter = setOf(Fullmakt().apply { type = Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT })
                    })
            }
        }
        trygdeavgiftsberegningService.finnFakturamottakerNavn(BEHANDLING_ID).shouldBe(FULLMEKTIG_NAVN)
    }

    @Test
    fun finnFakturamottaker_harFullmektigOrgForTrygdeavgift_mottakerErFullmektigOrg() {
        behandling.apply {
            fagsak = Fagsak().apply {
                aktører = setOf(
                    Aktoer().apply {
                        aktørId = BRUKER_AKTØR_ID
                        rolle = Aktoersroller.BRUKER
                    },
                    Aktoer().apply {
                        orgnr = FULLMEKTIG_ORGNR
                        rolle = Aktoersroller.FULLMEKTIG
                        fullmakter = setOf(Fullmakt().apply { type = Fullmaktstype.FULLMEKTIG_TRYGDEAVGIFT })
                    })
            }
        }
        trygdeavgiftsberegningService.finnFakturamottakerNavn(BEHANDLING_ID).shouldBe(FULLMEKTIG_ORG_NAVN)
    }

    @Test
    fun finnFakturamottaker_harFullmektigMenIkkeForTrygdeavgift_brukerErFullmektig() {
        behandling.apply {
            fagsak = Fagsak().apply {
                aktører = setOf(
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
            }
        }
        trygdeavgiftsberegningService.finnFakturamottakerNavn(BEHANDLING_ID).shouldBe(BRUKER_NAVN)
    }
}
