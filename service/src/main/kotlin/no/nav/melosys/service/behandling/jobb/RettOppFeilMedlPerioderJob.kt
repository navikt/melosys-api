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
import no.nav.melosys.service.persondata.PersondataFasade
import no.nav.melosys.service.sak.FagsakService
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.data.domain.PageRequest
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
 * Scenario 1 (X008/X006):
 * - Fagsak har status LOVVALG_AVKLART
 * - Behandlingsresultat er HENLEGGELSE (indikerer at saken ble annullert via X008/X006)
 * - Det finnes INGEN NY_VURDERING på fagsaken
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
    private val medlPeriodeService: MedlPeriodeService,
    private val persondataFasade: PersondataFasade
) {
    private val sakerFunnet: MutableList<FeilMedlPeriodeRapportEntry> = mutableListOf()

    private val jobMonitor = JobMonitor(
        jobName = "RettOppFeilMedlPerioder",
        stats = JobStatus()
    )

    @Synchronized
    fun sakerFunnetJsonString(): String {
        val gruppertOgSortert = sakerFunnet
            .sortedWith(compareBy({ it.saksnummer }, { it.registrertDato }))
            .groupBy { it.saksnummer }

        return objectMapper.valueToTree<JsonNode>(gruppertOgSortert).toPrettyString()
    }

    @Synchronized
    fun rapportStørrelse(): Int = sakerFunnet.size

    @Synchronized
    fun tømRapport() {
        sakerFunnet.clear()
    }

    /**
     * Teller totalt antall behandlinger som matcher kriteriene.
     */
    fun totaltAntall(): Long = rettOppFeilMedlPerioderRepository.countBehandlingerMedFeilStatus()

    @Async("taskExecutor")
    @Transactional
    fun kjørAsynkront(dryRun: Boolean, antallFeilFørStopp: Int, batchStørrelse: Int = 1000, startFraBehandlingId: Long = 0) {
        kjør(dryRun, antallFeilFørStopp, batchStørrelse, startFraBehandlingId)
    }

    @Synchronized
    @Transactional
    fun kjør(dryRun: Boolean, antallFeilFørStopp: Int = 0, batchStørrelse: Int = 1000, startFraBehandlingId: Long = 0) = runAsSystem {
        sakerFunnet.clear()
        jobMonitor.execute(antallFeilFørStopp) {
            log.info { "Starter RettOppFeilMedlPerioderJob (dryRun=$dryRun, batchStørrelse=$batchStørrelse, startFraBehandlingId=$startFraBehandlingId)" }

            val pageable = PageRequest.of(0, batchStørrelse)
            val berørteBehandlingIder = rettOppFeilMedlPerioderRepository.finnBehandlingIderMedFeilStatus(startFraBehandlingId, pageable)
            hentetDenneBatch = berørteBehandlingIder.size
            log.info { "Hentet $hentetDenneBatch behandlinger (startFraBehandlingId=$startFraBehandlingId, batchStørrelse=$batchStørrelse)" }

            berørteBehandlingIder.forEach { behandlingId ->
                if (jobMonitor.shouldStop) return@execute

                runCatching {
                    val behandling = rettOppFeilMedlPerioderRepository.findById(behandlingId).orElse(null)
                    if (behandling != null) {
                        behandleEnSak(behandling, dryRun)
                    } else {
                        log.warn { "Kunne ikke finne behandling med id $behandlingId" }
                    }
                }.onFailure { e ->
                    log.error(e) { "Feil ved behandling av behandlingId $behandlingId" }
                    jobMonitor.registerException(e)
                }
                totaltProsessert++
                sisteBehandledeId = behandlingId
            }
        }
    }

    private fun JobStatus.behandleEnSak(behandling: Behandling, dryRun: Boolean) {
        val saksnummer = behandling.fagsak.saksnummer
        val arkivsakID = behandling.fagsak.gsakSaksnummer
        val fagsak = behandling.fagsak

        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)

        val sedDokument = behandling.finnSedDokument()
        val sedInfoFraDb = if (sedDokument.isPresent) {
            SedInfo(
                rinaSaksnummer = sedDokument.get().rinaSaksnummer,
                rinaDokumentID = sedDokument.get().rinaDokumentID,
                sedType = sedDokument.get().sedType?.name
            )
        } else null

        val alleSederFraEessi = hentAlleSederFraEessi(arkivsakID, sedDokument.orElse(null)?.rinaSaksnummer)
        val medlSammenligningInfo = hentOgSammenlignMedlPerioder(behandling, behandlingsresultat)

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
                    sedInfo = sedInfoFraDb,
                    alleSederFraEessi = emptyList(),
                    medlPerioder = emptyList(),
                    medlSammenligningInfo = null,
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
                    sedInfo = sedInfoFraDb,
                    alleSederFraEessi = alleSederFraEessi,
                    medlPerioder = emptyList(),
                    medlSammenligningInfo = medlSammenligningInfo,
                    utfall = RapportUtfall.IKKE_INVALIDERT_I_EESSI,
                    feilmelding = null,
                    rettetOpp = false
                )
            )
            return
        }

        log.info { "Behandling ${behandling.id} (sak $saksnummer) er invalidert - ${if (dryRun) "ville rettet opp" else "retter opp"}" }
        skalRettesOpp++

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
            rettOppSak(behandling, behandlingsresultat)
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
                sedInfo = sedInfoFraDb,
                alleSederFraEessi = alleSederFraEessi,
                medlPerioder = medlPerioder,
                medlSammenligningInfo = medlSammenligningInfo,
                utfall = RapportUtfall.SKAL_RETTES_OPP,
                feilmelding = null,
                rettetOpp = !dryRun
            )
        )
    }

    /**
     * Henter alle SEDer fra EESSI for en gitt arkivsak og BUC.
     */
    private fun hentAlleSederFraEessi(arkivsakID: Long?, rinaSaksnummer: String?): List<EessiSedInfo> {
        if (arkivsakID == null) return emptyList()

        return try {
            eessiService.hentTilknyttedeBucer(arkivsakID, emptyList())
                .filter { rinaSaksnummer == null || it.id == rinaSaksnummer }
                .flatMap { buc ->
                    buc.seder.map { sed ->
                        EessiSedInfo(
                            bucId = buc.id,
                            bucType = buc.bucType,
                            sedId = sed.sedId,
                            sedType = sed.sedType,
                            status = sed.status,
                            opprettetDato = sed.opprettetDato,
                            erAvbrutt = sed.erAvbrutt()
                        )
                    }
                }
        } catch (e: Exception) {
            log.warn(e) { "Kunne ikke hente alle SEDer fra EESSI for arkivsak $arkivsakID" }
            jobMonitor.registerException(e)
            emptyList()
        }
    }

    /**
     * Henter MEDL-perioder fra MEDL-registeret og sammenligner med lovvalgsperiode fra behandlingen.
     */
    private fun hentOgSammenlignMedlPerioder(
        behandling: Behandling,
        behandlingsresultat: no.nav.melosys.domain.Behandlingsresultat
    ): MedlSammenligningInfo? {
        val fagsak = behandling.fagsak
        val brukerAktørId = fagsak.finnBrukersAktørID() ?: return null

        val lovvalgsperiode = behandlingsresultat.lovvalgsperioder.firstOrNull() ?: return null

        val fnr = try {
            persondataFasade.hentFolkeregisterident(brukerAktørId)
        } catch (e: Exception) {
            log.warn(e) { "Kunne ikke hente fnr for aktørId $brukerAktørId" }
            jobMonitor.registerException(e)
            return null
        }

        val medlPerioderFraRegister = try {
            val saksopplysning = medlPeriodeService.hentPeriodeListe(
                fnr,
                lovvalgsperiode.fom?.minusYears(1),
                lovvalgsperiode.tom?.plusYears(1)
            )
            val medlemskapDokument = saksopplysning.dokument as? no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument
            medlemskapDokument?.medlemsperiode?.map { periode ->
                MedlRegisterPeriode(
                    unntakId = periode.id,
                    fom = periode.periode?.fom,
                    tom = periode.periode?.tom,
                    status = periode.status,
                    lovvalg = periode.lovvalg,
                    lovvalgsland = periode.land,
                    dekning = periode.trygdedekning
                )
            } ?: emptyList()
        } catch (e: Exception) {
            log.warn(e) { "Kunne ikke hente MEDL-perioder fra register for fnr" }
            jobMonitor.registerException(e)
            emptyList()
        }

        val matchendeMedlPeriode = lovvalgsperiode.medlPeriodeID?.let { medlId ->
            medlPerioderFraRegister.find { it.unntakId == medlId }
        }

        return MedlSammenligningInfo(
            lovvalgsperiodeFraBehandling = LovvalgsperiodeInfo(
                fom = lovvalgsperiode.fom,
                tom = lovvalgsperiode.tom,
                medlPeriodeId = lovvalgsperiode.medlPeriodeID,
                lovvalgsland = lovvalgsperiode.lovvalgsland?.name,
                bestemmelse = lovvalgsperiode.bestemmelse?.kode
            ),
            matchendeMedlPeriodeFraRegister = matchendeMedlPeriode
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

    private fun rettOppSak(
        behandling: Behandling,
        behandlingsresultat: no.nav.melosys.domain.Behandlingsresultat
    ) {
        val fagsak = behandling.fagsak

        fagsakService.oppdaterStatus(fagsak, Saksstatuser.ANNULLERT)
        log.info { "Satt saksstatus til ANNULLERT for sak ${fagsak.saksnummer}" }

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
        @Volatile var hentetDenneBatch: Int = 0,
        @Volatile var totaltProsessert: Int = 0,
        @Volatile var manglerArkivsakId: Int = 0,
        @Volatile var ikkeInvalidertIEessi: Int = 0,
        @Volatile var skalRettesOpp: Int = 0,
        @Volatile var rettetOpp: Int = 0,
        @Volatile var sisteBehandledeId: Long? = null
    ) : JobMonitor.Stats {
        override fun reset() {
            hentetDenneBatch = 0
            totaltProsessert = 0
            manglerArkivsakId = 0
            ikkeInvalidertIEessi = 0
            skalRettesOpp = 0
            rettetOpp = 0
            sisteBehandledeId = null
        }

        override fun asMap(): Map<String, Any?> = mapOf(
            "hentetDenneBatch" to hentetDenneBatch,
            "totaltProsessert" to totaltProsessert,
            "manglerArkivsakId" to manglerArkivsakId,
            "ikkeInvalidertIEessi" to ikkeInvalidertIEessi,
            "skalRettesOpp" to skalRettesOpp,
            "rettetOpp" to rettetOpp,
            "sisteBehandledeId" to sisteBehandledeId
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
        val alleSederFraEessi: List<EessiSedInfo>,
        val medlPerioder: List<MedlPeriodeInfo>,
        val medlSammenligningInfo: MedlSammenligningInfo?,
        val utfall: RapportUtfall,
        val feilmelding: String?,
        val rettetOpp: Boolean
    )

    data class SedInfo(
        val rinaSaksnummer: String?,
        val rinaDokumentID: String?,
        val sedType: String?
    )

    data class EessiSedInfo(
        val bucId: String?,
        val bucType: String?,
        val sedId: String?,
        val sedType: String?,
        val status: String?,
        val opprettetDato: LocalDate?,
        val erAvbrutt: Boolean
    )

    data class MedlPeriodeInfo(
        val medlPeriodeId: Long?,
        val fom: LocalDate?,
        val tom: LocalDate?
    )

    data class MedlRegisterPeriode(
        val unntakId: Long?,
        val fom: LocalDate?,
        val tom: LocalDate?,
        val status: String?,
        val lovvalg: String?,
        val lovvalgsland: String?,
        val dekning: String?
    )

    data class LovvalgsperiodeInfo(
        val fom: LocalDate?,
        val tom: LocalDate?,
        val medlPeriodeId: Long?,
        val lovvalgsland: String?,
        val bestemmelse: String?
    )

    data class MedlSammenligningInfo(
        val lovvalgsperiodeFraBehandling: LovvalgsperiodeInfo,
        val matchendeMedlPeriodeFraRegister: MedlRegisterPeriode?
    )

    enum class RapportUtfall {
        MANGLER_ARKIVSAK_ID,
        IKKE_INVALIDERT_I_EESSI,
        SKAL_RETTES_OPP
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .registerModule(JavaTimeModule())
    }
}
