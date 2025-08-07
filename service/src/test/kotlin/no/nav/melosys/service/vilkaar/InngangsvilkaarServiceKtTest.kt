package no.nav.melosys.service.vilkaar

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.inngangsvilkar.Feilmelding
import no.nav.melosys.domain.inngangsvilkar.InngangsvilkarResponse
import no.nav.melosys.domain.inngangsvilkar.Kategori
import no.nav.melosys.domain.kodeverk.Landkoder
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.Vilkaar
import no.nav.melosys.domain.kodeverk.begrunnelser.Inngangsvilkaar
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.BESLUTNING_LOVVALG_NORGE
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.domain.person.Statsborgerskap
import no.nav.melosys.domain.util.IsoLandkodeKonverterer.tilIso3
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.inngangsvilkar.InngangsvilkaarConsumerImpl
import no.nav.melosys.service.SaksbehandlingDataFactory.lagBehandling
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class InngangsvilkaarServiceKtTest {
    
    @MockK
    lateinit var behandlingService: BehandlingService
    
    @MockK
    lateinit var inngangsvilkaarConsumer: InngangsvilkaarConsumerImpl
    
    @MockK
    lateinit var persondataFasade: PersondataFasade
    
    @MockK
    lateinit var saksbehandlingRegler: SaksbehandlingRegler
    
    @MockK
    lateinit var vilkaarsresultatService: VilkaarsresultatService

    private lateinit var inngangsvilkaarService: InngangsvilkaarService

    companion object {
        private val FINLAND = Land.FINLAND
        private val SVERIGE = Land.SVERIGE
    }

    @BeforeEach
    fun setUp() {
        inngangsvilkaarService = InngangsvilkaarService(
            behandlingService,
            inngangsvilkaarConsumer,
            persondataFasade,
            vilkaarsresultatService,
            saksbehandlingRegler
        )
    }

    @Test
    fun `vurderOgLagreInngangsvilkår_medFlereGyldigeStatsborgerskap_oppdaterVilkårsresultat`() {
        val søknadsland = listOf("FR", "DK", "NO")
        val periode = Periode(LocalDate.now().plusYears(1), LocalDate.MAX)
        
        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling()
        
        val statsborgerskap = setOf(
            Statsborgerskap("FIN", null, LocalDate.parse("1989-11-18"), null, "FREG", "Dolly", false),
            Statsborgerskap("SWE", LocalDate.parse("2009-11-18"), null, null, "PDL", "Dolly", false)
        )
        every { persondataFasade.hentStatsborgerskap(FagsakTestFactory.BRUKER_AKTØR_ID) } returns statsborgerskap

        val res = InngangsvilkarResponse().apply {
            feilmeldinger = emptyList()
            kvalifisererForEf883_2004 = true
        }
        every { 
            inngangsvilkaarConsumer.vurderInngangsvilkår(any(), any<Set<String>>(), any<Boolean>(), any()) 
        } returns res

        every { vilkaarsresultatService.oppdaterVilkaarsresultat(any(), any(), any(), any()) } just Runs

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, søknadsland, false, periode)

        verify { 
            inngangsvilkaarConsumer.vurderInngangsvilkår(
                setOf(Land.av(FINLAND), Land.av(SVERIGE)),
                tilIso3(søknadsland).toSet(),
                false,
                periode
            ) 
        }
        verify { 
            vilkaarsresultatService.oppdaterVilkaarsresultat(
                1L, 
                Vilkaar.FO_883_2004_INNGANGSVILKAAR, 
                true,
                emptySet()
            ) 
        }
    }

    @Test
    fun `vurderOgLagreInngangsvilkår_manglerStatsborgerskap_girBegrunnelse`() {
        val landkoder = listOf("FR", "DK", "NO")
        val periode = Periode(LocalDate.now().minusYears(2), LocalDate.now().minusYears(1))
        
        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling()
        every { persondataFasade.hentStatsborgerskap(any()) } returns emptySet()
        every { vilkaarsresultatService.oppdaterVilkaarsresultat(any(), any(), any(), any()) } just Runs

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, landkoder, false, periode)

        verify { 
            vilkaarsresultatService.oppdaterVilkaarsresultat(
                1L, 
                Vilkaar.FO_883_2004_INNGANGSVILKAAR,
                false, 
                setOf(Inngangsvilkaar.MANGLER_STATSBORGERSKAP)
            ) 
        }
    }

    @Test
    fun `vurderOgLagreInngangsvilkår_tomDatoErNull_tomDatoSettesTilEttÅrEtterFomDato`() {
        val landkoder = listOf("FR", "DK", "NO")
        val periode = Periode(LocalDate.now().plusYears(1), null)
        val søknadsperiodeSlot = slot<Periode>()
        
        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling()
        
        val statsborgerskap = setOf(
            Statsborgerskap("FIN", null, LocalDate.parse("1989-11-18"), null, "FREG", "Dolly", false)
        )
        every { persondataFasade.hentStatsborgerskap(any()) } returns statsborgerskap
        
        val res = InngangsvilkarResponse().apply {
            feilmeldinger = emptyList()
            kvalifisererForEf883_2004 = true
        }
        every { 
            inngangsvilkaarConsumer.vurderInngangsvilkår(
                any(), 
                any<Set<String>>(), 
                any<Boolean>(), 
                capture(søknadsperiodeSlot)
            ) 
        } returns res
        every { vilkaarsresultatService.oppdaterVilkaarsresultat(any(), any(), any(), any()) } just Runs

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, landkoder, false, periode)

        val søknadsperiode = søknadsperiodeSlot.captured
        søknadsperiode.tom shouldBe LocalDate.now().plusYears(2)
    }

    @Test
    fun `vurderOgLagreInngangsvilkår_flereLandUkjentHvilke`() {
        val periode = Periode(LocalDate.now().plusYears(1), LocalDate.MAX)
        
        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling()
        
        val statsborgerskap = setOf(
            Statsborgerskap("FIN", null, LocalDate.parse("1989-11-18"), null, "FREG", "Dolly", false)
        )
        every { persondataFasade.hentStatsborgerskap(any()) } returns statsborgerskap
        
        val res = InngangsvilkarResponse().apply {
            feilmeldinger = emptyList()
            kvalifisererForEf883_2004 = true
        }
        every { 
            inngangsvilkaarConsumer.vurderInngangsvilkår(any(), any<Set<String>>(), any<Boolean>(), any()) 
        } returns res
        every { vilkaarsresultatService.oppdaterVilkaarsresultat(any(), any(), any(), any()) } just Runs

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, emptyList(), true, periode)

        verify { 
            inngangsvilkaarConsumer.vurderInngangsvilkår(
                setOf(Land.av(FINLAND)),
                emptySet(),
                true,
                periode
            ) 
        }
        verify { 
            vilkaarsresultatService.oppdaterVilkaarsresultat(
                1L, 
                Vilkaar.FO_883_2004_INNGANGSVILKAAR, 
                true, 
                emptySet()
            ) 
        }
    }

    @Test
    fun `vurderOgLagreInngangsvilkår_feil_girBegrunnelse`() {
        val landkoder = listOf("FR", "DK", "NO")
        val periode = Periode(LocalDate.now().plusYears(1), LocalDate.MAX)
        
        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling()
        
        val statsborgerskap = setOf(
            Statsborgerskap("FIN", null, LocalDate.parse("1989-11-18"), null, "FREG", "Dolly", false)
        )
        every { persondataFasade.hentStatsborgerskap(any()) } returns statsborgerskap
        
        val feilmelding = Feilmelding().apply {
            kategori = Kategori.TEKNISK_FEIL
            melding = "FEIL!!!"
        }
        val res = InngangsvilkarResponse().apply {
            feilmeldinger = listOf(feilmelding)
            kvalifisererForEf883_2004 = false
        }
        every { 
            inngangsvilkaarConsumer.vurderInngangsvilkår(any(), any<Set<String>>(), any<Boolean>(), any()) 
        } returns res
        every { vilkaarsresultatService.oppdaterVilkaarsresultat(any(), any(), any(), any()) } just Runs

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, landkoder, false, periode)

        verify { 
            vilkaarsresultatService.oppdaterVilkaarsresultat(
                1L, 
                Vilkaar.FO_883_2004_INNGANGSVILKAAR,
                false, 
                setOf(Inngangsvilkaar.TEKNISK_FEIL)
            ) 
        }
    }

    @Test
    fun `avgjørGyldigeStatsborgerskapForPerioden`() {
        val statsborgerskapFraPdl = setOf(
            Statsborgerskap("AAA", null, LocalDate.parse("1979-11-18"), LocalDate.parse("1980-11-18"), "FREG", "Holly", false),
            Statsborgerskap("BBB", null, LocalDate.parse("1979-11-18"), LocalDate.parse("1980-11-18"), "PDL", "Dolly", false),
            Statsborgerskap("CCC", null, LocalDate.parse("2020-11-18"), null, "PDL", "Molly", false),
            Statsborgerskap("DDD", LocalDate.parse("2021-05-08"), LocalDate.parse("1979-11-18"), LocalDate.parse("1980-11-18"), "PDL", "Molly", false),
            Statsborgerskap("EEE", null, null, null, "FREG", "Nully", false)
        )
        val periode = Periode(LocalDate.parse("2020-11-18"), null)

        val statsborgerskap = inngangsvilkaarService.avgjørGyldigeStatsborgerskapForPerioden(statsborgerskapFraPdl, periode)

        statsborgerskap shouldContainExactlyInAnyOrder setOf(Land.av("CCC"), Land.av("DDD"), Land.av("EEE"))
    }

    @Test
    fun `overstyrInngangsvilkårTilOppfylt_ingenInngangsvilkårFunnet_kasterFunksjonellException`() {
        every { vilkaarsresultatService.finnVilkaarsresultat(any<Long>(), eq(Vilkaar.FO_883_2004_INNGANGSVILKAAR)) } returns null

        val exception = shouldThrow<FunksjonellException> {
            inngangsvilkaarService.overstyrInngangsvilkårTilOppfylt(1L)
        }
        exception.message shouldBe "Inngangsvilkår er ikke vurdert for behandling 1"
    }

    @Test
    fun `overstyrInngangsvilkårTilOppfylt_manglerLandOgPeriode_kasterFunksjonellException`() {
        every { behandlingService.hentBehandling(1L) } returns lagBehandling()
        every { vilkaarsresultatService.finnVilkaarsresultat(any<Long>(), eq(Vilkaar.FO_883_2004_INNGANGSVILKAAR)) } returns Vilkaarsresultat()

        val exception = shouldThrow<FunksjonellException> {
            inngangsvilkaarService.overstyrInngangsvilkårTilOppfylt(1L)
        }
        exception.message shouldBe "Mangler land eller periode for behandling 1"
    }

    @Test
    fun `overstyrInngangsvilkårTilOppfylt_inngangsvilkårFunnet_oppfyllerVilkår`() {
        every { behandlingService.hentBehandling(1L) } returns lagBehandlingMedPeriodeOgLand()
        val vilkaarsresultat = Vilkaarsresultat()
        every { vilkaarsresultatService.finnVilkaarsresultat(any<Long>(), eq(Vilkaar.FO_883_2004_INNGANGSVILKAAR)) } returns vilkaarsresultat
        every { vilkaarsresultatService.oppdaterVilkaarsresultat(any(), any(), any(), any()) } just Runs

        inngangsvilkaarService.overstyrInngangsvilkårTilOppfylt(1L)

        verify { 
            vilkaarsresultatService.oppdaterVilkaarsresultat(
                eq(1L), 
                eq(Vilkaar.FO_883_2004_INNGANGSVILKAAR), 
                eq(true), 
                any()
            ) 
        }
    }

    @Test
    fun `overstyrInngangsvilkårTilOppfylt_inngangsvilkårFunnet_beholderGamleBegrunnelserOgLeggerTilOverstyringsbegrunnelse`() {
        every { behandlingService.hentBehandling(1L) } returns lagBehandlingMedPeriodeOgLand()
        
        val vilkaarBegrunnelse = VilkaarBegrunnelse().apply {
            kode = Inngangsvilkaar.MANGLER_STATSBORGERSKAP.kode
        }
        val vilkaarsresultat = Vilkaarsresultat().apply {
            begrunnelser = setOf(vilkaarBegrunnelse)
        }
        every { vilkaarsresultatService.finnVilkaarsresultat(any<Long>(), eq(Vilkaar.FO_883_2004_INNGANGSVILKAAR)) } returns vilkaarsresultat
        every { vilkaarsresultatService.oppdaterVilkaarsresultat(any(), any(), any(), any()) } just Runs

        inngangsvilkaarService.overstyrInngangsvilkårTilOppfylt(1L)

        verify { 
            vilkaarsresultatService.oppdaterVilkaarsresultat(
                eq(1L), 
                eq(Vilkaar.FO_883_2004_INNGANGSVILKAAR), 
                any<Boolean>(), 
                eq(setOf(Inngangsvilkaar.OVERSTYRT_AV_SAKSBEHANDLER, Inngangsvilkaar.MANGLER_STATSBORGERSKAP))
            ) 
        }
    }

    @Test
    fun `skalVurdereInngangsvilkår_altStemmer_returnererTrue`() {
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns false
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns false
        every { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) } returns false
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandling = lagBehandlingMedPeriodeOgLand() // Må ha tema BESLUTNING_LOVVALG_NORGE satt, eller så feiler testen
        behandling.fagsak = fagsak

        inngangsvilkaarService.skalVurdereInngangsvilkår(behandling) shouldBe true
    }

    @Test
    fun `skalVurdereInngangsvilkår_sakstypeIkkeEøs_returnererFalse`() {
        val fagsak = FagsakTestFactory.builder().type(Sakstyper.FTRL).build()
        val behandling = BehandlingTestFactory.builderWithDefaults()
            .medFagsak(fagsak)
            .build()

        inngangsvilkaarService.skalVurdereInngangsvilkår(behandling) shouldBe false
        verify(exactly = 0) { saksbehandlingRegler.harIngenFlyt(any()) }
    }

    @Test
    fun `skalVurdereInngangsvilkår_harIngenFlyt_returnererFalse`() {
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns true
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandling = BehandlingTestFactory.builderWithDefaults()
            .medFagsak(fagsak)
            .build()

        inngangsvilkaarService.skalVurdereInngangsvilkår(behandling) shouldBe false
        verify { saksbehandlingRegler.harIngenFlyt(any()) }
        verify(exactly = 0) { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) }
        verify(exactly = 0) { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) }
    }

    @Test
    fun `skalVurdereInngangsvilkår_harUnntaktsregistreringFlyt_returnererFalse`() {
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns false
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns true
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandling = BehandlingTestFactory.builderWithDefaults()
            .medFagsak(fagsak)
            .build()

        inngangsvilkaarService.skalVurdereInngangsvilkår(behandling) shouldBe false
        verify { saksbehandlingRegler.harIngenFlyt(any()) }
        verify { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) }
        verify(exactly = 0) { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) }
    }

    @Test
    fun `skalVurdereInngangsvilkår_harIkkeYrkeskaktivFlyt_returnererFalse`() {
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns false
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns false
        every { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) } returns true
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandling = BehandlingTestFactory.builderWithDefaults()
            .medFagsak(fagsak)
            .build()

        inngangsvilkaarService.skalVurdereInngangsvilkår(behandling) shouldBe false
        verify { saksbehandlingRegler.harIngenFlyt(any()) }
        verify { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) }
        verify { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) }
    }

    @Test
    fun `skalVurdereInngangsvilkår_erSed_returnererFalse`() {
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns false
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns false
        every { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) } returns false
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandling = BehandlingTestFactory.builderWithDefaults()
            .medFagsak(fagsak)
            .medTema(REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE)
            .build()

        inngangsvilkaarService.skalVurdereInngangsvilkår(behandling) shouldBe false
        verify { saksbehandlingRegler.harIngenFlyt(any()) }
        verify { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) }
        verify { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) }
    }

    @Test
    fun `skalVurdereInngangsvilkår_harIkkePeriode_returnererFalse`() {
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns false
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns false
        every { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) } returns false
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandling = lagBehandlingMedPeriodeOgLand().apply {
            this.fagsak = fagsak
            mottatteOpplysninger!!.mottatteOpplysningerData.periode = no.nav.melosys.domain.mottatteopplysninger.data.Periode(null, null)
        }

        inngangsvilkaarService.skalVurdereInngangsvilkår(behandling) shouldBe false
        verify { saksbehandlingRegler.harIngenFlyt(any()) }
        verify { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) }
        verify { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) }
    }

    @Test
    fun `skalVurdereInngangsvilkår_harIkkeLand_returnererFalse`() {
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns false
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns false
        every { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) } returns false
        val fagsak = FagsakTestFactory.lagFagsak()
        val behandling = lagBehandlingMedPeriodeOgLand().apply {
            this.fagsak = fagsak
            mottatteOpplysninger!!.mottatteOpplysningerData.soeknadsland.landkoder = emptyList()
        }

        inngangsvilkaarService.skalVurdereInngangsvilkår(behandling) shouldBe false
        verify { saksbehandlingRegler.harIngenFlyt(any()) }
        verify { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) }
        verify { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) }
    }

    private fun lagBehandlingMedPeriodeOgLand(): Behandling {
        val mottatteOpplysningerData = MottatteOpplysningerData().apply {
            periode = no.nav.melosys.domain.mottatteopplysninger.data.Periode(LocalDate.now(), null)
            soeknadsland = Soeknadsland(listOf(Landkoder.BE.kode), false)
        }

        val mottatteOpplysninger = MottatteOpplysninger().apply {
            setMottatteOpplysningerData(mottatteOpplysningerData)
        }

        return BehandlingTestFactory.builderWithDefaults()
            .medTema(BESLUTNING_LOVVALG_NORGE)
            .medMottatteOpplysninger(mottatteOpplysninger)
            .build()
    }
}