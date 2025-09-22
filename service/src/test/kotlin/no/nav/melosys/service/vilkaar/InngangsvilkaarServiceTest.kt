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
import no.nav.melosys.domain.toErPeriode
import no.nav.melosys.domain.person.Statsborgerskap
import no.nav.melosys.domain.util.IsoLandkodeKonverterer.tilIso3
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.inngangsvilkar.InngangsvilkaarConsumer
import no.nav.melosys.service.SaksbehandlingDataFactory.lagBehandling
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.VilkaarsresultatService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import no.nav.melosys.integrasjon.inngangsvilkar.VurderInngangsvilkaarRequest

@ExtendWith(MockKExtension::class)
class InngangsvilkaarServiceTest {

    @MockK
    lateinit var behandlingService: BehandlingService

    @MockK
    lateinit var inngangsvilkaarConsumer: InngangsvilkaarConsumer

    @MockK
    lateinit var persondataFasade: PersondataFasade

    @MockK
    lateinit var saksbehandlingRegler: SaksbehandlingRegler

    @MockK
    lateinit var vilkaarsresultatService: VilkaarsresultatService

    private lateinit var inngangsvilkaarService: InngangsvilkaarService


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
    fun `vurderOgLagreInngangsvilkår skal oppdatere vilkårsresultat når det er flere gyldige statsborgerskap`() {
        val søknadsland = listOf("FR", "DK", "NO")
        val periode = Periode(LocalDate.now().plusYears(1), null)

        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling()

        val statsborgerskap = setOf(
            Statsborgerskap("FIN", null, LocalDate.parse("1989-11-18"), null, "FREG", "Dolly", false),
            Statsborgerskap("SWE", LocalDate.parse("2009-11-18"), null, null, "PDL", "Dolly", false)
        )
        every { persondataFasade.hentStatsborgerskap(any()) } returns statsborgerskap

        val res = InngangsvilkarResponse().apply {
            feilmeldinger = emptyList()
            kvalifisererForEf883_2004 = true
        }
        every {
            inngangsvilkaarConsumer.vurderInngangsvilkår(any())
        } returns res

        every { vilkaarsresultatService.oppdaterVilkaarsresultat(any(), any(), any(), any()) } just Runs

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, søknadsland, false, periode.toErPeriode()!!)

        verify {
            inngangsvilkaarConsumer.vurderInngangsvilkår(
                match {
                    it.statsborgerskap.containsAll(setOf(Land.FINLAND, Land.SVERIGE)) &&
                    it.statsborgerskap.size == 2 &&
                    it.arbeidsland.containsAll(tilIso3(søknadsland).toSet()) &&
                    it.arbeidsland.size == tilIso3(søknadsland).toSet().size &&
                    !it.flereLandUkjentHvilke &&
                    it.periode.fom == periode.fom &&
                    it.periode.tom == periode.fom?.plusYears(1) // Default 1 year added when tom is null
                }
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
    fun `vurderOgLagreInngangsvilkår skal gi begrunnelse når statsborgerskap mangler`() {
        val landkoder = listOf("FR", "DK", "NO")
        val periode = Periode(LocalDate.now().minusYears(2), LocalDate.now().minusYears(1))

        every { behandlingService.hentBehandling(any<Long>()) } returns lagBehandling()
        every { persondataFasade.hentStatsborgerskap(any()) } returns emptySet()
        every { vilkaarsresultatService.oppdaterVilkaarsresultat(any(), any(), any(), any()) } just Runs

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, landkoder, false, periode.toErPeriode()!!)

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
    fun `vurderOgLagreInngangsvilkår skal sette tom-dato til ett år etter fom-dato når tom-dato er null`() {
        val landkoder = listOf("FR", "DK", "NO")
        val periode = Periode(LocalDate.now().plusYears(1), null) // Use null for open-ended periods
        val vurderInngangsvilkaarRequestSlot = slot<VurderInngangsvilkaarRequest>()

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
                capture(vurderInngangsvilkaarRequestSlot),
            )
        } returns res
        every { vilkaarsresultatService.oppdaterVilkaarsresultat(any(), any(), any(), any()) } just Runs

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, landkoder, false, periode.toErPeriode()!!)

