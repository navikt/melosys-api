package no.nav.melosys.service.behandling.jobb

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
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
        // Grupper etter saksnummer og sorter etter registrertDato
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
     * Teller totalt antall behandlinger for begge scenarioer (for /totalt-antall endpoint).
     */
    fun totaltAntall(): TotaltAntall {
        val scenario1 = rettOppFeilMedlPerioderRepository.countBehandlingerMedFeilStatus()
        val scenario2 = rettOppFeilMedlPerioderRepository.countBehandlingerMedPotensielleNyVurderingFeil()
        val begge = rettOppFeilMedlPerioderRepository.countBehandlingerMedBeggeTyperFeil()
        return TotaltAntall(
            scenario1X008X006 = scenario1,
            scenario2NyVurdering = scenario2,
            beggeTyperFeil = begge,
            totaltUnike = scenario1 + scenario2 - begge
        )
    }

    data class TotaltAntall(
        val scenario1X008X006: Long,
        val scenario2NyVurdering: Long,
        val beggeTyperFeil: Long,
        val totaltUnike: Long
    )

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

            // ===== Scenario 1: X008/X006 invaliderte SEDer =====
            // Hent kun ID-er med ID-basert paginering for å unngå OOM
            val pageable = PageRequest.of(0, batchStørrelse)
            val berørteBehandlingIder = rettOppFeilMedlPerioderRepository.finnBehandlingIderMedFeilStatus(startFraBehandlingId, pageable)
            scenario1HentetDenneBatch = berørteBehandlingIder.size
            log.info { "Scenario 1: Hentet $scenario1HentetDenneBatch behandlinger (startFraBehandlingId=$startFraBehandlingId, batchStørrelse=$batchStørrelse)" }

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
                // Oppdater progress for scenario 1
                scenario1TotaltProsessert++
                sisteBehandledeIdScenario1 = behandlingId
            }

            // ===== Scenario 2: Ny vurdering potensielt overskrevet =====
            // Hent kun ID-er med ID-basert paginering for å unngå OOM
            val nyVurderingBehandlingIder = rettOppFeilMedlPerioderRepository.finnBehandlingIderMedPotensielleNyVurderingFeil(startFraBehandlingId, pageable)
            scenario2HentetDenneBatch = nyVurderingBehandlingIder.size
            log.info { "Scenario 2: Hentet $scenario2HentetDenneBatch behandlinger (startFraBehandlingId=$startFraBehandlingId, batchStørrelse=$batchStørrelse)" }

            nyVurderingBehandlingIder.forEach { behandlingId ->
                if (jobMonitor.shouldStop) return@execute

                runCatching {
                    val behandling = rettOppFeilMedlPerioderRepository.findById(behandlingId).orElse(null)
                    if (behandling != null) {
                        behandleNyVurderingSak(behandling)
                    } else {
                        log.warn { "Kunne ikke finne behandling med id $behandlingId" }
                    }
                }.onFailure { e ->
                    log.error(e) { "Feil ved behandling av ny vurdering behandlingId $behandlingId" }
                    jobMonitor.registerException(e)
                }
                // Oppdater progress for scenario 2
                scenario2TotaltProsessert++
                sisteBehandledeIdScenario2 = behandlingId
            }
        }
    }

    /**
     * Kjører EN batch og returnerer resultatene direkte.
     * Dette er en stateless metode - hvert kall er uavhengig og frigjør minne etter respons.
     * Brukes av Python-script for batch-prosessering uten å akkumulere data i minnet.
     */
    @Transactional
    fun kjørEnBatch(dryRun: Boolean, batchStørrelse: Int, startFraBehandlingId: Long): BatchResultat = runAsSystem {
        log.info { "Kjører én batch (dryRun=$dryRun, batchStørrelse=$batchStørrelse, startFraBehandlingId=$startFraBehandlingId)" }

        // Lokale variabler - frigjøres etter metoden returnerer
        val batchRapport = mutableListOf<FeilMedlPeriodeRapportEntry>()
        var scenario1SisteId: Long? = null
        var scenario2SisteId: Long? = null

        val pageable = PageRequest.of(0, batchStørrelse)

        // ===== Scenario 1: X008/X006 invaliderte SEDer =====
        val scenario1Ider = rettOppFeilMedlPerioderRepository.finnBehandlingIderMedFeilStatus(startFraBehandlingId, pageable)
        log.info { "Scenario 1: Hentet ${scenario1Ider.size} behandlinger" }

        scenario1Ider.forEach { behandlingId ->
            runCatching {
                val behandling = rettOppFeilMedlPerioderRepository.findById(behandlingId).orElse(null)
                if (behandling != null) {
                    val entry = behandleEnSakForBatch(behandling, dryRun)
                    batchRapport.add(entry)
                }
            }.onFailure { e ->
                log.error(e) { "Feil ved behandling av behandlingId $behandlingId" }
            }
            scenario1SisteId = behandlingId
        }

        // ===== Scenario 2: Ny vurdering potensielt overskrevet =====
        val scenario2Ider = rettOppFeilMedlPerioderRepository.finnBehandlingIderMedPotensielleNyVurderingFeil(startFraBehandlingId, pageable)
        log.info { "Scenario 2: Hentet ${scenario2Ider.size} behandlinger" }

        scenario2Ider.forEach { behandlingId ->
            runCatching {
                val behandling = rettOppFeilMedlPerioderRepository.findById(behandlingId).orElse(null)
                if (behandling != null) {
                    val entry = behandleNyVurderingSakForBatch(behandling)
                    if (entry != null) {
                        batchRapport.add(entry)
                    }
                }
            }.onFailure { e ->
                log.error(e) { "Feil ved behandling av ny vurdering behandlingId $behandlingId" }
            }
            scenario2SisteId = behandlingId
        }

        val nesteStartId = maxOf(scenario1SisteId ?: 0, scenario2SisteId ?: 0)
        val harMerData = scenario1Ider.size == batchStørrelse || scenario2Ider.size == batchStørrelse

        BatchResultat(
            batchInfo = BatchInfo(
                startFraBehandlingId = startFraBehandlingId,
                batchStørrelse = batchStørrelse,
                dryRun = dryRun
            ),
            scenario1 = ScenarioStatus(
                hentetDenneBatch = scenario1Ider.size,
                sisteBehandledeId = scenario1SisteId
            ),
            scenario2 = ScenarioStatus(
                hentetDenneBatch = scenario2Ider.size,
                sisteBehandledeId = scenario2SisteId
            ),
            nextStartFraBehandlingId = nesteStartId,
            hasMoreItems = harMerData,
            rapport = batchRapport
        )
    }

    /**
     * Behandler én sak (Scenario 1) og returnerer rapport-entry.
     * Stateless versjon av behandleEnSak for bruk i batch-modus.
     */
    private fun behandleEnSakForBatch(behandling: Behandling, dryRun: Boolean): FeilMedlPeriodeRapportEntry {
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
            return FeilMedlPeriodeRapportEntry(
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
                nyVurderingInfo = null,
                utfall = RapportUtfall.MANGLER_ARKIVSAK_ID,
                feilmelding = null,
                rettetOpp = false
            )
        }

        val erInvalidert = erSedInvalidertIEessi(behandling, arkivsakID)

        if (!erInvalidert) {
            return FeilMedlPeriodeRapportEntry(
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
                nyVurderingInfo = null,
                utfall = RapportUtfall.IKKE_INVALIDERT_I_EESSI,
                feilmelding = null,
                rettetOpp = false
            )
        }

        val medlPerioder = behandlingsresultat.lovvalgsperioder
            .filter { it.medlPeriodeID != null }
            .map { periode ->
                MedlPeriodeInfo(
                    medlPeriodeId = periode.medlPeriodeID,
                    fom = periode.fom,
                    tom = periode.tom
                )
            }

        // I dryRun gjør vi ingen endringer
        val rettetOpp = if (!dryRun) {
            rettOppSak(behandling, behandlingsresultat)
            true
        } else false

        return FeilMedlPeriodeRapportEntry(
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
            nyVurderingInfo = null,
            utfall = RapportUtfall.SKAL_RETTES_OPP,
            feilmelding = null,
            rettetOpp = rettetOpp
        )
    }

    /**
     * Behandler én ny vurdering-sak (Scenario 2) og returnerer rapport-entry.
     * Stateless versjon av behandleNyVurderingSak for bruk i batch-modus.
     */
    private fun behandleNyVurderingSakForBatch(foerstegangsbehandling: Behandling): FeilMedlPeriodeRapportEntry? {
        val fagsak = foerstegangsbehandling.fagsak
        val saksnummer = fagsak.saksnummer

        val foerstegangResultat = behandlingsresultatService.hentBehandlingsresultat(foerstegangsbehandling.id)
        val foerstegangPeriode = foerstegangResultat.lovvalgsperioder.firstOrNull()

        val nyVurderinger = fagsak.behandlinger
            .filter { it.type == Behandlingstyper.NY_VURDERING }
            .filter { it.registrertDato?.isAfter(foerstegangsbehandling.registrertDato) == true }
            .filter { it.erInaktiv() }
            .sortedByDescending { it.registrertDato }

        if (nyVurderinger.isEmpty()) {
            return null
        }

        val nyVurderingDetaljer = nyVurderinger.map { nyVurdering ->
            val nyVurderingResultat = behandlingsresultatService.hentBehandlingsresultat(nyVurdering.id)
            val nyVurderingPeriode = nyVurderingResultat.lovvalgsperioder.firstOrNull()
            NyVurderingDetaljer(
                behandlingId = nyVurdering.id,
                registrertDato = nyVurdering.registrertDato,
                status = nyVurdering.status.name,
                medlPeriodeId = nyVurderingPeriode?.medlPeriodeID,
                lovvalgsperiodeFom = nyVurderingPeriode?.fom,
                lovvalgsperiodeTom = nyVurderingPeriode?.tom
            )
        }

        val medlSammenligningInfo = hentOgSammenlignMedlPerioder(foerstegangsbehandling, foerstegangResultat)
        val medlPeriodeFraRegister = medlSammenligningInfo?.matchendeMedlPeriodeFraRegister

        val newestNyVurdering = nyVurderingDetaljer.firstOrNull()

        val matcherFoerstegangPeriode = medlPeriodeFraRegister != null &&
            foerstegangPeriode != null &&
            medlPeriodeFraRegister.fom == foerstegangPeriode.fom &&
            medlPeriodeFraRegister.tom == foerstegangPeriode.tom

        val matcherNyVurderingPeriode = medlPeriodeFraRegister != null &&
            newestNyVurdering != null &&
            medlPeriodeFraRegister.fom == newestNyVurdering.lovvalgsperiodeFom &&
            medlPeriodeFraRegister.tom == newestNyVurdering.lovvalgsperiodeTom

        val gjeldendePeriodeType = when {
            matcherNyVurderingPeriode -> "NY_VURDERING"
            matcherFoerstegangPeriode -> "FØRSTEGANG"
            medlPeriodeFraRegister != null -> "UKJENT"
            else -> null
        }

        val perioderErForskjellige = foerstegangPeriode != null && newestNyVurdering != null &&
            (foerstegangPeriode.fom != newestNyVurdering.lovvalgsperiodeFom || foerstegangPeriode.tom != newestNyVurdering.lovvalgsperiodeTom)
        val erOverskrevet = matcherFoerstegangPeriode && !matcherNyVurderingPeriode && perioderErForskjellige

        val nyVurderingInfo = NyVurderingInfo(
            foerstegangsbehandlingId = foerstegangsbehandling.id,
            foerstegangsbehandlingMedlPeriodeId = foerstegangPeriode?.medlPeriodeID,
            foerstegangsbehandlingLovvalgsperiodeFom = foerstegangPeriode?.fom,
            foerstegangsbehandlingLovvalgsperiodeTom = foerstegangPeriode?.tom,
            nyVurderinger = nyVurderingDetaljer,
            medlPeriodeLikFoerstegang = matcherFoerstegangPeriode,
            medlPeriodeLikNyVurdering = matcherNyVurderingPeriode,
            gjeldendePeriodeType = gjeldendePeriodeType
        )

        val utfall = if (erOverskrevet) {
            RapportUtfall.NY_VURDERING_OVERSKREVET
        } else {
            RapportUtfall.NY_VURDERING_IKKE_OVERSKREVET
        }

        return FeilMedlPeriodeRapportEntry(
            saksnummer = saksnummer,
            gsakSaksnummer = fagsak.gsakSaksnummer,
            saksstatus = fagsak.status.name,
            sakstype = fagsak.type.name,
            behandlingId = foerstegangsbehandling.id,
            behandlingType = foerstegangsbehandling.type.name,
            behandlingStatus = foerstegangsbehandling.status.name,
            registrertDato = foerstegangsbehandling.registrertDato,
            endretDato = foerstegangsbehandling.endretDato,
            sedInfo = null,
            alleSederFraEessi = emptyList(),
            medlPerioder = listOfNotNull(foerstegangPeriode?.let {
                MedlPeriodeInfo(
                    medlPeriodeId = it.medlPeriodeID,
                    fom = it.fom,
                    tom = it.tom
                )
            }),
            medlSammenligningInfo = medlSammenligningInfo,
            nyVurderingInfo = nyVurderingInfo,
            utfall = utfall,
            feilmelding = null,
            rettetOpp = false
        )
    }

    // Data classes for batch response
    data class BatchResultat(
        val batchInfo: BatchInfo,
        val scenario1: ScenarioStatus,
        val scenario2: ScenarioStatus,
        val nextStartFraBehandlingId: Long,
        val hasMoreItems: Boolean,
        val rapport: List<FeilMedlPeriodeRapportEntry>
    )

    data class BatchInfo(
        val startFraBehandlingId: Long,
        val batchStørrelse: Int,
        val dryRun: Boolean
    )

    data class ScenarioStatus(
        val hentetDenneBatch: Int,
        val sisteBehandledeId: Long?
    )

    private fun JobStatus.behandleEnSak(behandling: Behandling, dryRun: Boolean) {
        val saksnummer = behandling.fagsak.saksnummer
        val arkivsakID = behandling.fagsak.gsakSaksnummer
        val fagsak = behandling.fagsak

        // Hent behandlingsresultat én gang for gjenbruk
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)

        // Hent SED-info fra databasen (for bakoverkompatibilitet)
        val sedDokument = behandling.finnSedDokument()
        val sedInfoFraDb = if (sedDokument.isPresent) {
            SedInfo(
                rinaSaksnummer = sedDokument.get().rinaSaksnummer,
                rinaDokumentID = sedDokument.get().rinaDokumentID,
                sedType = sedDokument.get().sedType?.name
            )
        } else null

        // Hent alle SEDer fra EESSI for å få mer oversikt (inkl. X006/X008)
        val alleSederFraEessi = hentAlleSederFraEessi(arkivsakID, sedDokument.orElse(null)?.rinaSaksnummer)

        // Hent MEDL-perioder fra MEDL-registeret og sammenlign med lovvalgsperiode fra behandling
        val medlSammenligningInfo = hentOgSammenlignMedlPerioder(behandling, behandlingsresultat)

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
                    sedInfo = sedInfoFraDb,
                    alleSederFraEessi = emptyList(),
                    medlPerioder = emptyList(),
                    medlSammenligningInfo = null,
                    nyVurderingInfo = null,
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
                    nyVurderingInfo = null,
                    utfall = RapportUtfall.IKKE_INVALIDERT_I_EESSI,
                    feilmelding = null,
                    rettetOpp = false
                )
            )
            return
        }

        log.info { "Behandling ${behandling.id} (sak $saksnummer) er invalidert - ${if (dryRun) "ville rettet opp" else "retter opp"}" }
        skalRettesOpp++

        // Hent MEDL-periode info fra behandlingsresultat
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
                nyVurderingInfo = null,
                utfall = RapportUtfall.SKAL_RETTES_OPP,
                feilmelding = null,
                rettetOpp = !dryRun
            )
        )
    }

    /**
     * Henter alle SEDer fra EESSI for en gitt arkivsak og BUC.
     * Dette gir oversikt over alle dokumenter inkludert X006/X008 invaliderings-SEDer.
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

        // Hent MEDL-perioder fra MEDL-registeret for å sammenligne
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

        // Finn matchende periode i MEDL basert på medlPeriodeID
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

    // ===================== Scenario 2: Ny vurdering =====================

    /**
     * Behandler Scenario 2: Sjekker om førstegangsbehandlingens MEDL-periode har overskrevet
     * en nyere ny vurdering-periode.
     */
    private fun JobStatus.behandleNyVurderingSak(foerstegangsbehandling: Behandling) {
        val fagsak = foerstegangsbehandling.fagsak
        val saksnummer = fagsak.saksnummer

        // Hent MEDL-periode fra førstegangsbehandlingen
        val foerstegangResultat = behandlingsresultatService.hentBehandlingsresultat(foerstegangsbehandling.id)
        val foerstegangPeriode = foerstegangResultat.lovvalgsperioder.firstOrNull()

        // Finn ny vurdering-behandlinger på samme sak som er nyere enn førstegangsbehandlingen
        val nyVurderinger = fagsak.behandlinger
            .filter { it.type == Behandlingstyper.NY_VURDERING }
            .filter { it.registrertDato?.isAfter(foerstegangsbehandling.registrertDato) == true }
            .filter { it.erInaktiv() }
            .sortedByDescending { it.registrertDato }

        if (nyVurderinger.isEmpty()) {
            log.debug { "Ingen ny vurdering funnet for sak $saksnummer etter førstegangsbehandling ${foerstegangsbehandling.id}" }
            return
        }

        // Hent MEDL-periode info fra ny vurdering-behandlinger
        val nyVurderingDetaljer = nyVurderinger.map { nyVurdering ->
            val nyVurderingResultat = behandlingsresultatService.hentBehandlingsresultat(nyVurdering.id)
            val nyVurderingPeriode = nyVurderingResultat.lovvalgsperioder.firstOrNull()
            NyVurderingDetaljer(
                behandlingId = nyVurdering.id,
                registrertDato = nyVurdering.registrertDato,
                status = nyVurdering.status.name,
                medlPeriodeId = nyVurderingPeriode?.medlPeriodeID,
                lovvalgsperiodeFom = nyVurderingPeriode?.fom,
                lovvalgsperiodeTom = nyVurderingPeriode?.tom
            )
        }

        // Hent MEDL-info fra registeret for å sammenligne
        val medlSammenligningInfo = hentOgSammenlignMedlPerioder(foerstegangsbehandling, foerstegangResultat)
        val medlPeriodeFraRegister = medlSammenligningInfo?.matchendeMedlPeriodeFraRegister

        // Sjekk om MEDL-periodens fom/tom matcher førstegangsbehandlingen eller ny vurdering
        val newestNyVurdering = nyVurderingDetaljer.firstOrNull()

        // Sammenlign basert på fom/tom-datoer (ikke bare ID)
        val matcherFoerstegangPeriode = medlPeriodeFraRegister != null &&
            foerstegangPeriode != null &&
            medlPeriodeFraRegister.fom == foerstegangPeriode.fom &&
            medlPeriodeFraRegister.tom == foerstegangPeriode.tom

        val matcherNyVurderingPeriode = medlPeriodeFraRegister != null &&
            newestNyVurdering != null &&
            medlPeriodeFraRegister.fom == newestNyVurdering.lovvalgsperiodeFom &&
            medlPeriodeFraRegister.tom == newestNyVurdering.lovvalgsperiodeTom

        // Bestem hvilken periode som gjelder i MEDL
        val gjeldendePeriodeType = when {
            matcherNyVurderingPeriode -> "NY_VURDERING"
            matcherFoerstegangPeriode -> "FØRSTEGANG"
            medlPeriodeFraRegister != null -> "UKJENT"
            else -> null
        }

        // Bestem om ny vurdering ble overskrevet:
        // Overskrevet hvis MEDL-perioden matcher førstegangsbehandlingen men IKKE ny vurdering
        // og periodene er forskjellige (ny vurdering har endret perioden)
        val perioderErForskjellige = foerstegangPeriode != null && newestNyVurdering != null &&
            (foerstegangPeriode.fom != newestNyVurdering.lovvalgsperiodeFom || foerstegangPeriode.tom != newestNyVurdering.lovvalgsperiodeTom)
        val erOverskrevet = matcherFoerstegangPeriode && !matcherNyVurderingPeriode && perioderErForskjellige

        val nyVurderingInfo = NyVurderingInfo(
            foerstegangsbehandlingId = foerstegangsbehandling.id,
            foerstegangsbehandlingMedlPeriodeId = foerstegangPeriode?.medlPeriodeID,
            foerstegangsbehandlingLovvalgsperiodeFom = foerstegangPeriode?.fom,
            foerstegangsbehandlingLovvalgsperiodeTom = foerstegangPeriode?.tom,
            nyVurderinger = nyVurderingDetaljer,
            medlPeriodeLikFoerstegang = matcherFoerstegangPeriode,
            medlPeriodeLikNyVurdering = matcherNyVurderingPeriode,
            gjeldendePeriodeType = gjeldendePeriodeType
        )

        val utfall = if (erOverskrevet) {
            nyVurderingOverskrevet++
            log.warn { "Sak $saksnummer: Ny vurdering ble overskrevet av førstegangsbehandling!" }
            RapportUtfall.NY_VURDERING_OVERSKREVET
        } else {
            nyVurderingIkkeOverskrevet++
            log.info { "Sak $saksnummer: Ny vurdering er ikke overskrevet" }
            RapportUtfall.NY_VURDERING_IKKE_OVERSKREVET
        }

        sakerFunnet.add(
            FeilMedlPeriodeRapportEntry(
                saksnummer = saksnummer,
                gsakSaksnummer = fagsak.gsakSaksnummer,
                saksstatus = fagsak.status.name,
                sakstype = fagsak.type.name,
                behandlingId = foerstegangsbehandling.id,
                behandlingType = foerstegangsbehandling.type.name,
                behandlingStatus = foerstegangsbehandling.status.name,
                registrertDato = foerstegangsbehandling.registrertDato,
                endretDato = foerstegangsbehandling.endretDato,
                sedInfo = null,
                alleSederFraEessi = emptyList(),
                medlPerioder = listOfNotNull(foerstegangPeriode?.let {
                    MedlPeriodeInfo(
                        medlPeriodeId = it.medlPeriodeID,
                        fom = it.fom,
                        tom = it.tom
                    )
                }),
                medlSammenligningInfo = medlSammenligningInfo,
                nyVurderingInfo = nyVurderingInfo,
                utfall = utfall,
                feilmelding = null,
                rettetOpp = false
            )
        )
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
        // Scenario 1 (X008/X006)
        @Volatile var scenario1HentetDenneBatch: Int = 0,
        @Volatile var scenario1TotaltProsessert: Int = 0,
        @Volatile var manglerArkivsakId: Int = 0,
        @Volatile var ikkeInvalidertIEessi: Int = 0,
        @Volatile var skalRettesOpp: Int = 0,
        @Volatile var rettetOpp: Int = 0,
        // Scenario 2 (Ny vurdering)
        @Volatile var scenario2HentetDenneBatch: Int = 0,
        @Volatile var scenario2TotaltProsessert: Int = 0,
        @Volatile var nyVurderingIkkeOverskrevet: Int = 0,
        @Volatile var nyVurderingOverskrevet: Int = 0,
        // Paginering - siste behandlede ID for hver scenario
        @Volatile var sisteBehandledeIdScenario1: Long? = null,
        @Volatile var sisteBehandledeIdScenario2: Long? = null
    ) : JobMonitor.Stats {
        override fun reset() {
            scenario1HentetDenneBatch = 0
            scenario1TotaltProsessert = 0
            manglerArkivsakId = 0
            ikkeInvalidertIEessi = 0
            skalRettesOpp = 0
            rettetOpp = 0
            scenario2HentetDenneBatch = 0
            scenario2TotaltProsessert = 0
            nyVurderingIkkeOverskrevet = 0
            nyVurderingOverskrevet = 0
            sisteBehandledeIdScenario1 = null
            sisteBehandledeIdScenario2 = null
        }

        override fun asMap(): Map<String, Any?> = mapOf(
            // Scenario 1
            "scenario1HentetDenneBatch" to scenario1HentetDenneBatch,
            "scenario1TotaltProsessert" to scenario1TotaltProsessert,
            "manglerArkivsakId" to manglerArkivsakId,
            "ikkeInvalidertIEessi" to ikkeInvalidertIEessi,
            "skalRettesOpp" to skalRettesOpp,
            "rettetOpp" to rettetOpp,
            // Scenario 2
            "scenario2HentetDenneBatch" to scenario2HentetDenneBatch,
            "scenario2TotaltProsessert" to scenario2TotaltProsessert,
            "nyVurderingIkkeOverskrevet" to nyVurderingIkkeOverskrevet,
            "nyVurderingOverskrevet" to nyVurderingOverskrevet,
            // Paginering
            "sisteBehandledeIdScenario1" to sisteBehandledeIdScenario1,
            "sisteBehandledeIdScenario2" to sisteBehandledeIdScenario2,
            "nesteStartFraBehandlingId" to maxOf(sisteBehandledeIdScenario1 ?: 0, sisteBehandledeIdScenario2 ?: 0)
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
        val nyVurderingInfo: NyVurderingInfo?,
        val utfall: RapportUtfall,
        val feilmelding: String?,
        val rettetOpp: Boolean
    )

    /** SED-info fra databasen (behandlingens SED-dokument) */
    data class SedInfo(
        val rinaSaksnummer: String?,
        val rinaDokumentID: String?,
        val sedType: String?
    )

    /** SED-info hentet direkte fra EESSI */
    data class EessiSedInfo(
        val bucId: String?,
        val bucType: String?,
        val sedId: String?,
        val sedType: String?,
        val status: String?,
        val opprettetDato: LocalDate?,
        val erAvbrutt: Boolean
    )

    /** MEDL-periode info fra behandlingsresultat */
    data class MedlPeriodeInfo(
        val medlPeriodeId: Long?,
        val fom: LocalDate?,
        val tom: LocalDate?
    )

    /** MEDL-periode hentet direkte fra MEDL-registeret */
    data class MedlRegisterPeriode(
        val unntakId: Long?,
        val fom: LocalDate?,
        val tom: LocalDate?,
        val status: String?,
        val lovvalg: String?,
        val lovvalgsland: String?,
        val dekning: String?
    )

    /** Lovvalgsperiode fra behandlingen */
    data class LovvalgsperiodeInfo(
        val fom: LocalDate?,
        val tom: LocalDate?,
        val medlPeriodeId: Long?,
        val lovvalgsland: String?,
        val bestemmelse: String?
    )

    /** Sammenligning mellom lovvalgsperiode fra behandling og MEDL-periode fra registeret */
    data class MedlSammenligningInfo(
        val lovvalgsperiodeFraBehandling: LovvalgsperiodeInfo,
        val matchendeMedlPeriodeFraRegister: MedlRegisterPeriode?
    )

    /** Info om ny vurdering-behandlinger på samme sak (Scenario 2) */
    data class NyVurderingInfo(
        val foerstegangsbehandlingId: Long?,
        val foerstegangsbehandlingMedlPeriodeId: Long?,
        val foerstegangsbehandlingLovvalgsperiodeFom: LocalDate?,
        val foerstegangsbehandlingLovvalgsperiodeTom: LocalDate?,
        val nyVurderinger: List<NyVurderingDetaljer>,
        /** True hvis MEDL-periodens fom/tom er lik førstegangsbehandlingens lovvalgsperiode */
        val medlPeriodeLikFoerstegang: Boolean?,
        /** True hvis MEDL-periodens fom/tom er lik nyeste ny vurdering sin lovvalgsperiode */
        val medlPeriodeLikNyVurdering: Boolean?,
        /** Oppsummering: Hvilken periode gjelder i MEDL? */
        val gjeldendePeriodeType: String?
    )

    data class NyVurderingDetaljer(
        val behandlingId: Long?,
        val registrertDato: Instant?,
        val status: String?,
        val medlPeriodeId: Long?,
        val lovvalgsperiodeFom: LocalDate?,
        val lovvalgsperiodeTom: LocalDate?
    )

    enum class RapportUtfall {
        // Scenario 1 (X008/X006)
        MANGLER_ARKIVSAK_ID,
        IKKE_INVALIDERT_I_EESSI,
        SKAL_RETTES_OPP,

        // Scenario 2 (Ny vurdering)
        NY_VURDERING_IKKE_OVERSKREVET,
        NY_VURDERING_OVERSKREVET
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .registerModule(JavaTimeModule())
    }
}
