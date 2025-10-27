package no.nav.melosys.saksflyt.steg.medl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.Called
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflyt.TestdataFactory
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.medl.MedlPeriodeService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class LagreLovvalgsperiodeMedlTest {

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    lateinit var medlPeriodeService: MedlPeriodeService

    @RelaxedMockK
    lateinit var saksbehandlingRegler: SaksbehandlingRegler

    private lateinit var lagreLovvalgsperiodeMedl: LagreLovvalgsperiodeMedl

    private val behandlingID = 2434L
    private val prosessinstans = Prosessinstans.forTest()
    private val behandling = Behandling.forTest()
    private val behandlingsresultat = Behandlingsresultat()

    @BeforeEach
    fun setup() {
        lagreLovvalgsperiodeMedl = LagreLovvalgsperiodeMedl(
            behandlingsresultatService,
            medlPeriodeService,
            saksbehandlingRegler
        )

        behandling.apply {
            id = behandlingID
            tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
            fagsak = Fagsak.forTest {
                type = Sakstyper.TRYGDEAVTALE
                tema = Sakstemaer.UNNTAK
            }

        }

        prosessinstans.behandling = behandling
    }

    @Test
    fun utfør_erAvslagMedLovvalgsperiodeMedMedlID_avviserMedlPeriode() {
        val lovvalgsperiode =
            lagLovvalgsperiode(
                11L,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
                InnvilgelsesResultat.AVSLAATT
            )

        behandlingsresultat.lovvalgsperioder.add(lovvalgsperiode)
        behandlingsresultat.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND

        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat


        lagreLovvalgsperiodeMedl.utfør(prosessinstans)


        verify { medlPeriodeService.avvisPeriode(lovvalgsperiode.medlPeriodeID) }
    }

    @Test
    fun utfør_erInnvilgelseArt13IngenMedlID_oppretterForeløpigPeriode() {
        val lovvalgsperiode =
            lagLovvalgsperiode(
                null,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A,
                InnvilgelsesResultat.INNVILGET
            )

        behandlingsresultat.lovvalgsperioder.add(lovvalgsperiode)
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat


        lagreLovvalgsperiodeMedl.utfør(prosessinstans)


        verify { medlPeriodeService.opprettPeriodeForeløpig(lovvalgsperiode, behandlingID) }
    }

    @Test
    fun utfør_erInnvilgelseArt13MedMedlID_oppdatererTilForeløpigPeriode() {
        val lovvalgsperiode =
            lagLovvalgsperiode(
                11L,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A,
                InnvilgelsesResultat.INNVILGET
            )

        behandlingsresultat.lovvalgsperioder.add(lovvalgsperiode)
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat


        lagreLovvalgsperiodeMedl.utfør(prosessinstans)


        verify { medlPeriodeService.oppdaterPeriodeForeløpig(lovvalgsperiode) }
    }

    @Test
    fun utfør_erInnvilgelseArt13IngenMedlIDUnntaksflyt_oppretterEndeligPeriode() {
        val lovvalgsperiode =
            lagLovvalgsperiode(
                null,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A,
                InnvilgelsesResultat.INNVILGET
            )

        behandlingsresultat.lovvalgsperioder.add(lovvalgsperiode)
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling) } returns true


        lagreLovvalgsperiodeMedl.utfør(prosessinstans)


        verify { medlPeriodeService.opprettPeriodeEndelig(lovvalgsperiode, behandlingID) }
    }

    @Test
    fun utfør_erInnvilgelseArt13MedMedlIDUnntaksflyt_oppdatererTilEndeligPeriode() {
        val lovvalgsperiode =
            lagLovvalgsperiode(
                11L,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A,
                InnvilgelsesResultat.INNVILGET
            )

        behandlingsresultat.lovvalgsperioder.add(lovvalgsperiode)
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling) } returns true


        lagreLovvalgsperiodeMedl.utfør(prosessinstans)


        verify { medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode) }
    }

    @Test
    fun utfør_erInnvilgelseArt12IngenMedlID_oppretterEndeligPeriode() {
        val lovvalgsperiode =
            lagLovvalgsperiode(
                null,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
                InnvilgelsesResultat.INNVILGET
            )

        behandlingsresultat.lovvalgsperioder.add(lovvalgsperiode)
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat


        lagreLovvalgsperiodeMedl.utfør(prosessinstans)


        verify { medlPeriodeService.opprettPeriodeEndelig(lovvalgsperiode, behandlingID) }
    }

    @Test
    fun utfør_erInnvilgelseArt12MedMedlID_oppdatererTilEndeligPeriode() {
        val lovvalgsperiode =
            lagLovvalgsperiode(
                11L,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
                InnvilgelsesResultat.INNVILGET
            )

        behandlingsresultat.lovvalgsperioder.add(lovvalgsperiode)
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat


        lagreLovvalgsperiodeMedl.utfør(prosessinstans)


        every { medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode) }
    }

    @Test
    fun utfør_nyVurderingOgPeriodeFinnes_oppdaterPeriode() {
        val behandling = TestdataFactory.lagBehandlingNyVurdering()
        prosessinstans.behandling = behandling

        val opprinneligBehandling = TestdataFactory.lagBehandling()
        behandling.opprinneligBehandling = opprinneligBehandling
        val opprinneligResultat = Behandlingsresultat()

        val opprinneligLovvalgsperiode = lagLovvalgsperiode(
            777L,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
            InnvilgelsesResultat.INNVILGET
        )

        opprinneligResultat.lovvalgsperioder.add(opprinneligLovvalgsperiode)
        every { behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandling.id) } returns opprinneligResultat

        val nyLovvalgsperiode = lagLovvalgsperiode(
            null,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
            InnvilgelsesResultat.INNVILGET
        )

        behandlingsresultat.lovvalgsperioder.add(nyLovvalgsperiode)

        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat


        lagreLovvalgsperiodeMedl.utfør(prosessinstans)


        verify {
            medlPeriodeService.oppdaterPeriodeEndelig(
                match { it.medlPeriodeID == opprinneligLovvalgsperiode.medlPeriodeID }
            )
        }
    }

    @Test
    fun utfør_nyVurderingOgPeriodeFinnesIkke_opprettPeriode() {
        val behandling = TestdataFactory.lagBehandlingNyVurdering()
        prosessinstans.behandling = behandling

        val opprinneligBehandling = TestdataFactory.lagBehandling()
        behandling.opprinneligBehandling = opprinneligBehandling

        every { behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandling.id) } returns Behandlingsresultat()

        val nyLovvalgsperiode = lagLovvalgsperiode(
            null,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
            InnvilgelsesResultat.INNVILGET
        )

        behandlingsresultat.lovvalgsperioder.add(nyLovvalgsperiode)
        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat


        lagreLovvalgsperiodeMedl.utfør(prosessinstans)


        every { medlPeriodeService.opprettPeriodeEndelig(nyLovvalgsperiode, behandling.id) }
    }

    @Test
    fun utfør_nyVurderingOgOpprinneligBehandlingFinnesIkke_opprettPeriode() {
        val behandling = TestdataFactory.lagBehandlingNyVurdering()
        prosessinstans.behandling = behandling
        behandling.opprinneligBehandling = null

        val nyLovvalgsperiode =
            lagLovvalgsperiode(
                null,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
                InnvilgelsesResultat.INNVILGET
            )

        behandlingsresultat.lovvalgsperioder.add(nyLovvalgsperiode)
        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat


        lagreLovvalgsperiodeMedl.utfør(prosessinstans)


        every { medlPeriodeService.opprettPeriodeEndelig(nyLovvalgsperiode, behandling.id) }
    }

    @Test
    fun utfør_ikkeGodkjentRegistreringUnntak_oppretterIkkeLovvalgsperiode() {
        val fagsak = Fagsak.forTest {
            type = Sakstyper.TRYGDEAVTALE
            tema = Sakstemaer.UNNTAK
        }

        val behandling = TestdataFactory.lagBehandling()
        behandling.fagsak = fagsak
        behandling.tema = Behandlingstema.REGISTRERING_UNNTAK
        prosessinstans.behandling = behandling

        behandlingsresultat.utfallRegistreringUnntak = Utfallregistreringunntak.IKKE_GODKJENT

        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat
        every { saksbehandlingRegler.harRegistreringUnntakFraMedlemskapFlyt(behandling) } returns true


        lagreLovvalgsperiodeMedl.utfør(prosessinstans)


        verify { medlPeriodeService wasNot Called }
    }

    @Test
    fun utfør_typeFastsattLovvalgslandIngenLovvalgsperiode_forventException() {
        behandlingsresultat.type = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat


        shouldThrow<NoSuchElementException> {
            lagreLovvalgsperiodeMedl.utfør(prosessinstans)
        }.message.shouldContain("Ingen lovvalgsperiode")
    }

    @Test
    fun utfør_lovvalgsperiodeFinnesInnvilgelsesresultatDelvisInnvilget_forventException() {
        val lovvalgsperiode = lagLovvalgsperiode(
            11L,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1,
            InnvilgelsesResultat.DELVIS_INNVILGET
        )
        behandlingsresultat.lovvalgsperioder.add(lovvalgsperiode)
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat


        shouldThrow<FunksjonellException> {
            lagreLovvalgsperiodeMedl.utfør(prosessinstans)
        }.message.shouldContain("Ukjent eller ikke-eksisterende innvilgelsesresultat")
    }

    @Test
    fun utfør_ikke_opprett_lovvalgsperiode_dersom_unntak_turistskip_er_oppfylt() {
        val behandling = TestdataFactory.lagBehandling()

        val lovvalgsperiode =
            lagLovvalgsperiode(
                1L,
                Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A,
                InnvilgelsesResultat.INNVILGET
            )

        prosessinstans.behandling = behandling

        val vilkaarsresultat = Vilkaarsresultat()
        vilkaarsresultat.vilkaar = Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP
        vilkaarsresultat.isOppfylt = true

        behandlingsresultat.lovvalgsperioder.add(lovvalgsperiode)
        behandlingsresultat.vilkaarsresultater.add(vilkaarsresultat)

        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat


        lagreLovvalgsperiodeMedl.utfør(prosessinstans)


        verify { medlPeriodeService wasNot Called }
    }

    @Test
    fun slett_lovvalgsperiode_ved_ny_vurdering_dersom_unntak_turistskip_er_oppfylt() {
        val opprinneligBehandling = TestdataFactory.lagBehandling()
        val opprinneligVilkaarsresultat = lagVilkaarsresultat(Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP, false)
        val opprinneligResultat = Behandlingsresultat()

        val opprinneligLovvalgsperiode = lagLovvalgsperiode(
            777L,
            Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A,
            InnvilgelsesResultat.INNVILGET
        )

        opprinneligResultat.lovvalgsperioder.add(opprinneligLovvalgsperiode)
        opprinneligResultat.vilkaarsresultater.add(opprinneligVilkaarsresultat)

        every { behandlingsresultatService.hentBehandlingsresultat(opprinneligBehandling.id) } returns opprinneligResultat

        val behandling = TestdataFactory.lagBehandlingNyVurdering()
        behandling.opprinneligBehandling = opprinneligBehandling

        prosessinstans.behandling = behandling

        val nyVilkaarsresultat = lagVilkaarsresultat(Vilkaar.FTRL_2_12_UNNTAK_TURISTSKIP, true)

        behandlingsresultat.lovvalgsperioder.add(opprinneligLovvalgsperiode)
        behandlingsresultat.vilkaarsresultater.add(nyVilkaarsresultat)

        every { behandlingsresultatService.hentBehandlingsresultat(behandling.id) } returns behandlingsresultat


        lagreLovvalgsperiodeMedl.utfør(prosessinstans)


        verify { medlPeriodeService.avvisPeriodeFeilregistrert(opprinneligLovvalgsperiode.hentMedlPeriodeID()) }
    }

    private fun lagVilkaarsresultat(vilkaar: Vilkaar?, oppfylt: Boolean) = Vilkaarsresultat().apply {
        this.vilkaar = vilkaar
        this.isOppfylt = oppfylt
    }

    private fun lagLovvalgsperiode(
        medlPeriodeID: Long?,
        lovvalgBestemmelse: LovvalgBestemmelse?,
        innvilgelsesResultat: InnvilgelsesResultat?
    ) = Lovvalgsperiode().apply {
        this.medlPeriodeID = medlPeriodeID
        this.bestemmelse = lovvalgBestemmelse
        this.innvilgelsesresultat = innvilgelsesResultat
    }

}
