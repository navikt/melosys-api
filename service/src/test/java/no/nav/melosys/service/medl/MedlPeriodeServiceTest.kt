package no.nav.melosys.service.medl

import io.mockk.impl.annotations.MockK
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
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.time.LocalDate
import java.util.List
import java.util.Set

@ExtendWith(MockKExtension::class)
class MedlPeriodeServiceTest {
    @MockK
    lateinit var persondataFasade: PersondataFasade

    @MockK
    lateinit var medlService: MedlService

    @MockK
    lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    lateinit var lovvalgsperiodeRepository: LovvalgsperiodeRepository

    @MockK
    lateinit var anmodningsperiodeRepository: AnmodningsperiodeRepository

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
            anmodningsperiodeRepository,
            utpekingsperiodeRepository,
            medlemskapsperiodeRepository
        )
    }

    @Test
    fun hentPeriodeListe() {
        medlPeriodeService.hentPeriodeListe(FNR, LocalDate.now(), LocalDate.now().plusMonths(2))
        verify { medlService.hentPeriodeListe(any(), any(), any()) }
    }

    @Test
    fun opprettPeriodeForeløpig() {
        setupPeriodeForeløpig()
        medlPeriodeService.opprettPeriodeForeløpig(Utpekingsperiode(), 1L, true)
        Mockito.verify(medlService).opprettPeriodeForeløpig(
            ArgumentMatchers.eq(FNR), ArgumentMatchers.any(
                Utpekingsperiode::class.java
            ), ArgumentMatchers.eq(KildedokumenttypeMedl.SED)
        )
        Mockito.verify(utpekingsperiodeRepository).save(
            ArgumentMatchers.any(
                Utpekingsperiode::class.java
            )
        )
    }

    @Test
    fun opprettPeriodeUnderAvklaring() {
        setupPeriodeUnderAvklaring()
        medlPeriodeService.opprettPeriodeUnderAvklaring(Anmodningsperiode(), 1L, false)
        Mockito.verify(medlService).opprettPeriodeUnderAvklaring(
            ArgumentMatchers.eq(FNR), ArgumentMatchers.any(
                Anmodningsperiode::class.java
            ), ArgumentMatchers.eq(KildedokumenttypeMedl.HENV_SOKNAD)
        )
        Mockito.verify(anmodningsperiodeRepository).save(
            ArgumentMatchers.any(
                Anmodningsperiode::class.java
            )
        )
    }

    @Test
    fun opprettPeriodeEndelig() {
        setupPeriodeEndelig()
        medlPeriodeService.opprettPeriodeEndelig(Lovvalgsperiode(), 1L, true)
        Mockito.verify(medlService).opprettPeriodeEndelig(
            ArgumentMatchers.eq(FNR), ArgumentMatchers.any(
                Lovvalgsperiode::class.java
            ), ArgumentMatchers.eq(KildedokumenttypeMedl.SED)
        )
        Mockito.verify(lovvalgsperiodeRepository).save(
            ArgumentMatchers.any(
                Lovvalgsperiode::class.java
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun opprettPeriodeEndeligFtrl_feiler() {
        setupHappyPathBehandling()
        Mockito.`when`(
            medlService!!.opprettPeriodeEndelig(
                ArgumentMatchers.eq(FNR), ArgumentMatchers.any(
                    Medlemskapsperiode::class.java
                ), ArgumentMatchers.any(KildedokumenttypeMedl::class.java)
            )
        )
            .thenReturn(null)
        Assertions.assertThatThrownBy { medlPeriodeService.opprettPeriodeEndelig(1L, Medlemskapsperiode()) }
            .isInstanceOf(FunksjonellException::class.java)
            .hasMessageContaining("Opprettelse av periode i MEDL feilet med retur av null medlPeriodeID")
        Mockito.verifyNoInteractions(medlemskapsperiodeRepository)
    }

    @Test
    @Throws(Exception::class)
    fun opprettPeriodeEndeligFtrl() {
        setupHappyPathBehandling()
        medlPeriodeService.opprettPeriodeEndelig(1L, Medlemskapsperiode())
        Mockito.verify(medlService).opprettPeriodeEndelig(
            ArgumentMatchers.eq(FNR), ArgumentMatchers.any(
                Medlemskapsperiode::class.java
            ), ArgumentMatchers.any(KildedokumenttypeMedl::class.java)
        )
        Mockito.verify(medlemskapsperiodeRepository).save(
            ArgumentMatchers.any(
                Medlemskapsperiode::class.java
            )
        )
    }

    @Test
    fun oppdaterPeriodeEndelig() {
        val lovvalgsperiode = Lovvalgsperiode()
        lovvalgsperiode.medlPeriodeID = MEDL_PERIODE_ID
        medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode, false)
        Mockito.verify(medlService).oppdaterPeriodeEndelig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD)
    }

    @Test
    fun oppdaterPeriodeForeløpig() {
        val lovvalgsperiode = Lovvalgsperiode()
        lovvalgsperiode.medlPeriodeID = MEDL_PERIODE_ID
        medlPeriodeService.oppdaterPeriodeForeløpig(lovvalgsperiode, false)
        Mockito.verify(medlService).oppdaterPeriodeForeløpig(lovvalgsperiode, KildedokumenttypeMedl.HENV_SOKNAD)
    }

    @Test
    fun avvisPeriode() {
        medlPeriodeService.avvisPeriode(MEDL_PERIODE_ID)
        Mockito.verify(medlService).avvisPeriode(MEDL_PERIODE_ID, StatusaarsakMedl.AVVIST)
    }

    @Test
    fun avvisPeriodeFeilregistrert() {
        medlPeriodeService.avvisPeriodeFeilregistrert(MEDL_PERIODE_ID)
        Mockito.verify(medlService).avvisPeriode(MEDL_PERIODE_ID, StatusaarsakMedl.FEILREGISTRERT)
    }

    @Test
    fun avvisPeriodeOpphørt() {
        medlPeriodeService.avvisPeriodeOpphørt(MEDL_PERIODE_ID)
        Mockito.verify(medlService).avvisPeriode(MEDL_PERIODE_ID, StatusaarsakMedl.OPPHORT)
    }

    @Test
    fun avsluttTidligereMedlPeriode_behandlingOgPeriodeFinnes_avviserPeriode() {
        val behandling = Behandling()
        behandling.status = Behandlingsstatus.AVSLUTTET
        behandling.id = 1L
        val fagsak = Fagsak()
        fagsak.behandlinger = List.of(behandling)
        val lovvalgsperiode = Lovvalgsperiode()
        lovvalgsperiode.medlPeriodeID = MEDL_PERIODE_ID
        val behandlingsresultat = Behandlingsresultat()
        behandlingsresultat.lovvalgsperioder = Set.of(lovvalgsperiode)
        Mockito.`when`(behandlingsresultatService!!.hentBehandlingsresultat(1L)).thenReturn(behandlingsresultat)
        medlPeriodeService.avsluttTidligerMedlPeriode(fagsak)
        Mockito.verify(behandlingsresultatService).hentBehandlingsresultat(1L)
        Mockito.verify(medlService).avvisPeriode(MEDL_PERIODE_ID, StatusaarsakMedl.AVVIST)
    }

    @Test
    fun avsluttTidligereMedlPeriode_ingenEksisterendePeriode_ingenPeriodeBlirAvvist() {
        val behandling = Behandling()
        behandling.status = Behandlingsstatus.AVSLUTTET
        behandling.id = 1L
        val fagsak = Fagsak()
        fagsak.behandlinger = List.of(behandling)
        val behandlingsresultat = Behandlingsresultat()
        behandlingsresultat.lovvalgsperioder = Set.of()
        Mockito.`when`(behandlingsresultatService!!.hentBehandlingsresultat(1L)).thenReturn(behandlingsresultat)
        medlPeriodeService.avsluttTidligerMedlPeriode(fagsak)
        Mockito.verify(behandlingsresultatService).hentBehandlingsresultat(1L)
        Mockito.verify(medlService, Mockito.never()).avvisPeriode(
            ArgumentMatchers.anyLong(), ArgumentMatchers.any(
                StatusaarsakMedl::class.java
            )
        )
    }

    private fun setupPeriodeForeløpig() {
        Mockito.`when`(
            medlService!!.opprettPeriodeForeløpig(
                ArgumentMatchers.anyString(), ArgumentMatchers.any(
                    PeriodeOmLovvalg::class.java
                ), ArgumentMatchers.any(KildedokumenttypeMedl::class.java)
            )
        )
            .thenReturn(MEDL_PERIODE_ID)
        setupHappyPathBehandling()
    }

    private fun setupPeriodeUnderAvklaring() {
        Mockito.`when`(
            medlService!!.opprettPeriodeUnderAvklaring(
                ArgumentMatchers.anyString(), ArgumentMatchers.any(
                    PeriodeOmLovvalg::class.java
                ), ArgumentMatchers.any(KildedokumenttypeMedl::class.java)
            )
        )
            .thenReturn(MEDL_PERIODE_ID)
        setupHappyPathBehandling()
    }

    private fun setupPeriodeEndelig() {
        Mockito.`when`(
            medlService!!.opprettPeriodeEndelig(
                ArgumentMatchers.anyString(), ArgumentMatchers.any(
                    Lovvalgsperiode::class.java
                ), ArgumentMatchers.any(
                    KildedokumenttypeMedl::class.java
                )
            )
        )
            .thenReturn(MEDL_PERIODE_ID)
        setupHappyPathBehandling()
    }

    private fun setupHappyPathBehandling() {
        Mockito.`when`(behandlingsresultatService!!.hentBehandlingsresultat(ArgumentMatchers.anyLong()))
            .thenReturn(lagBehandlingsResultat())
        Mockito.`when`(persondataFasade!!.hentFolkeregisterident(ArgumentMatchers.anyString())).thenReturn(FNR)
    }

    private fun lagBehandlingsResultat(): Behandlingsresultat {
        val behandlingsresultat = Behandlingsresultat()
        val behandling = Behandling()
        val fagsak = Fagsak()
        val aktoer = Aktoer()
        aktoer.rolle = Aktoersroller.BRUKER
        aktoer.aktørId = "456"
        fagsak.aktører.add(aktoer)
        behandling.fagsak = fagsak
        behandlingsresultat.behandling = behandling
        return behandlingsresultat
    }

    companion object {
        private const val FNR = "12345678901"
        private const val MEDL_PERIODE_ID = 99L
    }
}
