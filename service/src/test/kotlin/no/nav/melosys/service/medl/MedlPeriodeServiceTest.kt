package no.nav.melosys.service.medl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.medl.KildedokumenttypeMedl
import no.nav.melosys.integrasjon.medl.MedlService
import no.nav.melosys.integrasjon.medl.StatusaarsakMedl
import no.nav.melosys.repository.AnmodningsperiodeRepository
import no.nav.melosys.repository.LovvalgsperiodeRepository
import no.nav.melosys.repository.MedlemskapsperiodeRepository
import no.nav.melosys.repository.UtpekingsperiodeRepository
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataFasade
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate

@ExtendWith(MockKExtension::class)
class MedlPeriodeServiceTest {
    @MockK
    lateinit var persondataFasade: PersondataFasade

    @RelaxedMockK
    lateinit var medlService: MedlService

    @MockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    lateinit var lovvalgsperiodeRepository: LovvalgsperiodeRepository

    @MockK
    lateinit var anmodningsperiodeRepository: AnmodningsperiodeRepository

    @RelaxedMockK
    lateinit var medlAnmodningsperiodeService: MedlAnmodningsperiodeService

    @MockK
    lateinit var utpekingsperiodeRepository: UtpekingsperiodeRepository

    @MockK
    lateinit var medlemskapsperiodeRepository: MedlemskapsperiodeRepository

    lateinit var medlPeriodeService: MedlPeriodeService

    @BeforeEach
    fun setUp() {
        medlPeriodeService = MedlPeriodeService(
            persondataFasade,
            medlService,
            behandlingsresultatService,
            lovvalgsperiodeRepository,
            medlAnmodningsperiodeService,
            utpekingsperiodeRepository,
            medlemskapsperiodeRepository
        )
    }

    @Test
    fun hentPeriodeListe() {
        val now = LocalDate.now()
        val plusMonths = LocalDate.now().plusMonths(2)

        medlPeriodeService.hentPeriodeListe(FNR, now, plusMonths)

        verify { medlService.hentPeriodeListe(FNR, now, plusMonths) }
    }

    @Test
    fun opprettPeriodeForeløpig() {
        setupHappyPathBehandling()
        val utpekingsperiode = Utpekingsperiode()
        every { medlService.opprettPeriodeForeløpig(any(), any(), any()) } returns MEDL_PERIODE_ID
        every { utpekingsperiodeRepository.save(any()) } returns utpekingsperiode

        medlPeriodeService.opprettPeriodeForeløpig(utpekingsperiode, 1L, true)

        verify { medlService.opprettPeriodeForeløpig(FNR, any(), KildedokumenttypeMedl.SED) }
        verify { utpekingsperiodeRepository.save(utpekingsperiode) }
    }

    @Test
    fun opprettPeriodeUnderAvklaring() {
        setupHappyPathBehandling()
        every { medlService.opprettPeriodeUnderAvklaring(any(), any(), any()) } returns MEDL_PERIODE_ID
        every { anmodningsperiodeRepository.save(any()) } returns Anmodningsperiode()

        medlPeriodeService.opprettPeriodeUnderAvklaring(Anmodningsperiode(), 1L, false)
        verify { medlService.opprettPeriodeUnderAvklaring(FNR, any(), KildedokumenttypeMedl.HENV_SOKNAD) }
        verify { medlAnmodningsperiodeService.lagreAnmodningsperiode(any()) }
    }

    @Test
    fun opprettPeriodeEndelig() {
        setupHappyPathBehandling()
        every { medlService.opprettPeriodeEndelig(any(), any(), any()) } returns MEDL_PERIODE_ID
        every { lovvalgsperiodeRepository.save(any()) } returns Lovvalgsperiode()

        medlPeriodeService.opprettPeriodeEndelig(Lovvalgsperiode(), 1L, true)

        verify { medlService.opprettPeriodeEndelig(FNR, any(), KildedokumenttypeMedl.SED) }
        verify { lovvalgsperiodeRepository.save(any()) }
    }

    @Test
    fun opprettPeriodeEndeligFtrl_feiler() {
        setupHappyPathBehandling()
        every { medlService.opprettPeriodeEndelig(FNR, any(), any()) } returns null

        shouldThrow<FunksjonellException> {
            medlPeriodeService.opprettPeriodeEndelig(1L, Medlemskapsperiode())
        }.message.shouldContain("Opprettelse av periode i MEDL feilet med retur av null medlPeriodeID")

        verify(exactly = 0) { medlemskapsperiodeRepository.save(any()) }
    }

