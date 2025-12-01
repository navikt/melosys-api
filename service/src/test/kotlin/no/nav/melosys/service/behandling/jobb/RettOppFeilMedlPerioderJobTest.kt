package no.nav.melosys.service.behandling.jobb

import io.kotest.matchers.shouldBe
import io.mockk.*
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.dokument.sed.SedDokument
import no.nav.melosys.domain.eessi.BucInformasjon
import no.nav.melosys.domain.eessi.SedInformasjon
import no.nav.melosys.domain.eessi.SedType
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.medl.MedlPeriodeService
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.sak.FagsakService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.*

class RettOppFeilMedlPerioderJobTest {

    private val repository = mockk<RettOppFeilMedlPerioderRepository>()
    private val eessiService = mockk<EessiService>()
    private val fagsakService = mockk<FagsakService>()
    private val behandlingsresultatService = mockk<BehandlingsresultatService>()
    private val medlPeriodeService = mockk<MedlPeriodeService>()
    private val persondataFasade = mockk<PersondataFasade>()

    private lateinit var job: RettOppFeilMedlPerioderJob

    @BeforeEach
    fun setUp() {
        job = RettOppFeilMedlPerioderJob(
            repository,
            eessiService,
            fagsakService,
            behandlingsresultatService,
            medlPeriodeService,
            persondataFasade
        )
    }

    @Test
    fun `skal ikke endre noe når dryRun er true`() {
        // Arrange
        val fagsak = mockk<Fagsak> {
            every { saksnummer } returns "MEL-123"
            every { gsakSaksnummer } returns 456L
            every { status } returns Saksstatuser.LOVVALG_AVKLART
            every { type } returns Sakstyper.EU_EOS
            every { finnBrukersAktørID() } returns "AKTØR-123"
        }
        val sedDokument = mockk<SedDokument> {
            every { rinaSaksnummer } returns "RINA-789"
            every { rinaDokumentID } returns "DOC-001"
            every { sedType } returns SedType.A001
        }
        val lovvalgsperiode = mockk<Lovvalgsperiode> {
            every { medlPeriodeID } returns 999L
            every { fom } returns LocalDate.of(2024, 1, 1)
            every { tom } returns LocalDate.of(2024, 12, 31)
            every { lovvalgsland } returns null
            every { bestemmelse } returns null
        }
        val behandlingsresultat = mockk<Behandlingsresultat> {
            every { lovvalgsperioder } returns mutableSetOf(lovvalgsperiode)
        }
        val behandling = mockk<Behandling> {
            every { id } returns 1L
            every { this@mockk.fagsak } returns fagsak
            every { finnSedDokument() } returns Optional.of(sedDokument)
            every { type } returns Behandlingstyper.FØRSTEGANG
            every { status } returns Behandlingsstatus.AVSLUTTET
            every { registrertDato } returns Instant.now()
            every { endretDato } returns Instant.now()
        }
        val sedInfo = mockk<SedInformasjon> {
            every { sedId } returns "DOC-001"
            every { sedType } returns "A001"
            every { status } returns "SENT"
            every { opprettetDato } returns LocalDate.of(2024, 1, 1)
            every { erAvbrutt() } returns true
        }
        val bucInfo = mockk<BucInformasjon> {
            every { id } returns "RINA-789"
            every { bucType } returns "LA_BUC_01"
            every { seder } returns listOf(sedInfo)
        }

        every { repository.finnBehandlingIderMedFeilStatus(any(), any()) } returns listOf(1L)
        every { repository.findById(1L) } returns Optional.of(behandling)
        every { repository.finnBehandlingIderMedPotensielleNyVurderingFeil(any(), any()) } returns emptyList()
        every { eessiService.hentTilknyttedeBucer(456L, emptyList()) } returns listOf(bucInfo)
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { persondataFasade.hentFolkeregisterident("AKTØR-123") } returns "12345678901"
        every { medlPeriodeService.hentPeriodeListe(any(), any(), any()) } returns mockk {
            every { dokument } returns null
        }

        // Act
        job.kjør(dryRun = true)

        // Assert
        val status = job.status()
        status["scenario1HentetDenneBatch"] shouldBe 1
        status["skalRettesOpp"] shouldBe 1
        status["rettetOpp"] shouldBe 0 // Dry run, så ingen faktiske endringer

        verify(exactly = 0) { fagsakService.oppdaterStatus(any(), any()) }
        verify(exactly = 0) { medlPeriodeService.avvisPeriode(any()) }
    }

