package no.nav.melosys.service.behandling

import io.getunleash.FakeUnleash
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
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.FagsakTestFactory
import no.nav.melosys.domain.Vilkaarsresultat
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.Art12_1_begrunnelser
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
internal class VilkaarsresultatServiceTest {
    private val BEHANDLING_ID = 1L

    @RelaxedMockK
    private lateinit var saksbehandlingRegler: SaksbehandlingRegler

    @MockK
    private lateinit var behandlingsresultatRepo: BehandlingsresultatRepository

    private val unleash = FakeUnleash()

    private lateinit var vilkaarsresultatService: VilkaarsresultatService

    @BeforeEach
    fun setUp() {
        vilkaarsresultatService = VilkaarsresultatService(behandlingsresultatRepo, saksbehandlingRegler, unleash)
    }

    @Test
    fun hentVilkaar() {
        val behandlingsresultat = lagBehandlingsresultat().apply {
            vilkaarsresultater = setOf(
                Vilkaarsresultat().apply {
                    vilkaar = Vilkaar.ART12_1_FORUTGAAENDE_MEDLEMSKAP
                    isOppfylt = true
                    begrunnelser = setOf()
                    begrunnelseFritekst = "begrunnelse"
                }
            )
        }
        every { behandlingsresultatRepo.findById(BEHANDLING_ID) } returns Optional.of(behandlingsresultat)


        val response = vilkaarsresultatService.hentVilkaar(BEHANDLING_ID)


        response
            .shouldNotBeNull()
            .shouldHaveSize(1)
            .single().vilkaar.shouldBe(Vilkaar.ART12_1_FORUTGAAENDE_MEDLEMSKAP.kode)
    }

    @Test
    fun registrerVilkår() {
        val behandlingsresultat = lagBehandlingsresultat()
        val vilkaarDto = VilkaarDto().apply {
            vilkaar = Vilkaar.FO_883_2004_ART12_1.kode
            begrunnelseKoder = setOf(Art12_1_begrunnelser.ERSTATTER_ANNEN.kode)
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
                begrunnelser.shouldHaveSize(1).single().kode.shouldBe(Art12_1_begrunnelser.ERSTATTER_ANNEN.kode)
            }
    }

    @Test
    fun registrer_inngangsvilkår_feiler() {
        val vilkaarDto = VilkaarDto().apply { vilkaar = Vilkaar.FO_883_2004_INNGANGSVILKAAR.kode }

        shouldThrow<FunksjonellException> { vilkaarsresultatService.registrerVilkår(BEHANDLING_ID, listOf(vilkaarDto)) }
            .message.shouldBe("Kan ikke endre vilkår " + Vilkaar.FO_883_2004_INNGANGSVILKAAR)
    }

    @Test
    fun tømVilkårsresultatFraBehandlingsresultat_sakstypeIkkeEøs_sletterAlleVilkår() {
        val behandlingsresultat = Behandlingsresultat().apply {
            id = BEHANDLING_ID
            behandling = Behandling().apply { fagsak = FagsakTestFactory.builder().type(Sakstyper.FTRL).build() }
            vilkaarsresultater = mutableSetOf(Vilkaarsresultat().apply { id = BEHANDLING_ID })
        }
        every { behandlingsresultatRepo.findById(BEHANDLING_ID) } returns Optional.of(behandlingsresultat)
        every { behandlingsresultatRepo.saveAndFlush(any()) } returnsArgument 0


        vilkaarsresultatService.tømVilkårsresultatFraBehandlingsresultat(BEHANDLING_ID)


        verify { behandlingsresultatRepo.saveAndFlush(behandlingsresultat) }
        behandlingsresultat.vilkaarsresultater.shouldBeEmpty()
    }

    @Test
    fun tømVilkårsresultatFraBehandlingsresultat_sakstypeEøsMenIngenFlyt_sletterAlleVilkår() {
        val behandlingsresultat = Behandlingsresultat().apply {
            id = BEHANDLING_ID
            behandling = Behandling().apply {
                id = BEHANDLING_ID
                fagsak = FagsakTestFactory.lagFagsak()
                tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
                type = Behandlingstyper.HENVENDELSE
            }
            vilkaarsresultater = mutableSetOf(Vilkaarsresultat().apply { id = BEHANDLING_ID })
        }
        every { saksbehandlingRegler.harIngenFlyt(behandlingsresultat.behandling) } returns true
        every { behandlingsresultatRepo.findById(BEHANDLING_ID) } returns Optional.of(behandlingsresultat)
        every { behandlingsresultatRepo.saveAndFlush(any()) } returnsArgument 0


        vilkaarsresultatService.tømVilkårsresultatFraBehandlingsresultat(BEHANDLING_ID)


        verify { behandlingsresultatRepo.saveAndFlush(behandlingsresultat) }
        behandlingsresultat.vilkaarsresultater.shouldBeEmpty()
    }

    @Test
    fun tømVilkårsresultatFraBehandlingsresultat_sakstypeEøsOgHarFlyt_sletterIkkeInngangsvilkår() {
        val behandlingsresultat = Behandlingsresultat().apply {
            id = BEHANDLING_ID
            behandling = Behandling().apply {
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
        every { behandlingsresultatRepo.saveAndFlush(any()) } returnsArgument 0


        vilkaarsresultatService.tømVilkårsresultatFraBehandlingsresultat(BEHANDLING_ID)


        verify { behandlingsresultatRepo.saveAndFlush(behandlingsresultat) }
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
            behandling = Behandling().apply {
                fagsak = FagsakTestFactory.lagFagsak()
            }
        }

}

