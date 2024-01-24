package no.nav.melosys.service.medlemskapsperiode

import io.getunleash.FakeUnleash
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
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
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.repository.MedlemskapsperiodeRepository
import no.nav.melosys.service.MedlemAvFolketrygdenService
import no.nav.melosys.service.avgift.TrygdeavgiftsgrunnlagService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.medl.MedlPeriodeService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*

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

    val fakeUnleash: FakeUnleash = FakeUnleash()

    @RelaxedMockK
    lateinit var medlPeriodeService: MedlPeriodeService

    lateinit var medlemskapsperiodeService: MedlemskapsperiodeService

    @BeforeEach
    fun setUp() {
        medlemskapsperiodeService = MedlemskapsperiodeService(
            medlemskapsperiodeRepository,
            medlemAvFolketrygdenService,
            trygdeavgiftsgrunnlagService,
            medlPeriodeService,
            fakeUnleash
        )
    }

    @Test
    fun `erstattMedlemskapsperioder skal kun opprette nye perioder når gammel liste er tom`() {
        setupHappyPathBehandling()
        every { medlemskapsperiodeRepository.save(any()) } returns Medlemskapsperiode()
        val medlemskapsperiode1 = Medlemskapsperiode().apply {
            fom = LocalDate.now().minusDays(1)
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        val medlemskapsperiode2 = Medlemskapsperiode().apply {
            fom = LocalDate.now().plusDays(1)
            innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        }
        val nyListe = listOf(medlemskapsperiode1, medlemskapsperiode2)

        every { medlemAvFolketrygdenService.finnMedlemAvFolketrygden(1L) } returns Optional.empty()


        medlemskapsperiodeService.erstattMedlemskapsperioder(2L, 1L, nyListe)


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

        every { medlemAvFolketrygdenService.finnMedlemAvFolketrygden(1L) } returns Optional.of(medlemAvFolketrygden)


        medlemskapsperiodeService.erstattMedlemskapsperioder(2L, 1L, emptyList())


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
        every { medlemAvFolketrygdenService.finnMedlemAvFolketrygden(1L) } returns Optional.of(medlemAvFolketrygden)
        val nyMedlemskapsperiode = Medlemskapsperiode().apply { innvilgelsesresultat = InnvilgelsesResultat.INNVILGET }


        medlemskapsperiodeService.erstattMedlemskapsperioder(2L, 1L, listOf(nyMedlemskapsperiode))


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
        val nyMedlemskapsperiode = Medlemskapsperiode().apply { innvilgelsesresultat = InnvilgelsesResultat.INNVILGET }
        val nyListe = listOf(fellesMedlemskapsperiode, nyMedlemskapsperiode)
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply { medlemskapsperioder = gammelListe }
        every { medlemAvFolketrygdenService.finnMedlemAvFolketrygden(1L) } returns Optional.of(medlemAvFolketrygden)


        medlemskapsperiodeService.erstattMedlemskapsperioder(2L, 1L, nyListe)


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

        every { medlemAvFolketrygdenService.finnMedlemAvFolketrygden(1L) } returns Optional.of(medlemAvFolketrygden)


        medlemskapsperiodeService.erstattMedlemskapsperioder(2L, 1L, emptyList())


        verify(exactly = 1) { medlPeriodeService.avvisPeriodeOpphørt(1L) }
        verify(exactly = 0) { medlPeriodeService.avvisPeriodeOpphørt(2L) }
        verify(exactly = 0) { medlPeriodeService.opprettPeriodeEndelig(any<Long>(), any()) }
    }

    @Test
    fun `erstattMedlemskapsperioder skal opprette nye opphørte perioder`() {
        setupHappyPathBehandling()
        val medlemAvFolketrygden = MedlemAvFolketrygden().apply { medlemskapsperioder = emptyList() }
        every { medlemAvFolketrygdenService.finnMedlemAvFolketrygden(1L) } returns Optional.of(medlemAvFolketrygden)
        val nyInnvilgetMedlemskapsperiode = Medlemskapsperiode().apply { innvilgelsesresultat = InnvilgelsesResultat.INNVILGET }
        val nyOpphørtMedlemskapsperiode = Medlemskapsperiode().apply { innvilgelsesresultat = InnvilgelsesResultat.OPPHØRT }


        medlemskapsperiodeService.erstattMedlemskapsperioder(2L, 1L, listOf(nyInnvilgetMedlemskapsperiode, nyOpphørtMedlemskapsperiode))


        verify(exactly = 1) { medlPeriodeService.opprettOpphørtPeriode(2L, nyOpphørtMedlemskapsperiode) }
    }

    @Test
    fun `opprettEllerOppdaterMedlPeriode oppretter når medlId ikke finnes`() {
        val medlemskapsperiodeUtenMedlId = Medlemskapsperiode()

        medlemskapsperiodeService.opprettEllerOppdaterMedlPeriode(1L, medlemskapsperiodeUtenMedlId)

        verify(exactly = 1) { medlPeriodeService.opprettPeriodeEndelig(1L, medlemskapsperiodeUtenMedlId) }
    }


    @Test
    fun `hentGyldigeTrygdedekninger returnerer GYLDIGE_TRYGDEDEKNINGER_2_7 og GYLDIGE_TRYGDEDEKNINGER_2_8 når MELOSYS_FOLKETRYGDEN_2_7 er enabled`() {
        fakeUnleash.enable(ToggleName.MELOSYS_FOLKETRYGDEN_2_7)
        val forventet = listOf(MedlemskapsperiodeService.GYLDIGE_TRYGDEDEKNINGER_2_7, MedlemskapsperiodeService.GYLDIGE_TRYGDEDEKNINGER_2_8).flatten()

        val result = medlemskapsperiodeService.hentGyldigeTrygdedekninger()

        result.shouldContainExactlyInAnyOrder(forventet)
    }

    @Test
    fun `hentGyldigeTrygdedekninger returnerer GYLDIGE_TRYGDEDEKNINGER_2_8 når MELOSYS_FOLKETRYGDEN_2_7 er disabled`() {
        fakeUnleash.disable(ToggleName.MELOSYS_FOLKETRYGDEN_2_7)

        val expected = MedlemskapsperiodeService.GYLDIGE_TRYGDEDEKNINGER_2_8

        val result = medlemskapsperiodeService.hentGyldigeTrygdedekninger()

        assertEquals(expected, result)
    }

    @Test
    fun `opprettEllerOppdaterMedlPeriode oppdaterer når medlId finnes`() {
        val medlemskapsperiodeMedMedlId = Medlemskapsperiode().apply { medlPeriodeID = 1L }

        medlemskapsperiodeService.opprettEllerOppdaterMedlPeriode(1L, medlemskapsperiodeMedMedlId)

        verify(exactly = 1) { medlPeriodeService.oppdaterPeriodeEndelig(1L, medlemskapsperiodeMedMedlId) }
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
