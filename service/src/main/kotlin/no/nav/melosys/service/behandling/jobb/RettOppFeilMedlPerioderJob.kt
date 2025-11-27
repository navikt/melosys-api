package no.nav.melosys.service.behandling.jobb

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.service.JobMonitor
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.dokument.sed.EessiService
import no.nav.melosys.service.medl.MedlPeriodeService
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val log = KotlinLogging.logger { }

/**
 * Job for å rette opp saker som ble feilaktig oppdatert av AvsluttArt13BehandlingJobb.
 *
 * Problemet: AvsluttArt13BehandlingJobb satte MEDL-perioder til "endelig" og saksstatus til
 * LOVVALG_AVKLART for behandlinger som egentlig skulle vært annullert (pga X008/X006).
 *
 * Denne jobben finner berørte saker og retter opp:
 * - Saksstatus settes tilbake til ANNULLERT
 * - MEDL-periode settes til avvist
 */
@Component
class RettOppFeilMedlPerioderJob(
    private val rettOppFeilMedlPerioderRepository: RettOppFeilMedlPerioderRepository,
    private val eessiService: EessiService,
    private val fagsakService: FagsakService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val medlPeriodeService: MedlPeriodeService
) {
    val sakerFunnet: MutableList<FeilMedlPeriodeRapportEntry> = mutableListOf()

    private val jobMonitor = JobMonitor(
        jobName = "RettOppFeilMedlPerioder",
        stats = JobStatus()
    )

    fun sakerFunnetJsonString(): String = jacksonObjectMapper()
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .registerModule(JavaTimeModule())
        .valueToTree<JsonNode>(sakerFunnet).toPrettyString()

    @Async("taskExecutor")
    @Transactional
    fun kjørAsynkront(dryRun: Boolean, antallFeilFørStopp: Int) {
        kjør(dryRun, antallFeilFørStopp)
    }

    @Synchronized
    @Transactional
    fun kjør(dryRun: Boolean, antallFeilFørStopp: Int = 0) = runAsSystem {
        sakerFunnet.clear()
        jobMonitor.execute(antallFeilFørStopp) {
            log.info { "Starter RettOppFeilMedlPerioderJob (dryRun=$dryRun)" }

            val berørteBehandlinger = rettOppFeilMedlPerioderRepository.finnBehandlingerMedFeilStatus()
            antallFunnet = berørteBehandlinger.size
            log.info { "Fant $antallFunnet potensielt berørte behandlinger" }

            berørteBehandlinger.forEach { behandling ->
                if (jobMonitor.shouldStop) return@execute

                runCatching {
                    behandleEnSak(behandling, dryRun)
                }.onFailure { e ->
                    log.error(e) { "Feil ved behandling av sak ${behandling.fagsak.saksnummer}" }
                    jobMonitor.registerException(e)
                }
            }
        }
    }

    private fun JobStatus.behandleEnSak(behandling: Behandling, dryRun: Boolean) {
        val saksnummer = behandling.fagsak.saksnummer
        val arkivsakID = behandling.fagsak.gsakSaksnummer
        val fagsak = behandling.fagsak

        // Hent SED-info for rapporten
        val sedDokument = behandling.finnSedDokument()
        val sedInfo = if (sedDokument.isPresent) {
            SedInfo(
                rinaSaksnummer = sedDokument.get().rinaSaksnummer,
                rinaDokumentID = sedDokument.get().rinaDokumentID,
                sedType = sedDokument.get().sedType?.name
            )
        } else null

        // Sjekk om behandlingen faktisk ble invalidert (X006/X008)
        if (arkivsakID == null) {
            log.warn { "Sak $saksnummer mangler gsakSaksnummer, hopper over" }
            manglerArkivsakId++
            sakerFunnet.add(
                FeilMedlPeriodeRapportEntry(
                    saksnummer = saksnummer,
                    gsakSaksnummer = null,
                    saksstatus = fagsak.status.name,
                    sakstype = fagsak.type.name,
                    behandlingId = behandling.id,
                    behandlingType = behandling.type.name,
                    behandlingStatus = behandling.status.name,
                    registrertDato = behandling.registrertDato,
                    endretDato = behandling.endretDato,
                    sedInfo = sedInfo,
                    medlPerioder = emptyList(),
                    utfall = RapportUtfall.MANGLER_ARKIVSAK_ID,
                    feilmelding = null,
                    rettetOpp = false
                )
            )
            return
        }

        val erInvalidert = erSedInvalidertIEessi(behandling, arkivsakID)

        if (!erInvalidert) {
            log.info { "Behandling ${behandling.id} (sak $saksnummer) er ikke invalidert i EESSI, hopper over" }
            ikkeInvalidertIEessi++
            sakerFunnet.add(
                FeilMedlPeriodeRapportEntry(
                    saksnummer = saksnummer,
                    gsakSaksnummer = arkivsakID,
                    saksstatus = fagsak.status.name,
                    sakstype = fagsak.type.name,
                    behandlingId = behandling.id,
                    behandlingType = behandling.type.name,
                    behandlingStatus = behandling.status.name,
                    registrertDato = behandling.registrertDato,
                    endretDato = behandling.endretDato,
                    sedInfo = sedInfo,
                    medlPerioder = emptyList(),
                    utfall = RapportUtfall.IKKE_INVALIDERT_I_EESSI,
                    feilmelding = null,
                    rettetOpp = false
                )
            )
            return
        }

        log.info { "Behandling ${behandling.id} (sak $saksnummer) er invalidert - ${if (dryRun) "ville rettet opp" else "retter opp"}" }
        skalRettesOpp++

        // Hent MEDL-periode info
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)
        val medlPerioder = behandlingsresultat.lovvalgsperioder
            .filter { it.medlPeriodeID != null }
            .map { periode ->
                MedlPeriodeInfo(
                    medlPeriodeId = periode.medlPeriodeID,
                    fom = periode.fom,
                    tom = periode.tom
                )
            }

        if (!dryRun) {
            rettOppSak(behandling)
            rettetOpp++
        }

        sakerFunnet.add(
            FeilMedlPeriodeRapportEntry(
                saksnummer = saksnummer,
                gsakSaksnummer = arkivsakID,
                saksstatus = fagsak.status.name,
                sakstype = fagsak.type.name,
                behandlingId = behandling.id,
                behandlingType = behandling.type.name,
                behandlingStatus = behandling.status.name,
                registrertDato = behandling.registrertDato,
                endretDato = behandling.endretDato,
                sedInfo = sedInfo,
                medlPerioder = medlPerioder,
                utfall = RapportUtfall.SKAL_RETTES_OPP,
                feilmelding = null,
                rettetOpp = !dryRun
            )
        )
    }

    private fun erSedInvalidertIEessi(behandling: Behandling, arkivsakID: Long): Boolean {
        val sedDokument = behandling.finnSedDokument()
        if (sedDokument.isEmpty) {
            log.debug { "Behandling ${behandling.id} har ingen SED-dokument" }
            return false
        }

        val rinaSaksnummer = sedDokument.get().rinaSaksnummer
        val rinaDokumentID = sedDokument.get().rinaDokumentID

        return try {
            eessiService.hentTilknyttedeBucer(arkivsakID, emptyList())
                .filter { it.id == rinaSaksnummer }
                .flatMap { it.seder }
                .filter { it.sedId == rinaDokumentID }
                .any { it.erAvbrutt() }
        } catch (e: Exception) {
            log.warn(e) { "Kunne ikke hente BUC-info for arkivsak $arkivsakID" }
            false
        }
    }

    private fun rettOppSak(behandling: Behandling) {
        val fagsak = behandling.fagsak
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)

        // Sett saksstatus tilbake til ANNULLERT
        fagsakService.oppdaterStatus(fagsak, Saksstatuser.ANNULLERT)
        log.info { "Satt saksstatus til ANNULLERT for sak ${fagsak.saksnummer}" }

        // Avvis MEDL-periode hvis den finnes
        behandlingsresultat.lovvalgsperioder.forEach { periode ->
            if (periode.medlPeriodeID != null) {
                try {
                    medlPeriodeService.avvisPeriode(periode.medlPeriodeID)
                    log.info { "Avvist MEDL-periode ${periode.medlPeriodeID} for sak ${fagsak.saksnummer}" }
                } catch (e: Exception) {
                    log.error(e) { "Kunne ikke avvise MEDL-periode ${periode.medlPeriodeID}" }
                    throw e
                }
            }
        }
    }

    private fun <T> runAsSystem(block: () -> T): T {
        val processId = UUID.randomUUID()
        ThreadLocalAccessInfo.beforeExecuteProcess(processId, "RettOppFeilMedlPerioderJob")
        return try {
            block()
        } finally {
            ThreadLocalAccessInfo.afterExecuteProcess(processId)
        }
    }

    fun status() = jobMonitor.status()

    fun stopp() = jobMonitor.stop()

    class JobStatus(
        @Volatile var antallFunnet: Int = 0,
        @Volatile var manglerArkivsakId: Int = 0,
        @Volatile var ikkeInvalidertIEessi: Int = 0,
        @Volatile var skalRettesOpp: Int = 0,
        @Volatile var rettetOpp: Int = 0
    ) : JobMonitor.Stats {
        override fun reset() {
            antallFunnet = 0
            manglerArkivsakId = 0
            ikkeInvalidertIEessi = 0
            skalRettesOpp = 0
            rettetOpp = 0
        }

        override fun asMap(): Map<String, Any?> = mapOf(
            "antallFunnet" to antallFunnet,
            "manglerArkivsakId" to manglerArkivsakId,
            "ikkeInvalidertIEessi" to ikkeInvalidertIEessi,
            "skalRettesOpp" to skalRettesOpp,
            "rettetOpp" to rettetOpp
        )
    }

    data class FeilMedlPeriodeRapportEntry(
        val saksnummer: String,
        val gsakSaksnummer: Long?,
        val saksstatus: String,
        val sakstype: String,
        val behandlingId: Long?,
        val behandlingType: String,
        val behandlingStatus: String,
        val registrertDato: Instant?,
        val endretDato: Instant?,
        val sedInfo: SedInfo?,
        val medlPerioder: List<MedlPeriodeInfo>,
        val utfall: RapportUtfall,
        val feilmelding: String?,
        val rettetOpp: Boolean
    )

    data class SedInfo(
        val rinaSaksnummer: String?,
        val rinaDokumentID: String?,
        val sedType: String?
    )

    data class MedlPeriodeInfo(
        val medlPeriodeId: Long?,
        val fom: LocalDate?,
        val tom: LocalDate?
    )

    enum class RapportUtfall {
        MANGLER_ARKIVSAK_ID,
        IKKE_INVALIDERT_I_EESSI,
        SKAL_RETTES_OPP
    }
}