    @Test
    fun opprettPeriodeEndeligFtrl() {
        setupHappyPathBehandling()
        val medlemskapsperiode = Medlemskapsperiode()
        every { medlemskapsperiodeRepository.save(any()) } returns medlemskapsperiode

        medlPeriodeService.opprettPeriodeEndelig(1L, medlemskapsperiode)

        verify { medlService.opprettPeriodeEndelig(FNR, any(), any()) }
        verify { medlemskapsperiodeRepository.save(medlemskapsperiode) }
    }

    @Test
    fun oppdaterPeriodeEndelig() {
        val lovvalgsperiode = Lovvalgsperiode().apply { medlPeriodeID = MEDL_PERIODE_ID }

        medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode, false)

        verify { medlService.oppdaterPeriodeEndelig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD) }
    }

    @Test
    fun oppdaterPeriodeForeløpig() {
        val lovvalgsperiode = Lovvalgsperiode().apply { medlPeriodeID = MEDL_PERIODE_ID }

        medlPeriodeService.oppdaterPeriodeForeløpig(lovvalgsperiode, false)

        verify { medlService.oppdaterPeriodeForeløpig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD) }
    }

    @Test
    fun avvisPeriode() {
        medlPeriodeService.avvisPeriode(MEDL_PERIODE_ID)

        verify { medlService.avvisPeriode(MEDL_PERIODE_ID, StatusaarsakMedl.AVVIST) }
    }

    @Test
    fun avvisPeriodeFeilregistrert() {
        medlPeriodeService.avvisPeriodeFeilregistrert(MEDL_PERIODE_ID)

        verify { medlService.avvisPeriode(MEDL_PERIODE_ID, StatusaarsakMedl.FEILREGISTRERT) }
    }

    @Test
    fun avvisPeriodeOpphørt() {
        medlPeriodeService.avvisPeriodeOpphørt(MEDL_PERIODE_ID)

        verify { medlService.avvisPeriode(MEDL_PERIODE_ID, StatusaarsakMedl.OPPHORT) }
    }

    @Test
    fun avsluttTidligereMedlPeriode_behandlingOgPeriodeFinnes_avviserPeriode() {
        val fagsak = Fagsak().apply {
            behandlinger = listOf(Behandling().apply {
                status = Behandlingsstatus.AVSLUTTET
                id = 1L
            })
        }
        val behandlingsresultat = Behandlingsresultat().apply {
            lovvalgsperioder =
                setOf(Lovvalgsperiode().apply { medlPeriodeID = MEDL_PERIODE_ID })
        }
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat

        medlPeriodeService.avsluttTidligerMedlPeriode(fagsak)

        verify { behandlingsresultatService.hentBehandlingsresultat(1L) }
        verify { medlService.avvisPeriode(MEDL_PERIODE_ID, StatusaarsakMedl.AVVIST) }
    }

    @Test
    fun avsluttTidligereMedlPeriode_ingenEksisterendePeriode_ingenPeriodeBlirAvvist() {
        val fagsak = Fagsak().apply {
            behandlinger = listOf(Behandling().apply {
                status = Behandlingsstatus.AVSLUTTET
                id = 1L
            })
        }
        val behandlingsresultat = Behandlingsresultat().apply { lovvalgsperioder = setOf() }
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat

        medlPeriodeService.avsluttTidligerMedlPeriode(fagsak)

        verify { behandlingsresultatService.hentBehandlingsresultat(1L) }
        verify(exactly = 0) { medlService.avvisPeriode(any(), any()) }
    }

    private fun setupHappyPathBehandling() {
        every { behandlingsresultatService.hentBehandlingsresultat(any()) } returns lagBehandlingsResultat()
        every { persondataFasade.hentFolkeregisterident(any()) } returns FNR
    }

    private fun lagBehandlingsResultat(): Behandlingsresultat = Behandlingsresultat().apply {
        behandling = Behandling().apply {
            fagsak = Fagsak().apply {
                aktører.add(Aktoer().apply {
                    rolle = Aktoersroller.BRUKER
                    aktørId = "456"
                })
            }
        }
    }

    companion object {
        private const val FNR = "12345678901"
        private const val MEDL_PERIODE_ID = 99L
    }
}