    @Test
    fun `skal rette opp sak når dryRun er false og SED er invalidert`() {
        // Arrange
        val fagsak = mockk<Fagsak> {
            every { saksnummer } returns "MEL-123"
            every { gsakSaksnummer } returns 456L
            every { status } returns Saksstatuser.LOVVALG_AVKLART
            every { type } returns Sakstyper.EU_EOS
            every { finnBrukersAktørID() } returns "AKTØR-123"
        }
        val sedDokument = mockk<SedDokument> {
            every { rinaSaksnummer } returns "RINA-789"
            every { rinaDokumentID } returns "DOC-001"
            every { sedType } returns SedType.A001
        }
        val lovvalgsperiode = mockk<Lovvalgsperiode> {
            every { medlPeriodeID } returns 999L
            every { fom } returns LocalDate.of(2024, 1, 1)
            every { tom } returns LocalDate.of(2024, 12, 31)
            every { lovvalgsland } returns null
            every { bestemmelse } returns null
        }
        val behandlingsresultat = mockk<Behandlingsresultat> {
            every { lovvalgsperioder } returns mutableSetOf(lovvalgsperiode)
        }
        val behandling = mockk<Behandling> {
            every { id } returns 1L
            every { this@mockk.fagsak } returns fagsak
            every { finnSedDokument() } returns Optional.of(sedDokument)
            every { type } returns Behandlingstyper.FØRSTEGANG
            every { status } returns Behandlingsstatus.AVSLUTTET
            every { registrertDato } returns Instant.now()
            every { endretDato } returns Instant.now()
        }
        val sedInfo = mockk<SedInformasjon> {
            every { sedId } returns "DOC-001"
            every { sedType } returns "A001"
            every { status } returns "SENT"
            every { opprettetDato } returns LocalDate.of(2024, 1, 1)
            every { erAvbrutt() } returns true
        }
        val bucInfo = mockk<BucInformasjon> {
            every { id } returns "RINA-789"
            every { bucType } returns "LA_BUC_01"
            every { seder } returns listOf(sedInfo)
        }

        every { repository.finnBehandlingIderMedFeilStatus(any(), any()) } returns listOf(1L)
        every { repository.findById(1L) } returns Optional.of(behandling)
        every { repository.finnBehandlingIderMedPotensielleNyVurderingFeil(any(), any()) } returns emptyList()
        every { eessiService.hentTilknyttedeBucer(456L, emptyList()) } returns listOf(bucInfo)
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { fagsakService.oppdaterStatus(fagsak, Saksstatuser.ANNULLERT) } just runs
        every { medlPeriodeService.avvisPeriode(999L) } just runs
        every { persondataFasade.hentFolkeregisterident("AKTØR-123") } returns "12345678901"
        every { medlPeriodeService.hentPeriodeListe(any(), any(), any()) } returns mockk {
            every { dokument } returns null
        }

        // Act
        job.kjør(dryRun = false)

        // Assert
        val status = job.status()
        status["scenario1HentetDenneBatch"] shouldBe 1
        status["skalRettesOpp"] shouldBe 1
        status["rettetOpp"] shouldBe 1

        verify(exactly = 1) { fagsakService.oppdaterStatus(fagsak, Saksstatuser.ANNULLERT) }
        verify(exactly = 1) { medlPeriodeService.avvisPeriode(999L) }
    }

