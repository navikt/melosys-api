package no.nav.melosys.saksflyt.steg.medl

import io.mockk.called
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.medl.MedlPeriodeService
import no.nav.melosys.service.medlemskapsperiode.MedlemskapsperiodeService
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
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns lagBehandlingsresulat(emptyList())

        lagreMedlemsperiodeMedl.utfør(prosessinstans)

        verify { medlPeriodeService wasNot called }
    }

    @Test
    fun utfør_ingenInnvilgedeMedlemskapsperioder_erAvslag_gjørIngenting() {
        val medlemskapsperioder = listOf(
            lagMedlemskapsperiode(InnvilgelsesResultat.AVSLAATT),
            lagMedlemskapsperiode(InnvilgelsesResultat.AVSLAATT)
        )
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns lagBehandlingsresulat(medlemskapsperioder)


        lagreMedlemsperiodeMedl.utfør(prosessinstans)


        verify { medlPeriodeService wasNot called }
    }

    @Test
    fun utfør_innvilgedeMedlemskapsperioder_oppretterEllerOppdatererMedlPerioder() {
        val medlemskapsperioder = listOf(
            lagMedlemskapsperiode(InnvilgelsesResultat.INNVILGET),
            lagMedlemskapsperiode(InnvilgelsesResultat.INNVILGET)
        )
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns lagBehandlingsresulat(medlemskapsperioder)


        lagreMedlemsperiodeMedl.utfør(lagProsessInstans())


        verify(exactly = 2) { medlemskapsperiodeService.opprettEllerOppdaterMedlPeriode(BEHANDLING_ID, any()) }
    }

    @Test
    fun utfør_avslutterMedlemskapsperioder_nårDetErNyVurderingOgInnvilgelse() {
        val innvilgetMedlemskapsperiode = lagMedlemskapsperiode(InnvilgelsesResultat.INNVILGET)
        val medlemskapsperioder =
            listOf(lagMedlemskapsperiode(InnvilgelsesResultat.AVSLAATT), innvilgetMedlemskapsperiode)
        val opprinneligBehandling = Behandling()
        opprinneligBehandling.id = 1L
        val prosessinstans = lagProsessInstans()
        prosessinstans.behandling.type = Behandlingstyper.NY_VURDERING
        prosessinstans.behandling.opprinneligBehandling = opprinneligBehandling
        val behandlingsresultat = lagBehandlingsresulat(medlemskapsperioder)
        behandlingsresultat.behandling = prosessinstans.behandling
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        lagreMedlemsperiodeMedl.utfør(prosessinstans)


        verify { medlemskapsperiodeService.erstattMedlemskapsperioder(BEHANDLING_ID, 1L, medlemskapsperioder) }
    }

    @Test
    fun `opphør medlemskapsperioder ved manglende innbetaling av trygdeavgift som fører til opphør av medlemskap`() {
        val opprinneligBehandling = Behandling().apply {
            id = 1L
        }
        val prosessinstans = lagProsessInstans().apply {
            behandling.type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
            behandling.opprinneligBehandling = opprinneligBehandling
        }
        val behandlingsresultat = lagBehandlingsresulat(emptyList())
        behandlingsresultat.behandling = prosessinstans.behandling
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        lagreMedlemsperiodeMedl.utfør(prosessinstans)


        verify { medlemskapsperiodeService.erstattMedlemskapsperioder(BEHANDLING_ID,  1L, emptyList()) }
    }

    @Test
    fun `erstatt medlemskapsperioder ved manglende innbetaling av trygdeavgift og delvis opphør`() {
        val innvilgetMedlemskapsperiode = lagMedlemskapsperiode(InnvilgelsesResultat.INNVILGET)
        val opphørtMedlemskapsperiode = lagMedlemskapsperiode(InnvilgelsesResultat.OPPHØRT)
        val medlemskapsperioder = listOf(innvilgetMedlemskapsperiode, opphørtMedlemskapsperiode)

        val opprinneligBehandling = Behandling()
        opprinneligBehandling.id = 1L
        val prosessinstans = lagProsessInstans().apply {
            behandling.type = Behandlingstyper.MANGLENDE_INNBETALING_TRYGDEAVGIFT
            behandling.opprinneligBehandling = opprinneligBehandling
        }
        val behandlingsresultat = lagBehandlingsresulat(medlemskapsperioder)
        behandlingsresultat.behandling = prosessinstans.behandling
        every { behandlingsresultatService.hentBehandlingsresultat(BEHANDLING_ID) } returns behandlingsresultat


        lagreMedlemsperiodeMedl.utfør(prosessinstans)


        verify { medlemskapsperiodeService.erstattMedlemskapsperioder(BEHANDLING_ID, 1L, medlemskapsperioder) }
    }

    private fun lagProsessInstans(): Prosessinstans {
        val behandling = Behandling().apply {
            id = BEHANDLING_ID
        }

        return Prosessinstans().apply {
            this.behandling = behandling
        }
    }

    private fun lagBehandlingsresulat(medlemskapsperioder: List<Medlemskapsperiode>): Behandlingsresultat {
        val medlemAvFolketrygden = MedlemAvFolketrygden()
        medlemAvFolketrygden.medlemskapsperioder = medlemskapsperioder
        val behandling = Behandling()
        behandling.type = Behandlingstyper.FØRSTEGANG

        val behandlingsresultat = Behandlingsresultat()
        behandlingsresultat.type = Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN
        behandlingsresultat.medlemAvFolketrygden = medlemAvFolketrygden
        behandlingsresultat.behandling = behandling

        return behandlingsresultat
    }

    private fun lagMedlemskapsperiode(innvilgelsesResultat: InnvilgelsesResultat): Medlemskapsperiode {
        val medlemskapsperiode = Medlemskapsperiode()
        medlemskapsperiode.innvilgelsesresultat = innvilgelsesResultat
        return medlemskapsperiode
    }

    companion object {
        const val BEHANDLING_ID: Long = 123L
    }
}
