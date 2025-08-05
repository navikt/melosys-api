package no.nav.melosys.service.behandling.jobb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import no.nav.melosys.domain.*
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.service.LovvalgsperiodeService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.medl.MedlPeriodeService
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class AvsluttArt13BehandlingServiceKtTest {

    @MockK
    private lateinit var behandlingService: BehandlingService

    @MockK
    private lateinit var fagsakService: FagsakService

    @MockK
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @MockK
    private lateinit var medlPeriodeService: MedlPeriodeService

    @MockK
    private lateinit var lovvalgsperiodeService: LovvalgsperiodeService

    private lateinit var avsluttArt13BehandlingService: AvsluttArt13BehandlingService

    private val behandlingID = 11L

    private val behandlingsresultat = Behandlingsresultat()
    private lateinit var behandling: Behandling
    private lateinit var fagsak: Fagsak
    private val lovvalgsperiode = Lovvalgsperiode()
    private val vedtakMetadata = VedtakMetadata()

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        avsluttArt13BehandlingService = AvsluttArt13BehandlingService(
            behandlingService, fagsakService,
            behandlingsresultatService, medlPeriodeService, lovvalgsperiodeService
        )

        fagsak = FagsakTestFactory.lagFagsak()

        behandling = BehandlingTestFactory.builderWithDefaults()
            .medId(behandlingID)
            .medStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING)
            .medTema(Behandlingstema.ARBEID_FLERE_LAND)
            .medFagsak(fagsak)
            .build()

        behandlingsresultat.id = behandlingID
        behandlingsresultat.behandling = behandling
        behandlingsresultat.lovvalgsperioder.add(lovvalgsperiode)
        behandlingsresultat.vedtakMetadata = vedtakMetadata
        lovvalgsperiode.innvilgelsesresultat = InnvilgelsesResultat.INNVILGET
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_2A
        lovvalgsperiode.medlPeriodeID = 123L

        every { behandlingService.hentBehandlingMedSaksopplysninger(behandlingID) } returns behandling
        every { behandlingsresultatService.hentBehandlingsresultat(behandlingID) } returns behandlingsresultat
        every { fagsakService.avsluttFagsakOgBehandling(any(), any(), any()) } returns Unit
        every { medlPeriodeService.oppdaterPeriodeEndelig(any()) } returns Unit
    }

    @Test
    fun avsluttBehandlingArt13_ikkeArt13_kasterException() {
        vedtakMetadata.vedtaksdato = månederOgDagerSiden(2, 1)
        lovvalgsperiode.bestemmelse = Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1

        val exception = shouldThrow<FunksjonellException> {
            avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)
        }
        exception.message shouldContain "Behandling skal ikke avsluttes automatisk da perioden er av bestemmelse"
    }

    @Test
    fun avsluttBehandlingArt13_søknad1MndSidenVedtak_behandlingIkkeAvlsuttet() {
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        vedtakMetadata.vedtaksdato = månederOgDagerSiden(1, 0)

        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)

        verify(exactly = 0) { fagsakService.avsluttFagsakOgBehandling(any(), any(), any()) }
    }

    @Test
    fun avsluttBehandlingArt13_norgeUtpekt2Mnd1DagSidenVedtak_behandlingBlirAvsluttet() {
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        vedtakMetadata.vedtaksdato = månederOgDagerSiden(2, 1)

        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)

        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, behandling, Saksstatuser.LOVVALG_AVKLART) }
        verify { medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode) }
    }

    @Test
    fun avsluttBehandlingArt13_norgeUtpektVedtakIkkeLagret_kasterException() {
        behandling.tema = Behandlingstema.BESLUTNING_LOVVALG_NORGE
        behandlingsresultat.vedtakMetadata = null

        val exception = shouldThrow<FunksjonellException> {
            avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)
        }
        exception.message shouldContain "har ikke et vedtak og status kan da ikke settes til AVSLUTTET"
    }

    @Test
    fun avsluttBehandlingArt13_søknad2Mnd1DagSidenEndretDato_medlOppdatertOgBehandlingBlirAvsluttet() {
        behandlingsresultat.endretDato = månederOgDagerSiden(2, 1)
        vedtakMetadata.vedtaksdato = månederOgDagerSiden(2, 1)

        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)

        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, behandling, Saksstatuser.LOVVALG_AVKLART) }
        verify { medlPeriodeService.oppdaterPeriodeEndelig(lovvalgsperiode) }
    }

    @Test
    fun avsluttBehandlingArt13_søknad3MndSidenEndretDatoUtpekingUtenVedtak_lovvalgsperiodeOpprettetOgBehandlingAvsluttet() {
        val utpekingsperiode = Utpekingsperiode(
            LocalDate.now(), LocalDate.now(), Land_iso2.SE, Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1, null
        )
        utpekingsperiode.medlPeriodeID = 123L

        behandlingsresultat.endretDato = månederOgDagerSiden(3, 0)
        behandlingsresultat.vedtakMetadata = null
        behandlingsresultat.utpekingsperioder.add(utpekingsperiode)

        every { lovvalgsperiodeService.lagreLovvalgsperioder(any(), any()) } answers { secondArg() }

        avsluttArt13BehandlingService.avsluttBehandlingHvisToMndPassert(behandlingID)

        verify { lovvalgsperiodeService.lagreLovvalgsperioder(eq(behandlingID), any()) }
        verify { fagsakService.avsluttFagsakOgBehandling(fagsak, behandling, Saksstatuser.LOVVALG_AVKLART) }
        verify { medlPeriodeService.oppdaterPeriodeEndelig(any()) }
    }

    private fun månederOgDagerSiden(mnd: Long, dager: Long): Instant {
        return LocalDate.now().minusMonths(mnd).minusDays(dager).atStartOfDay(ZoneId.systemDefault()).toInstant()
    }
}