    @Test
    fun `skal hoppe over behandling som ikke er invalidert i EESSI`() {
        // Arrange
        val fagsak = mockk<Fagsak> {
            every { saksnummer } returns "MEL-123"
            every { gsakSaksnummer } returns 456L
            every { status } returns Saksstatuser.LOVVALG_AVKLART
            every { type } returns Sakstyper.EU_EOS
            every { finnBrukersAktørID() } returns "AKTØR-123"
        }
        val sedDokument = mockk<SedDokument> {
            every { rinaSaksnummer } returns "RINA-789"
            every { rinaDokumentID } returns "DOC-001"
            every { sedType } returns SedType.A001
        }
        val lovvalgsperiode = mockk<Lovvalgsperiode> {
            every { medlPeriodeID } returns 999L
            every { fom } returns LocalDate.of(2024, 1, 1)
            every { tom } returns LocalDate.of(2024, 12, 31)
            every { lovvalgsland } returns null
            every { bestemmelse } returns null
        }
        val behandlingsresultat = mockk<Behandlingsresultat> {
            every { lovvalgsperioder } returns mutableSetOf(lovvalgsperiode)
        }
        val behandling = mockk<Behandling> {
            every { id } returns 1L
            every { this@mockk.fagsak } returns fagsak
            every { finnSedDokument() } returns Optional.of(sedDokument)
            every { type } returns Behandlingstyper.FØRSTEGANG
            every { status } returns Behandlingsstatus.AVSLUTTET
            every { registrertDato } returns Instant.now()
            every { endretDato } returns Instant.now()
        }
        val sedInfo = mockk<SedInformasjon> {
            every { sedId } returns "DOC-001"
            every { sedType } returns "A001"
            every { status } returns "SENT"
            every { opprettetDato } returns LocalDate.of(2024, 1, 1)
            every { erAvbrutt() } returns false // Ikke invalidert
        }
        val bucInfo = mockk<BucInformasjon> {
            every { id } returns "RINA-789"
            every { bucType } returns "LA_BUC_01"
            every { seder } returns listOf(sedInfo)
        }

        every { repository.finnBehandlingIderMedFeilStatus(any(), any()) } returns listOf(1L)
        every { repository.findById(1L) } returns Optional.of(behandling)
        every { repository.finnBehandlingIderMedPotensielleNyVurderingFeil(any(), any()) } returns emptyList()
        every { eessiService.hentTilknyttedeBucer(456L, emptyList()) } returns listOf(bucInfo)
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { persondataFasade.hentFolkeregisterident("AKTØR-123") } returns "12345678901"
        every { medlPeriodeService.hentPeriodeListe(any(), any(), any()) } returns mockk {
            every { dokument } returns null
        }

        // Act
        job.kjør(dryRun = false)

        // Assert
        val status = job.status()
        status["scenario1HentetDenneBatch"] shouldBe 1
        status["ikkeInvalidertIEessi"] shouldBe 1
        status["skalRettesOpp"] shouldBe 0

        verify(exactly = 0) { fagsakService.oppdaterStatus(any(), any()) }
    }

    // ===== Tests for kjørEnBatch =====

    @Test
    fun `kjørEnBatch skal returnere BatchResultat med korrekt struktur`() {
        // Arrange
        val fagsak = mockk<Fagsak> {
            every { saksnummer } returns "MEL-123"
            every { gsakSaksnummer } returns 456L
            every { status } returns Saksstatuser.LOVVALG_AVKLART
            every { type } returns Sakstyper.EU_EOS
            every { finnBrukersAktørID() } returns "AKTØR-123"
        }
        val sedDokument = mockk<SedDokument> {
            every { rinaSaksnummer } returns "RINA-789"
            every { rinaDokumentID } returns "DOC-001"
            every { sedType } returns SedType.A001
        }
        val lovvalgsperiode = mockk<Lovvalgsperiode> {
            every { medlPeriodeID } returns 999L
            every { fom } returns LocalDate.of(2024, 1, 1)
            every { tom } returns LocalDate.of(2024, 12, 31)
            every { lovvalgsland } returns null
            every { bestemmelse } returns null
        }
        val behandlingsresultat = mockk<Behandlingsresultat> {
            every { lovvalgsperioder } returns mutableSetOf(lovvalgsperiode)
        }
        val behandling = mockk<Behandling> {
            every { id } returns 1L
            every { this@mockk.fagsak } returns fagsak
            every { finnSedDokument() } returns Optional.of(sedDokument)
            every { type } returns Behandlingstyper.FØRSTEGANG
            every { status } returns Behandlingsstatus.AVSLUTTET
            every { registrertDato } returns Instant.now()
            every { endretDato } returns Instant.now()
        }
        val sedInfo = mockk<SedInformasjon> {
            every { sedId } returns "DOC-001"
            every { sedType } returns "A001"
            every { status } returns "SENT"
            every { opprettetDato } returns LocalDate.of(2024, 1, 1)
            every { erAvbrutt() } returns true
        }
        val bucInfo = mockk<BucInformasjon> {
            every { id } returns "RINA-789"
            every { bucType } returns "LA_BUC_01"
            every { seder } returns listOf(sedInfo)
        }

        every { repository.finnBehandlingIderMedFeilStatus(0L, any()) } returns listOf(1L)
        every { repository.findById(1L) } returns Optional.of(behandling)
        every { repository.finnBehandlingIderMedPotensielleNyVurderingFeil(0L, any()) } returns emptyList()
        every { eessiService.hentTilknyttedeBucer(456L, emptyList()) } returns listOf(bucInfo)
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { persondataFasade.hentFolkeregisterident("AKTØR-123") } returns "12345678901"
        every { medlPeriodeService.hentPeriodeListe(any(), any(), any()) } returns mockk {
            every { dokument } returns null
        }

        // Act
        val resultat = job.kjørEnBatch(dryRun = true, batchStørrelse = 10, startFraBehandlingId = 0)

        // Assert
        resultat.batchInfo.dryRun shouldBe true
        resultat.batchInfo.batchStørrelse shouldBe 10
        resultat.batchInfo.startFraBehandlingId shouldBe 0
        resultat.scenario1.hentetDenneBatch shouldBe 1
        resultat.scenario1.sisteBehandledeId shouldBe 1L
        resultat.scenario2.hentetDenneBatch shouldBe 0
        resultat.nextStartFraBehandlingId shouldBe 1L
        resultat.hasMoreItems shouldBe false // Bare 1 item, mindre enn batchSize 10
        resultat.rapport.size shouldBe 1
        resultat.rapport[0].saksnummer shouldBe "MEL-123"
        resultat.rapport[0].utfall shouldBe RettOppFeilMedlPerioderJob.RapportUtfall.SKAL_RETTES_OPP
    }

