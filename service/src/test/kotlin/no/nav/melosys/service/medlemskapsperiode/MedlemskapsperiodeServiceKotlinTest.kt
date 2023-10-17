package no.nav.melosys.service.medlemskapsperiode

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.folketrygden.MedlemAvFolketrygden
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.repository.MedlemskapsperiodeRepository
import no.nav.melosys.service.MedlemAvFolketrygdenService
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.medl.MedlPeriodeService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class MedlemskapsperiodeServiceKotlinTest {

    @MockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    lateinit var medlemAvFolketrygdenService: MedlemAvFolketrygdenService

    @MockK
    lateinit var trygdeavgiftsgrunnlagService: TrygdeavgiftsgrunnlagService

    @MockK
    lateinit var medlemskapsperiodeRepository: MedlemskapsperiodeRepository

    @RelaxedMockK
    lateinit var medlPeriodeService: MedlPeriodeService

    lateinit var medlemskapsperiodeService: MedlemskapsperiodeService

    @BeforeEach
    fun setUp() {
        medlemskapsperiodeService = MedlemskapsperiodeService(
            medlemskapsperiodeRepository,
            medlemAvFolketrygdenService,
            trygdeavgiftsgrunnlagService,
            behandlingsresultatService,
            medlPeriodeService
        )
    }

    @AfterEach
    fun tearDown() {

    }


    @Test
    fun `erstattMedlemskapsperioder skal kun opprette nye perioder når gammel liste er tom`() {
        setupHappyPathBehandling()
        every { medlemskapsperiodeRepository.save(any()) } returns Medlemskapsperiode()
        val medlemskapsperiode1 = Medlemskapsperiode().apply { fom = LocalDate.now().minusDays(1) }
        val medlemskapsperiode2 = Medlemskapsperiode().apply { fom = LocalDate.now().plusDays(1) }
        val nyListe = listOf(medlemskapsperiode1, medlemskapsperiode2)

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns Behandlingsresultat()

        medlemskapsperiodeService.erstattMedlemskapsperioder(nyListe, 1L, 2L)

        verify(exactly = 1) { medlPeriodeService.opprettPeriodeEndelig(2L, medlemskapsperiode1) }
        verify(exactly = 1) { medlPeriodeService.opprettPeriodeEndelig(2L, medlemskapsperiode2) }
    }

    @Test
    fun `erstattMedlemskapsperioder skal kun avvise gamle perioder når ny liste er tom`() {
        setupHappyPathBehandling()
        val gammelMedlemskapsperiode = Medlemskapsperiode().apply {
            medlPeriodeID = 1L
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        val gammelListe = listOf(gammelMedlemskapsperiode)
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply { medlemskapsperioder = gammelListe }

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns Behandlingsresultat().apply {
            this.medlemAvFolketrygden = medlemAvFolketrygden
        }

        medlemskapsperiodeService.erstattMedlemskapsperioder(emptyList(), 1L, 2L)

        verify(exactly = 1) { medlPeriodeService.avvisPeriodeOpphørt(1L) }
        verify(exactly = 0) { medlPeriodeService.opprettPeriodeEndelig(any<Long>(), any()) }
    }

    @Test
    fun `erstattMedlemskapsperioder skal opprette nye og avvise gamle når begge lister ikke er tomme og det er ingen felles elementer`() {
        setupHappyPathBehandling()
        every { medlemskapsperiodeRepository.save(any()) } returns Medlemskapsperiode()
        val gammelMedlemskapsperiode = Medlemskapsperiode().apply {
            medlPeriodeID = 1L
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply { medlemskapsperioder = listOf(gammelMedlemskapsperiode) }
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns Behandlingsresultat().apply {
            this.medlemAvFolketrygden = medlemAvFolketrygden
        }
        val nyMedlemskapsperiode = Medlemskapsperiode()

        medlemskapsperiodeService.erstattMedlemskapsperioder(listOf(nyMedlemskapsperiode), 1L, 2L)

        verify(exactly = 1) { medlPeriodeService.avvisPeriodeOpphørt(1L) }
        verify(exactly = 1) { medlPeriodeService.opprettPeriodeEndelig(2L, nyMedlemskapsperiode) }
    }

    @Test
    fun `erstattMedlemskapsperioder skal oppdatere periode når begge lister har felles elementer`() {
        setupHappyPathBehandling()
        every { medlemskapsperiodeRepository.save(any()) } returns Medlemskapsperiode()
        val fellesMedlemskapsperiode = Medlemskapsperiode().apply {
            medlPeriodeID = 1L
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        val gammelMedlemskapsperiode = Medlemskapsperiode().apply {
            medlPeriodeID = 2L
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        val gammelListe = listOf(fellesMedlemskapsperiode, gammelMedlemskapsperiode)
        val nyMedlemskapsperiode = Medlemskapsperiode()
        val nyListe = listOf(fellesMedlemskapsperiode, nyMedlemskapsperiode)
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply { medlemskapsperioder = gammelListe }
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns Behandlingsresultat().apply {
            this.medlemAvFolketrygden = medlemAvFolketrygden
        }

        medlemskapsperiodeService.erstattMedlemskapsperioder(nyListe, 1L, 2L)

        verify(exactly = 1) { medlPeriodeService.oppdaterPeriodeEndelig(2L, fellesMedlemskapsperiode) }
        verify(exactly = 1) { medlPeriodeService.avvisPeriodeOpphørt(2L) }
        verify(exactly = 1) { medlPeriodeService.opprettPeriodeEndelig(2L, nyMedlemskapsperiode) }
    }

    @Test
    fun `erstattMedlemskapsperioder skal kun avvise gamle perioder som er innvilget`() {
        setupHappyPathBehandling()
        val gammelMedlemskapsperiodeInnvilget = Medlemskapsperiode().apply {
            medlPeriodeID = 1L
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        val gammelMedlemskapsperiodeAvslag = Medlemskapsperiode().apply {
            medlPeriodeID = 2L
            innvilgelsesresultat = InnvilgelsesResultat.AVSLAATT
        }
        val gammelListe = listOf(gammelMedlemskapsperiodeInnvilget, gammelMedlemskapsperiodeAvslag)
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply { medlemskapsperioder = gammelListe }

        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns Behandlingsresultat().apply {
            this.medlemAvFolketrygden = medlemAvFolketrygden
        }

        medlemskapsperiodeService.erstattMedlemskapsperioder(emptyList(), 1L, 2L)

        verify(exactly = 1) { medlPeriodeService.avvisPeriodeOpphørt(1L) }
        verify(exactly = 0) { medlPeriodeService.avvisPeriodeOpphørt(2L) }
        verify(exactly = 0) { medlPeriodeService.opprettPeriodeEndelig(any<Long>(), any()) }
    }

    private fun setupHappyPathBehandling(
        sakstype: Sakstyper = Sakstyper.EU_EOS,
        behandlingstema: Behandlingstema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
    ) {
        val behandlingResultat = lagBehandlingsResultat(sakstype, behandlingstema)
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns behandlingResultat
    }

    private fun lagBehandlingsResultat(
        sakstype: Sakstyper = Sakstyper.EU_EOS,
        behandlingstema: Behandlingstema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
    ):
        Behandlingsresultat = Behandlingsresultat().apply {
        id = 1L
        behandling = Behandling().apply {
            tema = behandlingstema
            fagsak = Fagsak().apply {
                aktører.add(Aktoer().apply {
                    rolle = Aktoersroller.BRUKER
                    aktørId = "456"
                    type = sakstype
                })
            }
        }
    }
}
