package no.nav.melosys.service.behandling.jobb

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

        every { repository.finnBehandlingerMedFeilStatus() } returns listOf(behandling)
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
        assert(status["antallFunnet"] == 1)
        assert(status["skalRettesOpp"] == 1)
        assert(status["rettetOpp"] == 0) // Dry run, så ingen faktiske endringer

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

        every { repository.finnBehandlingerMedFeilStatus() } returns listOf(behandling)
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
        assert(status["antallFunnet"] == 1)
        assert(status["skalRettesOpp"] == 1)
        assert(status["rettetOpp"] == 1)

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

        every { repository.finnBehandlingerMedFeilStatus() } returns listOf(behandling)
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
        assert(status["antallFunnet"] == 1)
        assert(status["ikkeInvalidertIEessi"] == 1)
        assert(status["skalRettesOpp"] == 0)

        verify(exactly = 0) { fagsakService.oppdaterStatus(any(), any()) }
    }
}