    @Test
    fun `kjørEnBatch skal sette hasMoreItems true når batch er full`() {
        // Arrange - lager 10 behandlinger for å fylle batchen
        val behandlinger = (1L..10L).map { id ->
            val fagsak = mockk<Fagsak> {
                every { saksnummer } returns "MEL-$id"
                every { gsakSaksnummer } returns 100L + id
                every { status } returns Saksstatuser.LOVVALG_AVKLART
                every { type } returns Sakstyper.EU_EOS
                every { finnBrukersAktørID() } returns "AKTØR-$id"
            }
            val sedDokument = mockk<SedDokument> {
                every { rinaSaksnummer } returns "RINA-$id"
                every { rinaDokumentID } returns "DOC-$id"
                every { sedType } returns SedType.A001
            }
            val behandling = mockk<Behandling> {
                every { this@mockk.id } returns id
                every { this@mockk.fagsak } returns fagsak
                every { finnSedDokument() } returns Optional.of(sedDokument)
                every { type } returns Behandlingstyper.FØRSTEGANG
                every { status } returns Behandlingsstatus.AVSLUTTET
                every { registrertDato } returns Instant.now()
                every { endretDato } returns Instant.now()
            }
            val sedInfo = mockk<SedInformasjon> {
                every { sedId } returns "DOC-$id"
                every { sedType } returns "A001"
                every { status } returns "SENT"
                every { opprettetDato } returns LocalDate.of(2024, 1, 1)
                every { erAvbrutt() } returns false // Ikke invalidert - vil hoppes over
            }
            val bucInfo = mockk<BucInformasjon> {
                every { this@mockk.id } returns "RINA-$id"
                every { bucType } returns "LA_BUC_01"
                every { seder } returns listOf(sedInfo)
            }
            val lovvalgsperiode = mockk<Lovvalgsperiode> {
                every { medlPeriodeID } returns id * 100
                every { fom } returns LocalDate.of(2024, 1, 1)
                every { tom } returns LocalDate.of(2024, 12, 31)
                every { lovvalgsland } returns null
                every { bestemmelse } returns null
            }
            val behandlingsresultat = mockk<Behandlingsresultat> {
                every { lovvalgsperioder } returns mutableSetOf(lovvalgsperiode)
            }

            every { repository.findById(id) } returns Optional.of(behandling)
            every { eessiService.hentTilknyttedeBucer(100L + id, emptyList()) } returns listOf(bucInfo)
            every { behandlingsresultatService.hentBehandlingsresultat(id) } returns behandlingsresultat
            every { persondataFasade.hentFolkeregisterident("AKTØR-$id") } returns "1234567890$id"
            every { medlPeriodeService.hentPeriodeListe(any(), any(), any()) } returns mockk {
                every { dokument } returns null
            }

            id
        }

        every { repository.finnBehandlingIderMedFeilStatus(0L, any()) } returns behandlinger
        every { repository.finnBehandlingIderMedPotensielleNyVurderingFeil(0L, any()) } returns emptyList()

        // Act
        val resultat = job.kjørEnBatch(dryRun = true, batchStørrelse = 10, startFraBehandlingId = 0)

        // Assert
        resultat.scenario1.hentetDenneBatch shouldBe 10
        resultat.hasMoreItems shouldBe true // Batch er full, så det kan være mer data
        resultat.nextStartFraBehandlingId shouldBe 10L
    }

