package no.nav.melosys.service

import io.getunleash.FakeUnleash
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysninger
import no.nav.melosys.domain.mottatteopplysninger.Soeknad
import no.nav.melosys.domain.mottatteopplysninger.data.Periode
import no.nav.melosys.domain.mottatteopplysninger.data.Soeknadsland
import no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder.FysiskArbeidssted
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.helseutgiftdekkesperiode.HelseutgiftDekkesPeriodeService
import no.nav.melosys.service.kontroll.feature.ufm.UfmKontrollService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerFactory
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerRequest
import no.nav.melosys.service.registeropplysninger.RegisteropplysningerService
import no.nav.melosys.service.saksbehandling.SaksbehandlingRegler
import no.nav.melosys.service.saksopplysninger.OppfriskSaksopplysningerService
import no.nav.melosys.service.unntak.AnmodningsperiodeService
import no.nav.melosys.service.vilkaar.InngangsvilkaarService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class OppfriskSaksopplysningerServiceTest {

    @RelaxedMockK
    lateinit var anmodningsperiodeService: AnmodningsperiodeService

    @RelaxedMockK
    lateinit var behandlingService: BehandlingService

    @RelaxedMockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @RelaxedMockK
    lateinit var ufmKontrollService: UfmKontrollService

    @RelaxedMockK
    lateinit var inngangsvilkaarService: InngangsvilkaarService

    @RelaxedMockK
    lateinit var registeropplysningerService: RegisteropplysningerService

    @RelaxedMockK
    lateinit var persondataFasade: PersondataFasade

    @RelaxedMockK
    lateinit var saksbehandlingRegler: SaksbehandlingRegler

    @RelaxedMockK
    lateinit var årsavregningService: ÅrsavregningService

    @RelaxedMockK
    lateinit var helseutgiftDekkesPeriodeService: HelseutgiftDekkesPeriodeService

    private lateinit var oppfriskSaksopplysningerService: OppfriskSaksopplysningerService
    private lateinit var fakeUnleash: FakeUnleash

    @BeforeEach
    fun setUp() {
        fakeUnleash = FakeUnleash()
        val registeropplysningerFactory = RegisteropplysningerFactory(saksbehandlingRegler, fakeUnleash)
        oppfriskSaksopplysningerService = OppfriskSaksopplysningerService(
            anmodningsperiodeService,
            behandlingService,
            behandlingsresultatService,
            ufmKontrollService,
            inngangsvilkaarService,
            registeropplysningerService,
            persondataFasade,
            registeropplysningerFactory,
            årsavregningService,
            helseutgiftDekkesPeriodeService
        )
    }

    @Test
    fun `oppfrisk saksopplysning`() {
        every { behandlingService.hentBehandling(any()) } returns lagBehandling()
        every { persondataFasade.hentFolkeregisterident(any()) } returns "322211"

        oppfriskSaksopplysningerService.oppdaterRegisteropplysningerOgTilbakestillBehandlingsresultat(BEHANDLING_ID, false)

        verify { behandlingsresultatService.tømBehandlingsresultat(any()) }
        verify { registeropplysningerService.slettRegisterOpplysninger(BEHANDLING_ID) }
        verify { registeropplysningerService.hentOgLagreOpplysninger(any<RegisteropplysningerRequest>()) }
    }

    @Test
    fun `oppfrisk saksopplysning_ikke_tilbakestill`() {
        every { behandlingService.hentBehandling(any()) } returns lagBehandling()
        every { persondataFasade.hentFolkeregisterident(any()) } returns "322211"

        oppfriskSaksopplysningerService.oppdaterRegisteropplysningerForEøsPensjonist(BEHANDLING_ID, false)

        verify(exactly = 0) { behandlingsresultatService.tømBehandlingsresultat(any()) }
        verify { registeropplysningerService.slettRegisterOpplysninger(BEHANDLING_ID) }
        verify { registeropplysningerService.hentOgLagreOpplysninger(any<RegisteropplysningerRequest>()) }
    }

    @Test
    fun `oppfrisk saksopplysning virksomhet ingen flyt`() {
        val behandling = lagBehandling()
        val virksomhet = Aktoer().apply {
            rolle = Aktoersroller.VIRKSOMHET
        }
        behandling.fagsak.leggTilAktør(virksomhet)
        behandling.type = Behandlingstyper.HENVENDELSE
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { saksbehandlingRegler.harIngenFlyt(any(), any(), any(), any()) } returns true
        every { inngangsvilkaarService.skalVurdereInngangsvilkår(any()) } returns false

        oppfriskSaksopplysningerService.oppdaterRegisteropplysningerOgTilbakestillBehandlingsresultat(BEHANDLING_ID, false)

        verify { behandlingsresultatService.tømBehandlingsresultat(any()) }
        verify { registeropplysningerService.slettRegisterOpplysninger(BEHANDLING_ID) }
        verify(exactly = 0) { inngangsvilkaarService.vurderOgLagreInngangsvilkår(any(), any(), any(), any()) }
    }

    @Test
    fun `oppfrisk saksopplysning anmodning om unntak sendt feiler`() {
        every { behandlingService.hentBehandling(any()) } returns lagBehandling()
        lagBehandling().tema = Behandlingstema.ANMODNING_OM_UNNTAK_HOVEDREGEL
        every { anmodningsperiodeService.harSendtAnmodningsperiode(BEHANDLING_ID) } returns true

        val exception = shouldThrow<FunksjonellException> {
            oppfriskSaksopplysningerService.oppdaterRegisteropplysningerOgTilbakestillBehandlingsresultat(BEHANDLING_ID, false)
        }
        exception.message shouldContain "Anmodning om unntak er sendt"
    }

    @Test
    fun `oppfrisk saksopplysning med SED kaller kontroller`() {
        val behandling = lagBehandling()
        behandling.tema = Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE
        behandling.saksopplysninger.add(lagSED())
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { persondataFasade.hentFolkeregisterident(any()) } returns "322211"

        oppfriskSaksopplysningerService.oppdaterRegisteropplysningerOgTilbakestillBehandlingsresultat(BEHANDLING_ID, false)

        verify { ufmKontrollService.utførKontrollerOgRegistrerFeil(BEHANDLING_ID) }
    }

    @Test
    fun `oppfrisk saksopplysning har ikke oppfylt inngangsvilkår oppdaterer type`() {
        val behandling = lagBehandling()
        behandling.fagsak.type = Sakstyper.EU_EOS

        every { behandlingService.hentBehandling(any()) } returns behandling
        every { persondataFasade.hentFolkeregisterident(any()) } returns "322211"
        every { inngangsvilkaarService.skalVurdereInngangsvilkår(any()) } returns true
        every { inngangsvilkaarService.vurderOgLagreInngangsvilkår(any(), any(), any(), any()) } returns true

        oppfriskSaksopplysningerService.oppdaterRegisteropplysningerOgTilbakestillBehandlingsresultat(BEHANDLING_ID, false)

        verify { inngangsvilkaarService.vurderOgLagreInngangsvilkår(eq(behandling.id), eq(listOf("SE")), eq(false), any<ErPeriode>()) }
    }

    @Test
    fun `oppfrisk saksopplysning skal ikke hente inngangsvilkår henter ikke inngangsvilkår`() {
        val behandling = lagBehandling()
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { persondataFasade.hentFolkeregisterident(any()) } returns "322211"
        every { inngangsvilkaarService.skalVurdereInngangsvilkår(any()) } returns false

        oppfriskSaksopplysningerService.oppdaterRegisteropplysningerOgTilbakestillBehandlingsresultat(BEHANDLING_ID, false)

        verify(exactly = 0) { inngangsvilkaarService.vurderOgLagreInngangsvilkår(any(), any(), any(), any()) }
    }

    @Test
    fun `oppfrisk saksopplysning utleder periode for årsavregning`() {
        val behandling = lagBehandling()
        behandling.type = Behandlingstyper.ÅRSAVREGNING
        every { behandlingService.hentBehandling(any()) } returns behandling
        every { persondataFasade.hentFolkeregisterident(any()) } returns "322211"
        every { inngangsvilkaarService.skalVurdereInngangsvilkår(any()) } returns false
        every { årsavregningService.finnGjeldendeÅrForÅrsavregning(any()) } returns 2023

        oppfriskSaksopplysningerService.oppdaterRegisteropplysningerOgTilbakestillBehandlingsresultat(BEHANDLING_ID, false)

        verify { registeropplysningerService.hentOgLagreOpplysninger(any<RegisteropplysningerRequest>()) }
    }

    private fun lagSED() = Saksopplysning().apply {
        type = SaksopplysningType.SEDOPPL
        dokument = SedDokument().apply {
            lovvalgsperiode = no.nav.melosys.domain.dokument.medlemskap.Periode(LocalDate.now(), null)
        }
    }

    private fun lagBehandling() = Behandling.forTest {
        id = BEHANDLING_ID
        type = Behandlingstyper.FØRSTEGANG
        tema = Behandlingstema.UTSENDT_ARBEIDSTAKER
        fagsak {
            medBruker()
        }
        saksopplysninger = mutableSetOf<Saksopplysning>().apply {
            add(Saksopplysning().apply {
                type = SaksopplysningType.PERSOPL
            })
        }

        val soeknad = Soeknad().apply {
            arbeidPaaLand.fysiskeArbeidssteder = listOf(FysiskArbeidssted())
            periode = Periode(LocalDate.now(), LocalDate.now().plusYears(2))
            soeknadsland = Soeknadsland(listOf("SE"), false)
        }

        mottatteOpplysninger = MottatteOpplysninger().apply {
            mottatteOpplysningerData = soeknad
        }
    }

    companion object {
        private const val BEHANDLING_ID = 11L
    }
}
