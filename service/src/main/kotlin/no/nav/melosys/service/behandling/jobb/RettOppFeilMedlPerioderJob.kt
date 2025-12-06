package no.nav.melosys.service.behandling.jobb

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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

    /**
     * Forhåndsanalyserte lister med SAFE/UNSAFE behandlingIder for verifikasjon.
     * Brukes til å sammenligne runtime-sjekk med forhåndsanalyse.
     */
    val knownSafeIds: Set<Long> = loadVerificationList("/rett-opp-feil-medl/safe-behandling-ids.json")
    private val knownUnsafeIds: Set<Long> = loadVerificationList("/rett-opp-feil-medl/unsafe-behandling-ids.json")

    /**
     * Del 1: SAFE med nøyaktig 1 behandling og 1 A003.
     * Dette er de enkleste sakene som kan fikses automatisk.
     */
    val del1SafeIds: Set<Long> = loadVerificationList("/rett-opp-feil-medl/del1-safe-1-behandling-ids.json")

    private fun loadVerificationList(resourcePath: String): Set<Long> {
        return try {
            val resource = javaClass.getResourceAsStream(resourcePath)
            if (resource != null) {
                val ids: List<Long> = objectMapper.readValue(resource)
                log.info { "Lastet ${ids.size} behandlingIder fra $resourcePath" }
                ids.toSet()
            } else {
                log.warn { "Fant ikke ressursfil $resourcePath - verifikasjon vil bli hoppet over" }
                emptySet()
            }
        } catch (e: Exception) {
            log.error(e) { "Kunne ikke laste verifikasjonsliste fra $resourcePath" }
            emptySet()
        }
    }

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
    fun kjørAsynkront(
        dryRun: Boolean,
        antallFeilFørStopp: Int,
        batchStørrelse: Int = 1000,
        startFraBehandlingId: Long = 0,
        behandlingIderFilter: Set<Long>? = null,
        verifiserDel1Kriterie: Boolean = false,
        saksnummerFilter: String? = null
    ) {
        kjør(dryRun, antallFeilFørStopp, batchStørrelse, startFraBehandlingId, behandlingIderFilter, verifiserDel1Kriterie, saksnummerFilter)
    }

    @Synchronized
    @Transactional
    fun kjør(
        dryRun: Boolean,
        antallFeilFørStopp: Int = 0,
        batchStørrelse: Int = 1000,
        startFraBehandlingId: Long = 0,
        behandlingIderFilter: Set<Long>? = null,
        verifiserDel1Kriterie: Boolean = false,
        saksnummerFilter: String? = null
    ) = runAsSystem {
        sakerFunnet.clear()
        jobMonitor.execute(antallFeilFørStopp) {
            log.info { "Starter RettOppFeilMedlPerioderJob (dryRun=$dryRun, batchStørrelse=$batchStørrelse, startFraBehandlingId=$startFraBehandlingId, filter=${behandlingIderFilter?.size ?: "ingen"}, verifiserDel1Kriterie=$verifiserDel1Kriterie, saksnummer=${saksnummerFilter ?: "alle"})" }

            val pageable = PageRequest.of(0, batchStørrelse)
            val berørteBehandlingIder = rettOppFeilMedlPerioderRepository.finnBehandlingIderMedFeilStatus(startFraBehandlingId, pageable)
            hentetDenneBatch = berørteBehandlingIder.size
            log.info { "Hentet $hentetDenneBatch behandlinger (startFraBehandlingId=$startFraBehandlingId, batchStørrelse=$batchStørrelse)" }

            berørteBehandlingIder.forEach { behandlingId ->
                if (jobMonitor.shouldStop) return@execute

                // Skip hvis filter er oppgitt og behandlingId ikke er i filteret
                if (behandlingIderFilter != null && behandlingId !in behandlingIderFilter) {
                    log.debug { "Behandling $behandlingId ikke i filter, hopper over" }
                    filtrertBort++
                    totaltProsessert++
                    sisteBehandledeId = behandlingId
                    return@forEach
                }

                runCatching {
                    val behandling = rettOppFeilMedlPerioderRepository.findById(behandlingId).orElse(null)
                    if (behandling != null) {
                        behandleEnSak(behandling, dryRun, verifiserDel1Kriterie, saksnummerFilter)
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

    private fun JobStatus.behandleEnSak(behandling: Behandling, dryRun: Boolean, verifiserDel1Kriterie: Boolean, saksnummerFilter: String?) {
        val fagsak = behandling.fagsak

        // Filtrer på saksnummer hvis oppgitt
        if (saksnummerFilter != null && fagsak.saksnummer != saksnummerFilter) {
            filtrertBort++
            return
        }

        val arkivsakID = fagsak.gsakSaksnummer
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)

        val sedDokument = behandling.finnSedDokument()
        val sedInfoFraDb = sedDokument.orElse(null)?.let {
            SedInfo(rinaSaksnummer = it.rinaSaksnummer, rinaDokumentID = it.rinaDokumentID, sedType = it.sedType?.name)
        }
        val alleSederFraEessi = hentAlleSederFraEessi(arkivsakID, sedDokument.orElse(null)?.rinaSaksnummer)
        val medlSammenligningInfo = hentOgSammenlignMedlPerioder(behandling, behandlingsresultat)

        // Opprett context med grunndata - A003/behandling-telling legges til etter arkivsak-sjekk
        fun createContext(antallA003: Int = 0, antallBehandlinger: Int = 0) = BehandlingContext(
            behandling = behandling,
            arkivsakID = arkivsakID,
            sedInfoFraDb = sedInfoFraDb,
            alleSederFraEessi = alleSederFraEessi,
            medlSammenligningInfo = medlSammenligningInfo,
            antallA003 = antallA003,
            antallBehandlinger = antallBehandlinger
        )

        // Sjekk arkivsak-ID først (før vi har A003-telling)
        if (arkivsakID == null) {
            log.warn { "Sak ${fagsak.saksnummer} mangler gsakSaksnummer, hopper over" }
            manglerArkivsakId++
            sakerFunnet.add(createContext().toRapportEntry(RapportUtfall.MANGLER_ARKIVSAK_ID))
            return
        }

        // Beregn A003/behandling-telling
        val antallA003 = alleSederFraEessi.count { it.sedType == "A003" }
        val antallBehandlinger = fagsak.behandlinger.size
        val ctx = createContext(antallA003, antallBehandlinger)
        val runtimeSafe = antallA003 <= antallBehandlinger

        // Sjekk mot forhåndsanalyserte lister
        if (runtimeSafe && behandling.id in knownUnsafeIds) {
            log.warn { "MISMATCH: Behandling ${behandling.id} er SAFE nå ($antallA003 A003 <= $antallBehandlinger beh), men var UNSAFE i forhåndsanalyse" }
            tilstandMismatch++
            sakerFunnet.add(ctx.toRapportEntry(
                RapportUtfall.TILSTAND_ENDRET,
                "Runtime SAFE ($antallA003 A003 <= $antallBehandlinger beh), men var UNSAFE i forhåndsanalyse. Manuell fiks?"
            ))
            return
        }

        if (!runtimeSafe && behandling.id in knownSafeIds) {
            log.warn { "MISMATCH: Behandling ${behandling.id} er UNSAFE nå ($antallA003 A003 > $antallBehandlinger beh), men var SAFE i forhåndsanalyse" }
            tilstandMismatch++
            sakerFunnet.add(ctx.toRapportEntry(
                RapportUtfall.TILSTAND_ENDRET,
                "Runtime UNSAFE ($antallA003 A003 > $antallBehandlinger beh), men var SAFE i forhåndsanalyse. Data endret?"
            ))
            return
        }

        // A003 ubalanse - kan ikke fikses trygt
        if (!runtimeSafe) {
            log.info { "Behandling ${behandling.id} (sak ${ctx.saksnummer}) er UNSAFE: $antallA003 A003 > $antallBehandlinger behandlinger" }
            unsafeA003Ubalanse++
            sakerFunnet.add(ctx.toRapportEntry(
                RapportUtfall.UNSAFE_A003_UBALANSE,
                "A003 count ($antallA003) > behandling count ($antallBehandlinger)"
            ))
            return
        }

        // Del 1 verifisering: Krever nøyaktig 1 behandling og 1 A003
        if (verifiserDel1Kriterie && (antallBehandlinger != 1 || antallA003 != 1)) {
            log.info { "Behandling ${behandling.id} (sak ${ctx.saksnummer}) oppfyller ikke Del 1 kriterier: $antallBehandlinger behandlinger, $antallA003 A003 (krever 1 av hver)" }
            ikkeDel1Kriterie++
            sakerFunnet.add(ctx.toRapportEntry(
                RapportUtfall.IKKE_DEL1_KRITERIE,
                "Del 1 krever 1 behandling og 1 A003, men fant $antallBehandlinger behandlinger og $antallA003 A003"
            ))
            return
        }

        // Sjekk EESSI invalidering med feilhåndtering
        val erInvalidert: Boolean? = try {
            erSedInvalidertIEessi(behandling, arkivsakID)
        } catch (e: Exception) {
            log.error(e) { "Kunne ikke sjekke EESSI for behandling ${behandling.id} (arkivsak $arkivsakID)" }
            eessiFeil++
            jobMonitor.registerException(e)
            sakerFunnet.add(ctx.toRapportEntry(RapportUtfall.EESSI_FEIL, e.message))
            null
        }

        if (erInvalidert == null) return  // EESSI-feil allerede håndtert

        if (!erInvalidert) {
            log.info { "Behandling ${behandling.id} (sak ${ctx.saksnummer}) er ikke invalidert i EESSI, hopper over" }
            ikkeInvalidertIEessi++
            sakerFunnet.add(ctx.toRapportEntry(RapportUtfall.IKKE_INVALIDERT_I_EESSI))
            return
        }

        log.info { "Behandling ${behandling.id} (sak ${ctx.saksnummer}) er SAFE og invalidert - ${if (dryRun) "ville rettet opp" else "retter opp"}" }
        skalRettesOpp++

        val medlPerioder = behandlingsresultat.lovvalgsperioder
            .filter { it.medlPeriodeID != null }
            .map { MedlPeriodeInfo(medlPeriodeId = it.medlPeriodeID, fom = it.fom, tom = it.tom) }

        if (!dryRun) {
            rettOppSak(behandling, behandlingsresultat)
            rettetOpp++
        }

        sakerFunnet.add(ctx.toRapportEntry(
            utfall = RapportUtfall.SKAL_RETTES_OPP,
            rettetOpp = !dryRun,
            medlPerioder = medlPerioder
        ))
    }

    /**
     * Henter alle SEDer fra EESSI for en gitt arkivsak og BUC.
     */
    private fun hentAlleSederFraEessi(arkivsakID: Long?, rinaSaksnummer: String?): List<EessiSedInfo> {
        if (arkivsakID == null) return emptyList()

        // Kun A003, X006 og X008 er relevante for denne jobben
        val relevanteSedTyper = setOf("A003", "X006", "X008")

        return try {
            eessiService.hentTilknyttedeBucer(arkivsakID, emptyList())
                .filter { rinaSaksnummer == null || it.id == rinaSaksnummer }
                .flatMap { buc ->
                    buc.seder
                        .filter { it.sedType in relevanteSedTyper }
                        .map { sed ->
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

    /**
     * Sjekker om SED er invalidert i EESSI.
     * Kaster exception ved EESSI-feil så kaller kan håndtere det.
     */
    private fun erSedInvalidertIEessi(behandling: Behandling, arkivsakID: Long): Boolean {
        val sedDokument = behandling.finnSedDokument()
        if (sedDokument.isEmpty) {
            log.debug { "Behandling ${behandling.id} har ingen SED-dokument" }
            return false
        }

        val rinaSaksnummer = sedDokument.get().rinaSaksnummer
        val rinaDokumentID = sedDokument.get().rinaDokumentID

        // Ikke fang exceptions - la dem propagere til kaller som har EESSI_FEIL-håndtering
        val bucer = eessiService.hentTilknyttedeBucer(arkivsakID, emptyList())
            .filter { it.id == rinaSaksnummer }

        // Sjekk at behandlingens SED er markert som avbrutt
        val sedErAvbrutt = bucer
            .flatMap { it.seder }
            .filter { it.sedId == rinaDokumentID }
            .any { it.erAvbrutt() }

        // Sjekk at det finnes en X008 eller X006 som har invalidert SEDen
        val harX008EllerX006 = bucer
            .flatMap { it.seder }
            .any { it.sedType in listOf("X008", "X006") }

        if (sedErAvbrutt && !harX008EllerX006) {
            log.warn { "Behandling ${behandling.id}: SED er avbrutt men fant ingen X008/X006 i BUC $rinaSaksnummer" }
        }

        return sedErAvbrutt && harX008EllerX006
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
        @Volatile var eessiFeil: Int = 0,
        @Volatile var unsafeA003Ubalanse: Int = 0,
        @Volatile var tilstandMismatch: Int = 0,
        @Volatile var ikkeDel1Kriterie: Int = 0,
        @Volatile var filtrertBort: Int = 0,
        @Volatile var skalRettesOpp: Int = 0,
        @Volatile var rettetOpp: Int = 0,
        @Volatile var sisteBehandledeId: Long? = null
    ) : JobMonitor.Stats {
        override fun reset() {
            hentetDenneBatch = 0
            totaltProsessert = 0
            manglerArkivsakId = 0
            ikkeInvalidertIEessi = 0
            eessiFeil = 0
            unsafeA003Ubalanse = 0
            tilstandMismatch = 0
            ikkeDel1Kriterie = 0
            filtrertBort = 0
            skalRettesOpp = 0
            rettetOpp = 0
            sisteBehandledeId = null
        }

        override fun asMap(): Map<String, Any?> = mapOf(
            "hentetDenneBatch" to hentetDenneBatch,
            "totaltProsessert" to totaltProsessert,
            "manglerArkivsakId" to manglerArkivsakId,
            "ikkeInvalidertIEessi" to ikkeInvalidertIEessi,
            "eessiFeil" to eessiFeil,
            "unsafeA003Ubalanse" to unsafeA003Ubalanse,
            "tilstandMismatch" to tilstandMismatch,
            "ikkeDel1Kriterie" to ikkeDel1Kriterie,
            "filtrertBort" to filtrertBort,
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
        val antallA003: Int? = null,
        val antallBehandlinger: Int? = null,
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

    /**
     * Context-klasse som samler all informasjon som trengs for å lage rapport-entries.
     * Brukes for å unngå duplisering av FeilMedlPeriodeRapportEntry-konstruksjon.
     */
    private data class BehandlingContext(
        val behandling: Behandling,
        val arkivsakID: Long?,
        val sedInfoFraDb: SedInfo?,
        val alleSederFraEessi: List<EessiSedInfo>,
        val medlSammenligningInfo: MedlSammenligningInfo?,
        val antallA003: Int = 0,
        val antallBehandlinger: Int = 0
    ) {
        val fagsak get() = behandling.fagsak
        val saksnummer get() = fagsak.saksnummer

        fun toRapportEntry(
            utfall: RapportUtfall,
            feilmelding: String? = null,
            rettetOpp: Boolean = false,
            medlPerioder: List<MedlPeriodeInfo> = emptyList()
        ) = FeilMedlPeriodeRapportEntry(
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
            antallA003 = antallA003,
            antallBehandlinger = antallBehandlinger,
            utfall = utfall,
            feilmelding = feilmelding,
            rettetOpp = rettetOpp
        )
    }

    enum class RapportUtfall {
        MANGLER_ARKIVSAK_ID,
        IKKE_INVALIDERT_I_EESSI,
        EESSI_FEIL,
        UNSAFE_A003_UBALANSE,
        TILSTAND_ENDRET,
        IKKE_DEL1_KRITERIE,     // Runtime viser at saken ikke oppfyller Del 1 kriterier (1 beh, 1 A003)
        SKAL_RETTES_OPP
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .registerModule(JavaTimeModule())
    }
}