    @Test
    fun `kjørEnBatch skal ikke gjøre endringer når dryRun er true`() {
        // Arrange
        val fagsak = mockk<Fagsak> {
            every { saksnummer } returns "MEL-123"
            every { gsakSaksnummer } returns 456L
            every { status } returns Saksstatuser.LOVVALG_AVKLART
            every { type } returns Sakstyper.EU_EOS
            every { finnBrukersAktørID() } returns "AKTØR-123"
        }
        val sedDokument = mockk<SedDokument> {
            every { rinaSaksnummer } returns "RINA-789"
            every { rinaDokumentID } returns "DOC-001"
            every { sedType } returns SedType.A001
        }
        val lovvalgsperiode = mockk<Lovvalgsperiode> {
            every { medlPeriodeID } returns 999L
            every { fom } returns LocalDate.of(2024, 1, 1)
            every { tom } returns LocalDate.of(2024, 12, 31)
            every { lovvalgsland } returns null
            every { bestemmelse } returns null
        }
        val behandlingsresultat = mockk<Behandlingsresultat> {
            every { lovvalgsperioder } returns mutableSetOf(lovvalgsperiode)
        }
        val behandling = mockk<Behandling> {
            every { id } returns 1L
            every { this@mockk.fagsak } returns fagsak
            every { finnSedDokument() } returns Optional.of(sedDokument)
            every { type } returns Behandlingstyper.FØRSTEGANG
            every { status } returns Behandlingsstatus.AVSLUTTET
            every { registrertDato } returns Instant.now()
            every { endretDato } returns Instant.now()
        }
        val sedInfo = mockk<SedInformasjon> {
            every { sedId } returns "DOC-001"
            every { sedType } returns "A001"
            every { status } returns "SENT"
            every { opprettetDato } returns LocalDate.of(2024, 1, 1)
            every { erAvbrutt() } returns true
        }
        val bucInfo = mockk<BucInformasjon> {
            every { id } returns "RINA-789"
            every { bucType } returns "LA_BUC_01"
            every { seder } returns listOf(sedInfo)
        }

        every { repository.finnBehandlingIderMedFeilStatus(0L, any()) } returns listOf(1L)
        every { repository.findById(1L) } returns Optional.of(behandling)
        every { repository.finnBehandlingIderMedPotensielleNyVurderingFeil(0L, any()) } returns emptyList()
        every { eessiService.hentTilknyttedeBucer(456L, emptyList()) } returns listOf(bucInfo)
        every { behandlingsresultatService.hentBehandlingsresultat(1L) } returns behandlingsresultat
        every { persondataFasade.hentFolkeregisterident("AKTØR-123") } returns "12345678901"
        every { medlPeriodeService.hentPeriodeListe(any(), any(), any()) } returns mockk {
            every { dokument } returns null
        }

        // Act
        val resultat = job.kjørEnBatch(dryRun = true, batchStørrelse = 10, startFraBehandlingId = 0)

        // Assert
        resultat.rapport[0].rettetOpp shouldBe false
        verify(exactly = 0) { fagsakService.oppdaterStatus(any(), any()) }
        verify(exactly = 0) { medlPeriodeService.avvisPeriode(any()) }
    }

    @Test
    fun `kjørEnBatch skal returnere tom rapport når ingen behandlinger matcher`() {
        // Arrange
        every { repository.finnBehandlingIderMedFeilStatus(0L, any()) } returns emptyList()
        every { repository.finnBehandlingIderMedPotensielleNyVurderingFeil(0L, any()) } returns emptyList()

        // Act
        val resultat = job.kjørEnBatch(dryRun = true, batchStørrelse = 10, startFraBehandlingId = 0)

        // Assert
        resultat.scenario1.hentetDenneBatch shouldBe 0
        resultat.scenario2.hentetDenneBatch shouldBe 0
        resultat.hasMoreItems shouldBe false
        resultat.rapport shouldBe emptyList()
    }
}
