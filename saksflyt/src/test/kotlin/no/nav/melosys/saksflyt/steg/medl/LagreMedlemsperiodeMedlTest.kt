package no.nav.melosys.saksflyt.steg.medl

import io.mockk.called
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.fagsak
import no.nav.melosys.domain.forTest
import no.nav.melosys.domain.medlemskapsperiodeForTest
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.saksflytapi.domain.behandling
import no.nav.melosys.saksflytapi.domain.forTest
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.ftrl.medlemskapsperiode.MedlemskapsperiodeService
import no.nav.melosys.service.medl.MedlPeriodeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class LagreMedlemsperiodeMedlTest {
    @RelaxedMockK
    private lateinit var medlPeriodeService: MedlPeriodeService

    @RelaxedMockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    private lateinit var medlemskapsperiodeService: MedlemskapsperiodeService

    private lateinit var lagreMedlemsperiodeMedl: LagreMedlemsperiodeMedl
    private lateinit var prosessinstans: Prosessinstans

    @BeforeEach
    fun setup() {
        lagreMedlemsperiodeMedl = LagreMedlemsperiodeMedl(medlemskapsperiodeService, behandlingsresultatService)
        prosessinstans = lagProsessInstans()
    }

    @Test
    fun utfør_ingenMedlemskapsperioder_gjørIngenting() {
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns lagBehandlingsresultat(emptyList())

        lagreMedlemsperiodeMedl.utfør(prosessinstans)

        verify { medlPeriodeService wasNot called }
    }

    @Test
    fun utfør_ingenInnvilgedeMedlemskapsperioder_erAvslag_gjørIngenting() {
        val medlemskapsperioder = listOf(
            lagMedlemskapsperiode(InnvilgelsesResultat.AVSLAATT),
            lagMedlemskapsperiode(InnvilgelsesResultat.AVSLAATT)
        )
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns lagBehandlingsresultat(medlemskapsperioder)


        lagreMedlemsperiodeMedl.utfør(prosessinstans)


        verify { medlPeriodeService wasNot called }
    }

    @Test
    fun utfør_innvilgedeMedlemskapsperioder_oppretterEllerOppdatererMedlPerioder() {
        val medlemskapsperioder = listOf(
            lagMedlemskapsperiode(InnvilgelsesResultat.INNVILGET, 1),
            lagMedlemskapsperiode(InnvilgelsesResultat.INNVILGET, 2)
        )
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns lagBehandlingsresultat(medlemskapsperioder)


        lagreMedlemsperiodeMedl.utfør(lagProsessInstans())


        verify(exactly = 2) { medlemskapsperiodeService.opprettEllerOppdaterMedlPeriode(BEHANDLING_ID, any()) }
    }

    @Test
    fun utfør_avslutterMedlemskapsperioder_nårDetErNyVurderingOgInnvilgelse() {
        val innvilgetMedlemskapsperiode = lagMedlemskapsperiode(InnvilgelsesResultat.INNVILGET)
        val medlemskapsperioder =
            listOf(lagMedlemskapsperiode(InnvilgelsesResultat.AVSLAATT), innvilgetMedlemskapsperiode)
        val opprinneligBehandling = Behandling.forTest { id = 1L }
        val prosessinstans = lagProsessInstans().also {
            it.hentBehandling.type = Behandlingstyper.NY_VURDERING
            it.hentBehandling.opprinneligBehandling = opprinneligBehandling
        }
        val behandlingsresultat = lagBehandlingsresultat(medlemskapsperioder).also {
            it.behandling = prosessinstans.behandling
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        lagreMedlemsperiodeMedl.utfør(prosessinstans)


        verify { medlemskapsperiodeService.erstattMedlemskapsperioder(BEHANDLING_ID, 1L, medlemskapsperioder) }
    }

    @Test
    fun `opphør medlemskapsperioder ved manglende innbetaling av trygdeavgift som fører til opphør av medlemskap`() {
        val opphørtMedlemskapsperiode = lagMedlemskapsperiode(InnvilgelsesResultat.OPPHØRT)
        val medlemskapsperioder = listOf(opphørtMedlemskapsperiode)

        val opprinneligBehandling = Behandling.forTest { id = 1L }
        val prosessinstans = lagProsessInstans().also {
            it.hentBehandling.type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
            it.hentBehandling.opprinneligBehandling = opprinneligBehandling
        }
        val behandlingsresultat = lagBehandlingsresultat(medlemskapsperioder).also {
            it.behandling = prosessinstans.behandling
            it.type = Behandlingsresultattyper.OPPHØRT
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        lagreMedlemsperiodeMedl.utfør(prosessinstans)


        verify { medlemskapsperiodeService.erstattMedlemskapsperioder(BEHANDLING_ID, 1L, medlemskapsperioder) }
    }

    @Test
    fun `erstatt medlemskapsperioder ved manglende innbetaling av trygdeavgift og delvis opphør`() {
        val innvilgetMedlemskapsperiode = lagMedlemskapsperiode(InnvilgelsesResultat.INNVILGET)
        val opphørtMedlemskapsperiode = lagMedlemskapsperiode(InnvilgelsesResultat.OPPHØRT)
        val medlemskapsperioder = listOf(innvilgetMedlemskapsperiode, opphørtMedlemskapsperiode)

        val opprinneligBehandling = Behandling.forTest { id = 1L }
        val prosessinstans = lagProsessInstans().also {
            it.hentBehandling.type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
            it.hentBehandling.opprinneligBehandling = opprinneligBehandling
        }
        val behandlingsresultat = lagBehandlingsresultat(medlemskapsperioder).also {
            it.behandling = prosessinstans.behandling
            it.type = Behandlingsresultattyper.DELVIS_OPPHØRT
        }
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        lagreMedlemsperiodeMedl.utfør(prosessinstans)


        verify { medlemskapsperiodeService.erstattMedlemskapsperioder(BEHANDLING_ID, 1L, medlemskapsperioder) }
    }

    @Test
    fun `Utfør skal gjøre ingenting hvis dette er EØS pensjonist sak`() {
        val opprinneligBehandling = Behandling.forTest {
            id = 1L
            type = Behandlingstyper.FØRSTEGANG
            tema = Behandlingstema.PENSJONIST
        }

        val prosessinstans = lagProsessInstans().also {
            it.hentBehandling.tema = Behandlingstema.PENSJONIST
            it.hentBehandling.fagsak.type = Sakstyper.EU_EOS
            it.hentBehandling.opprinneligBehandling = opprinneligBehandling
        }

        val behandlingsresultat = Behandlingsresultat.forTest {
            behandling = prosessinstans.behandling
        }

        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        lagreMedlemsperiodeMedl.utfør(prosessinstans)


        verify { medlPeriodeService wasNot called }
        verify { medlemskapsperiodeService wasNot called }
    }


    private fun lagProsessInstans(): Prosessinstans =
        Prosessinstans.forTest {
            behandling {
                id = BEHANDLING_ID
                fagsak {
                    type = Sakstyper.FTRL
                }
            }
        }

    private fun lagBehandlingsresultat(medlemskapsperioder: List<Medlemskapsperiode>): Behandlingsresultat =
        Behandlingsresultat.forTest {
            type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
            this.behandling = Behandling.forTest {
                type = Behandlingstyper.FØRSTEGANG
                fagsak {
                    type = Sakstyper.FTRL
                }
            }
        }.also { result ->
            result.medlemskapsperioder = medlemskapsperioder.toMutableSet()
        }

    private fun lagMedlemskapsperiode(innvilgelsesResultat: InnvilgelsesResultat, id: Long? = null): Medlemskapsperiode =
        medlemskapsperiodeForTest {
            this.id = id
            this.innvilgelsesresultat = innvilgelsesResultat
        }

    companion object {
        const val BEHANDLING_ID: Long = 123L
    }
}