        val vurderInngangsvilkaarRequest = vurderInngangsvilkaarRequestSlot.captured
        vurderInngangsvilkaarRequest.periode.tom shouldBe LocalDate.now().plusYears(2)
    }

    @Test
    fun `vurderOgLagreInngangsvilkår skal håndtere flere land når det er ukjent hvilke`() {
        val periode = Periode(LocalDate.now().plusYears(1), null)

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
            inngangsvilkaarConsumer.vurderInngangsvilkår(any())
        } returns res
        every { vilkaarsresultatService.oppdaterVilkaarsresultat(any(), any(), any(), any()) } just Runs

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, emptyList(), true, periode.toErPeriode()!!)

        verify {
            inngangsvilkaarConsumer.vurderInngangsvilkår(
                match {
                    it.statsborgerskap == setOf(Land.FINLAND) &&
                    it.arbeidsland.isEmpty() &&
                    it.flereLandUkjentHvilke &&
                    it.periode.fom == periode.fom &&
                    it.periode.tom == periode.fom?.plusYears(1) // Default 1 year added when tom is null
                }
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
    fun `vurderOgLagreInngangsvilkår skal gi begrunnelse når det oppstår feil`() {
        val landkoder = listOf("FR", "DK", "NO")
        val periode = Periode(LocalDate.now().plusYears(1), null)

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
            inngangsvilkaarConsumer.vurderInngangsvilkår(any())
        } returns res
        every { vilkaarsresultatService.oppdaterVilkaarsresultat(any(), any(), any(), any()) } just Runs

        inngangsvilkaarService.vurderOgLagreInngangsvilkår(1L, landkoder, false, periode.toErPeriode()!!)

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
    fun `avgjørGyldigeStatsborgerskapForPerioden skal returnere gyldige statsborgerskap for gitt periode`() {
        val statsborgerskapFraPdl = setOf(
            Statsborgerskap("AAA", null, LocalDate.parse("1979-11-18"), LocalDate.parse("1980-11-18"), "FREG", "Holly", false),
            Statsborgerskap("BBB", null, LocalDate.parse("1979-11-18"), LocalDate.parse("1980-11-18"), "PDL", "Dolly", false),
            Statsborgerskap("CCC", null, LocalDate.parse("2020-11-18"), null, "PDL", "Molly", false),
            Statsborgerskap("DDD", LocalDate.parse("2021-05-08"), LocalDate.parse("1979-11-18"), LocalDate.parse("1980-11-18"), "PDL", "Molly", false),
            Statsborgerskap("EEE", null, null, null, "FREG", "Nully", false)
        )
        val periode = Periode(LocalDate.parse("2020-11-18"), null) // Use null for open-ended periods

        val statsborgerskap = inngangsvilkaarService.avgjørGyldigeStatsborgerskapForPerioden(statsborgerskapFraPdl, periode.toErPeriode()!!)

        statsborgerskap shouldContainExactlyInAnyOrder setOf(Land.av("CCC"), Land.av("DDD"), Land.av("EEE"))
    }

    @Test
    fun `overstyrInngangsvilkårTilOppfylt skal kaste FunksjonellException når ingen inngangsvilkår er funnet`() {
        every { vilkaarsresultatService.finnVilkaarsresultat(any<Long>(), eq(Vilkaar.FO_883_2004_INNGANGSVILKAAR)) } returns null

        val exception = shouldThrow<FunksjonellException> {
            inngangsvilkaarService.overstyrInngangsvilkårTilOppfylt(1L)
        }
        exception.message shouldBe "Inngangsvilkår er ikke vurdert for behandling 1"
    }

    @Test
    fun `overstyrInngangsvilkårTilOppfylt skal kaste FunksjonellException når land og periode mangler`() {
        every { behandlingService.hentBehandling(1L) } returns lagBehandling()
        every { vilkaarsresultatService.finnVilkaarsresultat(any<Long>(), eq(Vilkaar.FO_883_2004_INNGANGSVILKAAR)) } returns Vilkaarsresultat()

        val exception = shouldThrow<FunksjonellException> {
            inngangsvilkaarService.overstyrInngangsvilkårTilOppfylt(1L)
        }
        exception.message shouldBe "Mangler land eller periode for behandling 1"
    }

    @Test
    fun `overstyrInngangsvilkårTilOppfylt skal oppfylle vilkår når inngangsvilkår er funnet`() {
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
    fun `overstyrInngangsvilkårTilOppfylt skal beholde gamle begrunnelser og legge til overstyringsbegrunnelse når inngangsvilkår er funnet`() {
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
    fun `skalVurdereInngangsvilkår skal returnere true når alt stemmer`() {
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns false
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns false
        every { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) } returns false
        val behandling = lagBehandlingMedPeriodeOgLand().apply {
            fagsak = Fagsak.forTest { medBruker() }
        }

        inngangsvilkaarService.skalVurdereInngangsvilkår(behandling) shouldBe true
    }

    @Test
    fun `skalVurdereInngangsvilkår skal returnere false når sakstype ikke er EØS`() {
        val behandling = Behandling.forTest {
            fagsak {
                type = Sakstyper.FTRL
            }
        }

        inngangsvilkaarService.skalVurdereInngangsvilkår(behandling) shouldBe false
        verify(exactly = 0) { saksbehandlingRegler.harIngenFlyt(any()) }
    }

    @Test
    fun `skalVurdereInngangsvilkår skal returnere false når behandling har ingen flyt`() {
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns true
        val behandling = Behandling.forTest {
            fagsak { medBruker() }
        }

        inngangsvilkaarService.skalVurdereInngangsvilkår(behandling) shouldBe false
        verify { saksbehandlingRegler.harIngenFlyt(any()) }
        verify(exactly = 0) { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) }
        verify(exactly = 0) { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) }
    }

    @Test
    fun `skalVurdereInngangsvilkår skal returnere false når behandling har unntaksregistrering flyt`() {
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns false
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns true
        val behandling = Behandling.forTest {
            fagsak { medBruker() }
        }

        inngangsvilkaarService.skalVurdereInngangsvilkår(behandling) shouldBe false
        verify { saksbehandlingRegler.harIngenFlyt(any()) }
        verify { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) }
        verify(exactly = 0) { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) }
    }

    @Test
    fun `skalVurdereInngangsvilkår skal returnere false når behandling har ikke-yrkesaktiv flyt`() {
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns false
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns false
        every { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) } returns true
        val behandling = Behandling.forTest {
            fagsak { medBruker() }
        }

        inngangsvilkaarService.skalVurdereInngangsvilkår(behandling) shouldBe false
        verify { saksbehandlingRegler.harIngenFlyt(any()) }
        verify { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) }
        verify { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) }
    }

    @Test
    fun `skalVurdereInngangsvilkår skal returnere false når behandling er SED`() {
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns false
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns false
        every { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) } returns false
        val behandling = Behandling.forTest {
            fagsak { medBruker() }
            tema = REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE
        }

        inngangsvilkaarService.skalVurdereInngangsvilkår(behandling) shouldBe false
        verify { saksbehandlingRegler.harIngenFlyt(any()) }
        verify { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) }
        verify { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) }
    }

    @Test
    fun `skalVurdereInngangsvilkår skal returnere false når behandling ikke har periode`() {
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns false
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns false
        every { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) } returns false
        val behandling = lagBehandlingMedPeriodeOgLand().apply {
            fagsak = Fagsak.forTest { medBruker() }
            mottatteOpplysninger!!.mottatteOpplysningerData.periode = null // Test without a periode
        }

        inngangsvilkaarService.skalVurdereInngangsvilkår(behandling) shouldBe false
        verify { saksbehandlingRegler.harIngenFlyt(any()) }
        verify { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) }
        verify { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) }
    }

    @Test
    fun `skalVurdereInngangsvilkår skal returnere false når behandling ikke har land`() {
        every { saksbehandlingRegler.harIngenFlyt(any()) } returns false
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) } returns false
        every { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) } returns false
        val behandling = lagBehandlingMedPeriodeOgLand().apply {
            fagsak = Fagsak.forTest { medBruker() }
            mottatteOpplysninger!!.mottatteOpplysningerData.soeknadsland.landkoder = emptyList()
        }

        inngangsvilkaarService.skalVurdereInngangsvilkår(behandling) shouldBe false
        verify { saksbehandlingRegler.harIngenFlyt(any()) }
        verify { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(any()) }
        verify { saksbehandlingRegler.harIkkeYrkesaktivFlyt(any()) }
    }

    private fun lagBehandlingMedPeriodeOgLand(): Behandling {
        val mottatteOpplysningerData = MottatteOpplysningerData().apply {
            periode = Periode(LocalDate.now(), null) // Use null for open-ended periods
            soeknadsland = Soeknadsland(listOf(Landkoder.BE.kode), false)
        }

        val mottatteOpplysninger = MottatteOpplysninger().apply {
            setMottatteOpplysningerData(mottatteOpplysningerData)
        }

        return Behandling.forTest {
            tema = BESLUTNING_LOVVALG_NORGE
            this.mottatteOpplysninger = mottatteOpplysninger
        }
    }

    companion object {
        private val FINLAND = Land.FINLAND
        private val SVERIGE = Land.SVERIGE
    }
}
