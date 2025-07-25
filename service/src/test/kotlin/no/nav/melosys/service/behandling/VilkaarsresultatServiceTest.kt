package no.nav.melosys.service.behandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.Forutgaaende_medl_begrunnelser
import no.nav.melosys.domain.kodeverk.begrunnelser.Utsendt_arbeidstaker_begrunnelser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.repository.BehandlingsresultatRepository
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import no.nav.melosys.service.vilkaar.VilkaarDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*


@ExtendWith(MockKExtension::class)
class VilkaarsresultatServiceTest {
    @RelaxedMockK
    private lateinit var saksbehandlingRegler: SaksbehandlingRegler

    @MockK
    private lateinit var behandlingsresultatRepo: BehandlingsresultatRepository

    private lateinit var vilkaarsresultatService: VilkaarsresultatService

    @BeforeEach
    fun setUp() {
        vilkaarsresultatService = VilkaarsresultatService(behandlingsresultatRepo, saksbehandlingRegler)
    }

    @Test
    fun hentVilkaar() {
        val behandlingsresultat = lagBehandlingsresultat().apply {
            vilkaarsresultater = setOf(
                Vilkaarsresultat().apply {
                    vilkaar = Vilkaar.FORUTGAAENDE_MEDLEMSKAP
                    isOppfylt = true
                    begrunnelser =
                        setOf(VilkaarBegrunnelse().apply { kode = Forutgaaende_medl_begrunnelser.IKKE_FOLKEREGISTRERT_ELLER_ARBEIDET_I_NORGE.kode })
                    begrunnelseFritekst = "begrunnelse"
                    begrunnelseFritekstEessi = "kommer ikke på hva begrunnelse er på engelsk"
                }
            )
        }
        every { behandlingsresultatRepo.findById(BEHANDLING_ID) } returns Optional.of(behandlingsresultat)


        val response = vilkaarsresultatService.hentVilkaar(BEHANDLING_ID)


        response
            .shouldNotBeNull()
            .shouldHaveSize(1)
            .single().run {
                vilkaar.shouldBe(Vilkaar.FORUTGAAENDE_MEDLEMSKAP.kode)
                begrunnelseKoder.shouldHaveSize(1).single().shouldBe(Forutgaaende_medl_begrunnelser.IKKE_FOLKEREGISTRERT_ELLER_ARBEIDET_I_NORGE.kode)
                begrunnelseFritekstEngelsk.shouldBe("kommer ikke på hva begrunnelse er på engelsk")
            }
    }

    @Test
    fun finnVilkaarsresultat_artikkel16_1_finnerVilkaarsresultat() {
        val behandlingsresultat = lagBehandlingsresultat().apply {
            vilkaarsresultater = setOf(Vilkaarsresultat().apply { vilkaar = Vilkaar.FO_883_2004_ART16_1 })
        }
        every { behandlingsresultatRepo.findById(BEHANDLING_ID) } returns Optional.of(behandlingsresultat)


        val response = vilkaarsresultatService.finnVilkaarsresultat(BEHANDLING_ID, Vilkaar.FO_883_2004_ART16_1)


        response.shouldNotBeNull().vilkaar.shouldBe(Vilkaar.FO_883_2004_ART16_1)
    }

    @Test
    fun finnUnntaksVilkaarsresultat_artikkel16_1_finnerVilkaarsresultat() {
        val behandlingsresultat = lagBehandlingsresultat().apply {
            vilkaarsresultater = setOf(Vilkaarsresultat().apply { vilkaar = Vilkaar.FO_883_2004_ART16_1 })
        }
        every { behandlingsresultatRepo.findById(BEHANDLING_ID) } returns Optional.of(behandlingsresultat)


        val response = vilkaarsresultatService.finnUnntaksVilkaarsresultat(BEHANDLING_ID)


        response.shouldNotBeNull().vilkaar.shouldBe(Vilkaar.FO_883_2004_ART16_1)
    }

    @Test
    fun finnUnntaksVilkaarsresultat_artikkel18_1_finnerVilkaarsresultat() {
        val behandlingsresultat = lagBehandlingsresultat().apply {
            vilkaarsresultater = setOf(Vilkaarsresultat().apply { vilkaar = Vilkaar.KONV_EFTA_STORBRITANNIA_ART18_1 })
        }
        every { behandlingsresultatRepo.findById(BEHANDLING_ID) } returns Optional.of(behandlingsresultat)


        val response = vilkaarsresultatService.finnUnntaksVilkaarsresultat(BEHANDLING_ID)


        response.shouldNotBeNull().vilkaar.shouldBe(Vilkaar.KONV_EFTA_STORBRITANNIA_ART18_1)
    }

    @Test
    fun finnUtsendingArbeidstakerVilkaarsresultat_artikkel12_1_finnerVilkaarsresultat() {
        val behandlingsresultat = lagBehandlingsresultat().apply {
            vilkaarsresultater = setOf(Vilkaarsresultat().apply { vilkaar = Vilkaar.FO_883_2004_ART12_1 })
        }
        every { behandlingsresultatRepo.findById(BEHANDLING_ID) } returns Optional.of(behandlingsresultat)


        val response = vilkaarsresultatService.finnUtsendingArbeidstakerVilkaarsresultat(BEHANDLING_ID)


        response.shouldNotBeNull().vilkaar.shouldBe(Vilkaar.FO_883_2004_ART12_1)
    }

