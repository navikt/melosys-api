package no.nav.melosys.service.avgift

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.*
import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.avgift.dto.InntektskildeRequest
import no.nav.melosys.service.avgift.dto.OppdaterTrygdeavgiftsgrunnlagRequest
import no.nav.melosys.service.avgift.dto.SkatteforholdTilNorgeRequest
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class TrygdeavgiftsgrunnlagServiceTest {

    @MockK
    private lateinit var mockBehandlingsresultatService: BehandlingsresultatService
    @MockK
    private lateinit var mockTrygdeavgiftMottakerService: TrygdeavgiftMottakerService

    private lateinit var trygdeavgiftsgrunnlagService: TrygdeavgiftsgrunnlagService

    private lateinit var behandlingsresultat: Behandlingsresultat
    private lateinit var opprinneligBehandlingsresultat: Behandlingsresultat
    private val slotBehandlingsresultat = slot<Behandlingsresultat>()
    private val BEHANDLING_ID: Long = 1291
    private val OPPRINNELIG_BEHANDLING_ID: Long = 1292


    @BeforeEach
    fun setup() {
        trygdeavgiftsgrunnlagService = TrygdeavgiftsgrunnlagService(mockBehandlingsresultatService, mockTrygdeavgiftMottakerService)
        behandlingsresultat = Behandlingsresultat()
        opprinneligBehandlingsresultat = Behandlingsresultat()
        every { mockBehandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) }.returns(behandlingsresultat)
        every { mockBehandlingsresultatService.hentBehandlingsresultat(OPPRINNELIG_BEHANDLING_ID) }.returns(opprinneligBehandlingsresultat)
    }

    @Test
    fun hentTrygdeavgiftsgrunnlag_ingenGrunnlag_returnerNull() {
        behandlingsresultat.behandling = Behandling().apply {
            type = Behandlingstyper.FØRSTEGANG
        }
        trygdeavgiftsgrunnlagService.hentTrygdeavgiftsgrunnlag(BEHANDLING_ID).shouldBeNull()
    }

    @Test
    fun hentTrygdeavgiftsgrunnlagEllerOpprinneligTrygdeavgiftsgrunnlag_ingenGrunnlag_returnerNull() {
        behandlingsresultat.behandling = Behandling().apply {
            type = Behandlingstyper.FØRSTEGANG
        }
        trygdeavgiftsgrunnlagService.hentTrygdeavgiftsgrunnlagEllerOpprinneligTrygdeavgiftsgrunnlag(BEHANDLING_ID).shouldBeNull()
    }

    @Test
    fun hentTrygdeavgiftsgrunnlagEllerOpprinneligTrygdeavgiftsgrunnlag_ingenGrunnlag_nyVurdering_lagrerOgReturnererGammeltGrunnlag() {
        val nå = LocalDate.now()
        every { mockBehandlingsresultatService.lagre(any()) } returnsArgument 0
        behandlingsresultat.apply {
            behandling = Behandling().apply {
                id = BEHANDLING_ID
                type = Behandlingstyper.NY_VURDERING
                opprinneligBehandling = Behandling().apply { id = OPPRINNELIG_BEHANDLING_ID }
            }
            medlemAvFolketrygden = MedlemAvFolketrygden()
        }
        opprinneligBehandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
                trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                    id = 10
                    skatteforholdTilNorge = mutableListOf(SkatteforholdTilNorge().apply {
                        id = 10
                        fomDato = nå
                        tomDato = nå
                        skatteplikttype = Skatteplikttype.SKATTEPLIKTIG
                    })
                    inntektsperioder = mutableListOf(Inntektsperiode().apply {
                        id = 10
                        fomDato = nå
                        tomDato = nå
                        type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                    })
                }
            }
        }


        trygdeavgiftsgrunnlagService.hentTrygdeavgiftsgrunnlagEllerOpprinneligTrygdeavgiftsgrunnlag(BEHANDLING_ID)


        verify { mockBehandlingsresultatService.lagre(capture(slotBehandlingsresultat)) }
        slotBehandlingsresultat.captured.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag?.run {
            id.shouldNotBe(10)
            skatteforholdTilNorge.shouldNotBeEmpty().first().run {
                id.shouldNotBe(10)
                fomDato.shouldBe(nå)
                tomDato.shouldBe(nå)
                skatteplikttype.shouldBe(Skatteplikttype.SKATTEPLIKTIG)
            }
            inntektsperioder.shouldNotBeEmpty().first().run {
                id.shouldNotBe(10)
                fomDato.shouldBe(nå)
                tomDato.shouldBe(nå)
                type.shouldBe(Inntektskildetype.INNTEKT_FRA_UTLANDET)
            }
        }
    }

    @Test
    fun hentTrygdeavgiftsgrunnlagEllerOpprinneligTrygdeavgiftsgrunnlag_ingenGrunnlag_nyVurdering_harIkkeMedlemAvFolketrygdenEnda_returnererGammeltGrunnlag() {
        behandlingsresultat.behandling = Behandling().apply {
            type = Behandlingstyper.NY_VURDERING
            opprinneligBehandling = Behandling().apply { id = OPPRINNELIG_BEHANDLING_ID }
        }
        opprinneligBehandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
                trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                    id = 10
                }
            }
        }


        val trygdeavgiftsgrunnlag = trygdeavgiftsgrunnlagService.hentTrygdeavgiftsgrunnlagEllerOpprinneligTrygdeavgiftsgrunnlag(BEHANDLING_ID)


        assertEquals(10, trygdeavgiftsgrunnlag?.id)
        verify(exactly = 0) { mockBehandlingsresultatService.lagre(any()) }
    }

    @Test
    fun hentTrygdeavgiftsgrunnlagEllerOpprinneligTrygdeavgiftsgrunnlag_ingenGrunnlag_manglendeInnbetalingTrygdeavgift_harIkkeMedlemAvFolketrygdenEnda_returnererGammeltGrunnlag() {
        behandlingsresultat.behandling = Behandling().apply {
            type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
            opprinneligBehandling = Behandling().apply { id = OPPRINNELIG_BEHANDLING_ID }
        }
        opprinneligBehandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
                trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                    id = 10
                }
            }
        }


        val trygdeavgiftsgrunnlag = trygdeavgiftsgrunnlagService.hentTrygdeavgiftsgrunnlagEllerOpprinneligTrygdeavgiftsgrunnlag(BEHANDLING_ID)


        assertEquals(10, trygdeavgiftsgrunnlag?.id)
        verify(exactly = 0) { mockBehandlingsresultatService.lagre(any()) }
    }

    @Test
    fun hentTrygdeavgiftsgrunnlagEllerOpprinneligTrygdeavgiftsgrunnlag_grunnlagFinnes_nyVurdering_returnererNyttGrunnlag() {
        behandlingsresultat.behandling = Behandling().apply {
            type = Behandlingstyper.NY_VURDERING
            opprinneligBehandling = Behandling().apply { id = OPPRINNELIG_BEHANDLING_ID }
        }
        behandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
                trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                    id = 50
                }
            }
        }
        opprinneligBehandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
                trygdeavgiftsgrunnlag = Trygdeavgiftsgrunnlag().apply {
                    id = 10
                }
            }
        }


        val trygdeavgiftsgrunnlag = trygdeavgiftsgrunnlagService.hentTrygdeavgiftsgrunnlagEllerOpprinneligTrygdeavgiftsgrunnlag(BEHANDLING_ID)


        assertEquals(50, trygdeavgiftsgrunnlag?.id)
        verify(exactly = 0) { mockBehandlingsresultatService.lagre(any()) }
    }

    @Test
    fun oppdaterTrygdeavgiftsgrunnlag_ingenMedlemskapsperioder_kasterFeil() {
        behandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply { medlemskapsperioder = emptyList() }


        shouldThrow<FunksjonellException> {
            trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
                BEHANDLING_ID, OppdaterTrygdeavgiftsgrunnlagRequest(emptyList(), emptyList())
            )
        }.message.shouldContain("Kan ikke oppdatere trygdeavgiftsgrunnlaget før medlemskapsperioder finnes")
    }


    @Test
    fun oppdaterTrygdeavgiftsgrunnlag_FTRL_2_2_1_åpenSluttdato_erGyldig() {
        val fomDato = LocalDate.now().minusMonths(1);

        behandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = listOf(Medlemskapsperiode()
                .apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = fomDato
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                })
        }
        every { mockBehandlingsresultatService.lagre(any()) } returnsArgument 0
        every { mockTrygdeavgiftMottakerService.getTrygdeavgiftMottaker(any()) } returns Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT

        trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
            BEHANDLING_ID, OppdaterTrygdeavgiftsgrunnlagRequest(listOf(lagSkatteforholdTilNorge(fomDato, null)), listOf(lagInntektsperiode(fomDato, null)))
        )

        verify { mockBehandlingsresultatService.lagre(capture(slotBehandlingsresultat)) }
        slotBehandlingsresultat.captured.shouldNotBeNull()
        slotBehandlingsresultat.captured.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.shouldBeEmpty()
    }

    @Test
    fun oppdaterTrygdeavgiftsgrunnlag_FTRL_2_2_1_åpenSluttdatoMedlemskapsperiodeOgLukketInntektsperiode_kasterFeil() {
        val fomDato = LocalDate.now().minusMonths(1);
        val tomDato = LocalDate.now().plusMonths(1);

        behandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = listOf(Medlemskapsperiode()
                .apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = fomDato
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                })
        }
        every { mockBehandlingsresultatService.lagre(any()) } returnsArgument 0
        every { mockTrygdeavgiftMottakerService.getTrygdeavgiftMottaker(any()) } returns Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT

        shouldThrow<FunksjonellException> {
            trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
                BEHANDLING_ID, OppdaterTrygdeavgiftsgrunnlagRequest(listOf(lagSkatteforholdTilNorge(fomDato, null)), listOf(lagInntektsperiode(fomDato, tomDato)))
            )
        }.message.shouldContain("Skatteforholdsperiode må ha åpen sluttdato når medlemskapsperiode har åpen sluttdato")
    }

    @Test
    fun oppdaterTrygdeavgiftsgrunnlag_FTRL_2_2_1_lukketSluttdatoMedlemskapsperiodeOgLukketInntektsperiode_kasterFeil() {
        val fomDato = LocalDate.now().minusMonths(1);
        val tomDato = LocalDate.now().plusMonths(1);

        behandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = listOf(Medlemskapsperiode()
                .apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = fomDato
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                })
        }
        every { mockBehandlingsresultatService.lagre(any()) } returnsArgument 0
        every { mockTrygdeavgiftMottakerService.getTrygdeavgiftMottaker(any()) } returns Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT

        shouldThrow<FunksjonellException> {
            trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
                BEHANDLING_ID, OppdaterTrygdeavgiftsgrunnlagRequest(listOf(lagSkatteforholdTilNorge(fomDato, tomDato)), listOf(lagInntektsperiode(fomDato, tomDato)))
            )
        }.message.shouldContain("Skatteforholdsperiode må ha åpen sluttdato når medlemskapsperiode har åpen sluttdato")
    }

    @Test
    fun oppdaterTrygdeavgiftsgrunnlag_FTRL_2_2_1_lukketSluttdatoMedlemskapsperiodeOgÅpenInntektsperiode_kasterFeil() {
        val fomDato = LocalDate.now().minusMonths(1);
        val tomDato = LocalDate.now().plusMonths(1);

        behandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = listOf(Medlemskapsperiode()
                .apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = fomDato
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                })
        }
        every { mockBehandlingsresultatService.lagre(any()) } returnsArgument 0
        every { mockTrygdeavgiftMottakerService.getTrygdeavgiftMottaker(any()) } returns Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT

        shouldThrow<FunksjonellException> {
            trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
                BEHANDLING_ID, OppdaterTrygdeavgiftsgrunnlagRequest(listOf(lagSkatteforholdTilNorge(fomDato, tomDato)), listOf(lagInntektsperiode(fomDato, null)))
            )
        }.message.shouldContain("Skatteforholdsperiode må ha åpen sluttdato når medlemskapsperiode har åpen sluttdato")
    }

    @Test
    fun oppdaterTrygdeavgiftsgrunnlag_FTRL_2_2_1_åpenSluttdatoMedTRYGDEAVGIFT_BETALES_TIL_NAV_kasterFeil() {
        val fomDato = LocalDate.now().minusMonths(1);

        behandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = listOf(Medlemskapsperiode()
                .apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = fomDato
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                })
        }
        every { mockBehandlingsresultatService.lagre(any()) } returnsArgument 0
        every { mockTrygdeavgiftMottakerService.getTrygdeavgiftMottaker(any()) } returns Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV


        shouldThrow<FunksjonellException> {
            trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
                BEHANDLING_ID, OppdaterTrygdeavgiftsgrunnlagRequest(listOf(lagSkatteforholdTilNorge(fomDato, null)), listOf(lagInntektsperiode(fomDato, null)))
            )
        }.message.shouldContain("Faktura kan ikke opprettes for medlemskapsperiode med åpen sluttdato. Angi sluttdato på medlemskapsperiode")
    }

    @Test
    fun oppdaterTrygdeavgiftsgrunnlag_FTRL_2_2_1_åpenSluttdatoMedTRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT_kasterFeil() {
        val fomDato = LocalDate.now().minusMonths(1);

        behandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = listOf(Medlemskapsperiode()
                .apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = fomDato
                    bestemmelse = Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_1
                })
        }
        every { mockBehandlingsresultatService.lagre(any()) } returnsArgument 0
        every { mockTrygdeavgiftMottakerService.getTrygdeavgiftMottaker(any()) } returns Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT


        shouldThrow<FunksjonellException> {
            trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
                BEHANDLING_ID, OppdaterTrygdeavgiftsgrunnlagRequest(listOf(lagSkatteforholdTilNorge(fomDato, null)), listOf(lagInntektsperiode(fomDato, null)))
            )
        }.message.shouldContain("Faktura kan ikke opprettes for medlemskapsperiode med åpen sluttdato. Angi sluttdato på medlemskapsperiode")
    }

    @Test
    fun oppdaterTrygdeavgiftsgrunnlag_ingenMedlemskapsperioderInnvilget_kasterFeil() {
        behandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = listOf(Medlemskapsperiode()
                .apply { innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT })
        }


        shouldThrow<FunksjonellException> {
            trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
                BEHANDLING_ID, OppdaterTrygdeavgiftsgrunnlagRequest(emptyList(), emptyList())
            )
        }.message.shouldContain("Klarte ikke finne startdatoen på medlemskapet")
    }

    @Test
    fun oppdaterTrygdeavgiftsgrunnlag_medlemskapsperiodeÅpenSluttdato_kasterFeil() {
        behandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = listOf(Medlemskapsperiode()
                .apply {
                    innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                    fom = LocalDate.now()
                })
        }


        shouldThrow<FunksjonellException> {
            trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
                BEHANDLING_ID, OppdaterTrygdeavgiftsgrunnlagRequest(emptyList(), emptyList())
            )
        }.message.shouldContain("Klarte ikke finne sluttdatoen på medlemskapet")
    }

    @Test
    fun oppdaterTrygdeavgiftsgrunnlag_eksistererBeregnetTrygdeavgift_sletterEksisterendeBeregning() {
        val fom = LocalDate.now().minusMonths(1);
        val tom = LocalDate.now().plusMonths(3);
        every { mockBehandlingsresultatService.lagre(any()) } returnsArgument 0
        behandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                this.fom = fom
                this.tom = tom
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
            })
            fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
                trygdeavgiftsperioder = mutableSetOf(Trygdeavgiftsperiode())
            }
        }
        behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.shouldNotBeEmpty()


        trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
            BEHANDLING_ID, OppdaterTrygdeavgiftsgrunnlagRequest(listOf(lagSkatteforholdTilNorge(fom, tom)), listOf(lagInntektsperiode(fom, tom)))
        )


        verify { mockBehandlingsresultatService.lagre(capture(slotBehandlingsresultat)) }
        slotBehandlingsresultat.captured.shouldNotBeNull()
        slotBehandlingsresultat.captured.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.shouldBeEmpty()
    }

    @Test
    fun oppdaterTrygdeavgiftsgrunnlag_inntektsperioderDekkerIkkeHelePerioden_kasterFeil() {
        val fom = LocalDate.now().minusMonths(1);
        val tom = LocalDate.now().plusMonths(3);
        every { mockBehandlingsresultatService.lagre(any()) } returnsArgument 0
        behandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                this.fom = fom
                this.tom = tom
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                medlemskapstype = Medlemskapstyper.FRIVILLIG
            })
            fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
                trygdeavgiftsperioder = mutableSetOf(Trygdeavgiftsperiode())
            }
        }
        behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.shouldNotBeEmpty()


        shouldThrow<FunksjonellException> {
            trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
                BEHANDLING_ID,
                OppdaterTrygdeavgiftsgrunnlagRequest(listOf(lagSkatteforholdTilNorge(fom, tom)), listOf(lagInntektsperiode(fom, tom.minusDays(1))))
            )
        }.message.shouldContain("Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

    @Test
    fun oppdaterTrygdeavgiftsgrunnlag_skatteforholdDekkerIkkeHelePerioden_kasterFeil() {
        val fom = LocalDate.now().minusMonths(1);
        val tom = LocalDate.now().plusMonths(3);
        every { mockBehandlingsresultatService.lagre(any()) } returnsArgument 0
        behandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                this.fom = fom
                this.tom = tom
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            })
            fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
                trygdeavgiftsperioder = mutableSetOf(Trygdeavgiftsperiode())
            }
        }
        behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.shouldNotBeEmpty()


        shouldThrow<FunksjonellException> {
            trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
                BEHANDLING_ID,
                OppdaterTrygdeavgiftsgrunnlagRequest(listOf(lagSkatteforholdTilNorge(fom, tom.minusDays(1))), listOf(lagInntektsperiode(fom, tom)))
            )
        }.message.shouldContain("Skatteforholdsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

    @Test
    fun oppdaterTrygdeavgiftsgrunnlag_skatteforholdOverlapper_kasterFeil() {
        val fom = LocalDate.now().minusMonths(1);
        val tom = LocalDate.now().plusMonths(3);
        every { mockBehandlingsresultatService.lagre(any()) } returnsArgument 0
        behandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                this.fom = fom
                this.tom = tom
                innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
            })
            fastsattTrygdeavgift = FastsattTrygdeavgift().apply {
                trygdeavgiftsperioder = mutableSetOf(Trygdeavgiftsperiode())
            }
        }
        behandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.shouldNotBeEmpty()


        shouldThrow<FunksjonellException> {
            trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
                BEHANDLING_ID,
                OppdaterTrygdeavgiftsgrunnlagRequest(
                    listOf(lagSkatteforholdTilNorge(fom, tom), lagSkatteforholdTilNorge(fom, tom)),
                    listOf(lagInntektsperiode(fom, tom))
                )
            )
        }.message.shouldContain("Skatteforholdsperiodene kan ikke overlappe")
    }

    @Test
    fun oppdaterTrygdeavgiftsgrunnlag_requestMedSkattepliktOgInntektskilder_lagrerAltKorrekt() {
        val fom = LocalDate.now().minusMonths(1)
        val tom = LocalDate.now().plusMonths(3)
        every { mockBehandlingsresultatService.lagre(any()) } returnsArgument 0
        behandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                this.fom = fom
                this.tom = tom
                this.innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                this.medlemskapstype = Medlemskapstyper.FRIVILLIG
            })
        }


        trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
            BEHANDLING_ID, OppdaterTrygdeavgiftsgrunnlagRequest(
            listOf(SkatteforholdTilNorgeRequest(fom, tom, Skatteplikttype.SKATTEPLIKTIG)), listOf(
            InntektskildeRequest(Inntektskildetype.INNTEKT_FRA_UTLANDET, false, BigDecimal.valueOf(30000), fom, tom),
            InntektskildeRequest(Inntektskildetype.NÆRINGSINNTEKT_FRA_NORGE, false, BigDecimal.valueOf(10000), fom, tom),
            InntektskildeRequest(Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE, true, null, fom, tom)
        )
        )
        )


        verify(exactly = 1) { mockBehandlingsresultatService.lagre(capture(slotBehandlingsresultat)) }
        val lagretBehandlingsresultat = slotBehandlingsresultat.captured
        lagretBehandlingsresultat.shouldNotBeNull().medlemAvFolketrygden.shouldNotBeNull().fastsattTrygdeavgift.shouldNotBeNull().trygdeavgiftsgrunnlag.shouldNotBeNull()
        lagretBehandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.shouldBeEmpty()
        lagretBehandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag
            .skatteforholdTilNorge.shouldHaveSize(1).first().skatteplikttype.shouldBe(Skatteplikttype.SKATTEPLIKTIG)
        lagretBehandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag
            .inntektsperioder.shouldHaveSize(3).shouldContainExactlyInAnyOrder(
                Inntektsperiode().apply {
                    fomDato = fom
                    tomDato = tom
                    type = Inntektskildetype.INNTEKT_FRA_UTLANDET
                    isArbeidsgiversavgiftBetalesTilSkatt = false
                    avgiftspliktigInntektMnd = Penger(BigDecimal.valueOf(30000))
                    trygdeavgiftsgrunnlag =
                        lagretBehandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag
                },
                Inntektsperiode().apply {
                    fomDato = fom
                    tomDato = tom
                    type = Inntektskildetype.NÆRINGSINNTEKT_FRA_NORGE
                    isArbeidsgiversavgiftBetalesTilSkatt = false
                    avgiftspliktigInntektMnd = Penger(BigDecimal.valueOf(10000))
                    trygdeavgiftsgrunnlag =
                        lagretBehandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag
                },
                Inntektsperiode().apply {
                    fomDato = fom
                    tomDato = tom
                    type = Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE
                    isArbeidsgiversavgiftBetalesTilSkatt = true
                    avgiftspliktigInntektMnd = null
                    trygdeavgiftsgrunnlag =
                        lagretBehandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag
                }
            )
    }

    @Test
    fun oppdaterTrygdeavgiftsgrunnlag_requestMedSkattepliktigPliktigMedlemskapOgTomInntektskilder_lagrerAltKorrekt() {
        val fom = LocalDate.now().minusMonths(1)
        val tom = LocalDate.now().plusMonths(3)
        every { mockBehandlingsresultatService.lagre(any()) } returnsArgument 0
        behandlingsresultat.medlemAvFolketrygden = MedlemAvFolketrygden().apply {
            medlemskapsperioder = listOf(Medlemskapsperiode().apply {
                this.fom = fom
                this.tom = tom
                this.innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
                this.medlemskapstype = Medlemskapstyper.PLIKTIG
            })
        }

        trygdeavgiftsgrunnlagService.oppdaterTrygdeavgiftsgrunnlag(
            BEHANDLING_ID, OppdaterTrygdeavgiftsgrunnlagRequest(
            listOf(SkatteforholdTilNorgeRequest(fom, tom, Skatteplikttype.SKATTEPLIKTIG)),
            emptyList()
        )
        )

        verify(exactly = 1) { mockBehandlingsresultatService.lagre(capture(slotBehandlingsresultat)) }
        val lagretBehandlingsresultat = slotBehandlingsresultat.captured
        lagretBehandlingsresultat.shouldNotBeNull().medlemAvFolketrygden.shouldNotBeNull().fastsattTrygdeavgift.shouldNotBeNull().trygdeavgiftsgrunnlag.shouldNotBeNull()
        lagretBehandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsperioder.shouldBeEmpty()
        lagretBehandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag
            .skatteforholdTilNorge.shouldHaveSize(1).first().skatteplikttype.shouldBe(Skatteplikttype.SKATTEPLIKTIG)
        lagretBehandlingsresultat.medlemAvFolketrygden.fastsattTrygdeavgift.trygdeavgiftsgrunnlag
            .inntektsperioder.shouldHaveSize(0)
    }
    // Tester valideringen:

    @Test
    fun `Inntektsperioder er uten opphold og starter slutter på samme dato som medlemskapsperioder - ok`() {
        val medlemskapsperioder: List<Medlemskapsperiode> = listOf(
            lagMedlemskapsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-31")),
            lagMedlemskapsperiode(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28")),
            lagMedlemskapsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        val inntektsperioder: List<InntektskildeRequest> = listOf(
            lagInntektsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-15")),
            lagInntektsperiode(LocalDate.parse("2023-01-10"), LocalDate.parse("2023-02-28")),
            lagInntektsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        shouldNotThrowAny {
            TrygdeavgiftsgrunnlagService.validerAtInntekstperioderDekkerInnvilgedeMedlemskapsperioder(
                inntektsperioder,
                medlemskapsperioder
            )
        }
    }

    @Test
    fun `Inntektsperioder er uten opphold og slutter ikke på samme dato som medlemskapsperioder - false`() {
        val medlemskapsperioder: List<Medlemskapsperiode> = listOf(
            lagMedlemskapsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-31")),
            lagMedlemskapsperiode(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28")),
            lagMedlemskapsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        val inntektsperioder: List<InntektskildeRequest> = listOf(
            lagInntektsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-15")),
            lagInntektsperiode(LocalDate.parse("2023-01-10"), LocalDate.parse("2023-02-28")),
            lagInntektsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-05"))
        )

        shouldThrow<FunksjonellException> {
            TrygdeavgiftsgrunnlagService.validerAtInntekstperioderDekkerInnvilgedeMedlemskapsperioder(
                inntektsperioder,
                medlemskapsperioder
            )
        }.message.shouldContain("Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

    @Test
    fun `Inntektsperioder er uten opphold og starter ikke på samme dato som medlemskapsperioder - false`() {
        val medlemskapsperioder: List<Medlemskapsperiode> = listOf(
            lagMedlemskapsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-31")),
            lagMedlemskapsperiode(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28")),
            lagMedlemskapsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        val inntektsperioder: List<InntektskildeRequest> = listOf(
            lagInntektsperiode(LocalDate.parse("2023-01-03"), LocalDate.parse("2023-01-15")),
            lagInntektsperiode(LocalDate.parse("2023-01-10"), LocalDate.parse("2023-02-28")),
            lagInntektsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        shouldThrow<FunksjonellException> {
            TrygdeavgiftsgrunnlagService.validerAtInntekstperioderDekkerInnvilgedeMedlemskapsperioder(
                inntektsperioder,
                medlemskapsperioder
            )
        }.message.shouldContain("Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

    @Test
    fun `Inntektsperioder har et opphold - false`() {
        val medlemskapsperioder: List<Medlemskapsperiode> = listOf(
            lagMedlemskapsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-31")),
            lagMedlemskapsperiode(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28")),
            lagMedlemskapsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        val inntektsperioder: List<InntektskildeRequest> = listOf(
            lagInntektsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-15")),
            lagInntektsperiode(LocalDate.parse("2023-01-10"), LocalDate.parse("2023-02-20")),
            lagInntektsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        shouldThrow<FunksjonellException> {
            TrygdeavgiftsgrunnlagService.validerAtInntekstperioderDekkerInnvilgedeMedlemskapsperioder(
                inntektsperioder,
                medlemskapsperioder
            )
        }.message.shouldContain("Inntektsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

    @Test
    fun `Inntektsperioder har flere med samme fom dato uten opphold- ok`() {
        val medlemskapsperioder: List<Medlemskapsperiode> = listOf(
            lagMedlemskapsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-31")),
            lagMedlemskapsperiode(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28")),
            lagMedlemskapsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        val inntektsperioder: List<InntektskildeRequest> = listOf(
            lagInntektsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-02-28")),
            lagInntektsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-15")),
            lagInntektsperiode(LocalDate.parse("2023-01-14"), LocalDate.parse("2023-02-20")),
            lagInntektsperiode(LocalDate.parse("2023-02-22"), LocalDate.parse("2023-05-31")),
        )

        TrygdeavgiftsgrunnlagService.validerAtInntekstperioderDekkerInnvilgedeMedlemskapsperioder(
            inntektsperioder,
            medlemskapsperioder
        )
    }

    @Test
    fun `SkatteforholdTilNorge er uten opphold og starter slutter på samme dato som medlemskapsperioder - ok`() {
        val medlemskapsperioder: List<Medlemskapsperiode> = listOf(
            lagMedlemskapsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-31")),
            lagMedlemskapsperiode(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28")),
            lagMedlemskapsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        val skatteforholdTilNorge: List<SkatteforholdTilNorgeRequest> = listOf(
            lagSkatteforholdTilNorge(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-15")),
            lagSkatteforholdTilNorge(LocalDate.parse("2023-01-16"), LocalDate.parse("2023-02-28")),
            lagSkatteforholdTilNorge(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        shouldNotThrowAny {
            TrygdeavgiftsgrunnlagService.validerAtSkatteforholdTilNorgeDekkerInnvilgedeMedlemskapsperioderOgOverlapperIkke(
                skatteforholdTilNorge,
                medlemskapsperioder
            )
        }
    }

    @Test
    fun `SkatteforholdTilNorge er uten opphold og slutter ikke på samme dato som medlemskapsperioder - false`() {
        val medlemskapsperioder: List<Medlemskapsperiode> = listOf(
            lagMedlemskapsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-31")),
            lagMedlemskapsperiode(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28")),
            lagMedlemskapsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        val skatteforholdTilNorge: List<SkatteforholdTilNorgeRequest> = listOf(
            lagSkatteforholdTilNorge(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-15")),
            lagSkatteforholdTilNorge(LocalDate.parse("2023-01-16"), LocalDate.parse("2023-02-28")),
            lagSkatteforholdTilNorge(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-05"))
        )

        shouldThrow<FunksjonellException> {
            TrygdeavgiftsgrunnlagService.validerAtSkatteforholdTilNorgeDekkerInnvilgedeMedlemskapsperioderOgOverlapperIkke(
                skatteforholdTilNorge,
                medlemskapsperioder
            )
        }.message.shouldContain("Skatteforholdsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

    @Test
    fun `SkatteforholdTilNorge er uten opphold og starter ikke på samme dato som medlemskapsperioder - false`() {
        val medlemskapsperioder: List<Medlemskapsperiode> = listOf(
            lagMedlemskapsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-31")),
            lagMedlemskapsperiode(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28")),
            lagMedlemskapsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        val skatteforholdTilNorge: List<SkatteforholdTilNorgeRequest> = listOf(
            lagSkatteforholdTilNorge(LocalDate.parse("2023-01-03"), LocalDate.parse("2023-01-15")),
            lagSkatteforholdTilNorge(LocalDate.parse("2023-01-16"), LocalDate.parse("2023-02-28")),
            lagSkatteforholdTilNorge(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        shouldThrow<FunksjonellException> {
            TrygdeavgiftsgrunnlagService.validerAtSkatteforholdTilNorgeDekkerInnvilgedeMedlemskapsperioderOgOverlapperIkke(
                skatteforholdTilNorge,
                medlemskapsperioder
            )
        }.message.shouldContain("Skatteforholdsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

    @Test
    fun `SkatteforholdTilNorge har et opphold - false`() {
        val medlemskapsperioder: List<Medlemskapsperiode> = listOf(
            lagMedlemskapsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-31")),
            lagMedlemskapsperiode(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28")),
            lagMedlemskapsperiode(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        val skatteforholdTilNorge: List<SkatteforholdTilNorgeRequest> = listOf(
            lagSkatteforholdTilNorge(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-15")),
            lagSkatteforholdTilNorge(LocalDate.parse("2023-01-16"), LocalDate.parse("2023-02-20")),
            lagSkatteforholdTilNorge(LocalDate.parse("2023-03-01"), LocalDate.parse("2023-05-31"))
        )

        shouldThrow<FunksjonellException> {
            TrygdeavgiftsgrunnlagService.validerAtSkatteforholdTilNorgeDekkerInnvilgedeMedlemskapsperioderOgOverlapperIkke(
                skatteforholdTilNorge,
                medlemskapsperioder
            )
        }.message.shouldContain("Skatteforholdsperioden(e) du har lagt inn dekker ikke hele medlemskapsperioden(e)")
    }

    @Test
    fun `SkatteforholdTilNorge har to perioder som overlapper med 1 dag - false`() {
        val medlemskapsperioder: List<Medlemskapsperiode> = listOf(
            lagMedlemskapsperiode(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-01-31")),
            lagMedlemskapsperiode(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-05-31"))
        )

        val skatteforholdTilNorge: List<SkatteforholdTilNorgeRequest> = listOf(
            lagSkatteforholdTilNorge(LocalDate.parse("2023-01-01"), LocalDate.parse("2023-02-28")),
            lagSkatteforholdTilNorge(LocalDate.parse("2023-02-28"), LocalDate.parse("2023-05-31")),
        )

        shouldThrow<FunksjonellException> {
            TrygdeavgiftsgrunnlagService.validerAtSkatteforholdTilNorgeDekkerInnvilgedeMedlemskapsperioderOgOverlapperIkke(
                skatteforholdTilNorge,
                medlemskapsperioder
            )
        }.message.shouldContain("Skatteforholdsperiodene kan ikke overlappe")
    }

    private fun lagMedlemskapsperiode(fom: LocalDate, tom: LocalDate): Medlemskapsperiode {
        return Medlemskapsperiode().apply {
            this.fom = fom
            this.tom = tom
        }
    }

    private fun lagInntektsperiode(fom: LocalDate, tom: LocalDate?): InntektskildeRequest {
        return InntektskildeRequest(Inntektskildetype.ARBEIDSINNTEKT_FRA_NORGE, false, null, fom, tom)
    }

    private fun lagSkatteforholdTilNorge(fom: LocalDate, tom: LocalDate?): SkatteforholdTilNorgeRequest {
        return SkatteforholdTilNorgeRequest(fom, tom, Skatteplikttype.SKATTEPLIKTIG)
    }
}