    @Test
    fun finnUtsendingNæringsdrivendeVilkaarsresultat_artikkel12_2_finnerVilkaarsresultat() {
        val behandlingsresultat = lagBehandlingsresultat().apply {
            vilkaarsresultater = setOf(Vilkaarsresultat().apply { vilkaar = Vilkaar.FO_883_2004_ART12_2 })
        }
        every { behandlingsresultatRepo.findById(BEHANDLING_ID) } returns Optional.of(behandlingsresultat)


        val response = vilkaarsresultatService.finnUtsendingNæringsdrivendeVilkaarsresultat(BEHANDLING_ID)


        response.shouldNotBeNull().vilkaar.shouldBe(Vilkaar.FO_883_2004_ART12_2)
    }

    @Test
    fun registrerVilkår() {
        val behandlingsresultat = lagBehandlingsresultat()
        val vilkaarDto = VilkaarDto().apply {
            vilkaar = Vilkaar.FO_883_2004_ART12_1.kode
            begrunnelseKoder = setOf(Utsendt_arbeidstaker_begrunnelser.ERSTATTER_ANNEN.kode)
        }
        every { behandlingsresultatRepo.findById(BEHANDLING_ID) } returns Optional.of(behandlingsresultat)
        every { behandlingsresultatRepo.saveAndFlush(any()) } returnsArgument 0
        every { behandlingsresultatRepo.save(any()) } returnsArgument 0
        behandlingsresultat.vilkaarsresultater.shouldBeEmpty()


        vilkaarsresultatService.registrerVilkår(BEHANDLING_ID, listOf(vilkaarDto))


        verify { behandlingsresultatRepo.save(behandlingsresultat) }
        behandlingsresultat.vilkaarsresultater
            .shouldHaveSize(1)
            .single().run {
                vilkaar.shouldBe(Vilkaar.FO_883_2004_ART12_1)
                begrunnelser.shouldHaveSize(1).single().kode.shouldBe(Utsendt_arbeidstaker_begrunnelser.ERSTATTER_ANNEN.kode)
            }
    }

    @Test
    fun registrer_inngangsvilkår_feiler() {
        val vilkaarDto = VilkaarDto().apply { vilkaar = Vilkaar.FO_883_2004_INNGANGSVILKAAR.kode }

        shouldThrow<FunksjonellException> { vilkaarsresultatService.registrerVilkår(BEHANDLING_ID, listOf(vilkaarDto)) }
            .message.shouldBe("Kan ikke endre vilkår " + Vilkaar.FO_883_2004_INNGANGSVILKAAR)
    }

    @Test
    fun tilbakestillVilkårsresultatFraBehandlingsresultat_OgLagre_sakstypeIkkeEøs_sletterAlleVilkår() {
        val behandlingsresultat = Behandlingsresultat().apply {
            id = BEHANDLING_ID
            behandling = Behandling.buildForTest { fagsak = FagsakTestFactory.builder().type(Sakstyper.FTRL).build() }
            vilkaarsresultater = mutableSetOf(Vilkaarsresultat().apply { id = BEHANDLING_ID })
        }
        every { behandlingsresultatRepo.findById(BEHANDLING_ID) } returns Optional.of(behandlingsresultat)


        vilkaarsresultatService.tilbakestillVilkårsresultatFraBehandlingsresultat(behandlingsresultat)


        behandlingsresultat.vilkaarsresultater.shouldBeEmpty()
    }

    @Test
    fun tilbakestillVilkårsresultatFraBehandlingsresultat_OgLagre_sakstypeEøsMenIngenFlyt_sletterAlleVilkår() {
        val behandlingsresultat = Behandlingsresultat().apply {
            id = BEHANDLING_ID
            behandling = Behandling.buildForTest {
                id = BEHANDLING_ID
                fagsak = FagsakTestFactory.lagFagsak()
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.HENVENDELSE
            }
            vilkaarsresultater = mutableSetOf(Vilkaarsresultat().apply { id = BEHANDLING_ID })
        }
        every { saksbehandlingRegler.harIngenFlyt(behandlingsresultat.behandling) } returns true
        every { behandlingsresultatRepo.findById(BEHANDLING_ID) } returns Optional.of(behandlingsresultat)


        vilkaarsresultatService.tilbakestillVilkårsresultatFraBehandlingsresultat(behandlingsresultat)


        behandlingsresultat.vilkaarsresultater.shouldBeEmpty()
    }

    @Test
    fun tilbakestillVilkårsresultatFraBehandlingsresultat_OgLagre_sakstypeEøsOgHarFlyt_sletterIkkeInngangsvilkår() {
        val behandlingsresultat = Behandlingsresultat().apply {
            id = BEHANDLING_ID
            behandling = Behandling.buildForTest {
                id = BEHANDLING_ID
                fagsak = FagsakTestFactory.lagFagsak()
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.FØRSTEGANG
            }
            vilkaarsresultater = mutableSetOf(
                Vilkaarsresultat().apply {
                    id = BEHANDLING_ID
                    vilkaar = Vilkaar.FO_883_2004_INNGANGSVILKAAR
                },
                Vilkaarsresultat().apply {
                    id = 2L
                    vilkaar = Vilkaar.FO_883_2004_ART12_1
                }
            )
        }

        every { behandlingsresultatRepo.findById(BEHANDLING_ID) } returns Optional.of(behandlingsresultat)


        vilkaarsresultatService.tilbakestillVilkårsresultatFraBehandlingsresultat(behandlingsresultat)


        behandlingsresultat.vilkaarsresultater
            .shouldHaveSize(1)
            .single().run {
                id.shouldBe(BEHANDLING_ID)
                vilkaar.shouldBe(Vilkaar.FO_883_2004_INNGANGSVILKAAR)
            }
    }

    private fun lagBehandlingsresultat(): Behandlingsresultat =
        Behandlingsresultat().apply {
            id = BEHANDLING_ID
            behandling = Behandling.buildForTest {
                fagsak = FagsakTestFactory.lagFagsak()
            }
        }

    companion object {
        const val BEHANDLING_ID = 1L
    }
}

