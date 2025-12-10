package no.nav.melosys.service.behandling.jobb

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
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
 * Feil 1 Del 2: Fagsaker med flere behandlinger der A003 har blitt ugyldiggjort (X008/X006).
 *
 * Del 2a: Siste behandling har resultat REGISTRERT_UNNTAK
 * - Saksstatus endres IKKE (det finnes en gyldig periode fra siste behandling)
 * - Kun MEDL-perioder på HENLEGGELSE-behandlinger settes til avvist
 *
 * Del 2b: Alle behandlinger har resultat HENLEGGELSE
 * - Saksstatus settes til ANNULLERT
 * - Alle MEDL-perioder settes til avvist
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

    /** Holder styr på hvilke fagsaker vi allerede har prosessert i denne kjøringen */
    private val prosesserteFagsaker = mutableSetOf<String>()

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
        saksnummerFilter: String? = null
    ) {
        kjør(dryRun, antallFeilFørStopp, batchStørrelse, startFraBehandlingId, behandlingIderFilter, saksnummerFilter)
    }

    @Synchronized
    @Transactional
    fun kjør(
        dryRun: Boolean,
        antallFeilFørStopp: Int = 0,
        batchStørrelse: Int = 1000,
        startFraBehandlingId: Long = 0,
        behandlingIderFilter: Set<Long>? = null,
        saksnummerFilter: String? = null
    ) = runAsSystem {
        sakerFunnet.clear()
        prosesserteFagsaker.clear()
        jobMonitor.execute(antallFeilFørStopp) {
            log.info { "Starter RettOppFeilMedlPerioderJob Del 2 (dryRun=$dryRun, batchStørrelse=$batchStørrelse, startFraBehandlingId=$startFraBehandlingId, filter=${behandlingIderFilter?.size ?: "ingen"}, saksnummer=${saksnummerFilter ?: "alle"})" }

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
                        behandleEnSak(behandling, dryRun, saksnummerFilter)
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

    private fun JobStatus.behandleEnSak(behandling: Behandling, dryRun: Boolean, saksnummerFilter: String?) {
        val fagsak = behandling.fagsak

        // Filtrer på saksnummer hvis oppgitt
        if (saksnummerFilter != null && fagsak.saksnummer != saksnummerFilter) {
            filtrertBort++
            return
        }

        // Unngå å prosessere samme fagsak flere ganger (kan ha flere HENLEGGELSE-behandlinger)
        if (fagsak.saksnummer in prosesserteFagsaker) {
            log.debug { "Fagsak ${fagsak.saksnummer} er allerede prosessert i denne kjøringen, hopper over" }
            filtrertBort++
            return
        }
        prosesserteFagsaker.add(fagsak.saksnummer)

        val arkivsakID = fagsak.gsakSaksnummer

        // Sjekk arkivsak-ID først
        if (arkivsakID == null) {
            log.warn { "Sak ${fagsak.saksnummer} mangler gsakSaksnummer, hopper over" }
            manglerArkivsakId++
            sakerFunnet.add(createBasicRapportEntry(behandling, RapportUtfall.MANGLER_ARKIVSAK_ID))
            return
        }

        // Hent SEDer fra EESSI for å telle A003
        val sedDokument = behandling.finnSedDokument()
        val alleSederFraEessi = hentAlleSederFraEessi(arkivsakID, sedDokument.orElse(null)?.rinaSaksnummer)
        val antallA003 = alleSederFraEessi.count { it.sedType == "A003" }
        val antallBehandlinger = fagsak.behandlinger.size

        // Del 2 krever mer enn 1 behandling
        if (antallBehandlinger <= 1) {
            log.debug { "Fagsak ${fagsak.saksnummer} har kun 1 behandling, ikke Del 2 kriterie" }
            ikkeDel2Kriterie++
            sakerFunnet.add(createBasicRapportEntry(behandling, RapportUtfall.IKKE_DEL2_KRITERIE,
                "Del 2 krever mer enn 1 behandling, men fant $antallBehandlinger", antallA003, antallBehandlinger))
            return
        }

        // A003 ubalanse - kan ikke fikses trygt
        val runtimeSafe = antallA003 <= antallBehandlinger
        if (!runtimeSafe) {
            log.info { "Fagsak ${fagsak.saksnummer} er UNSAFE: $antallA003 A003 > $antallBehandlinger behandlinger" }
            unsafeA003Ubalanse++
            sakerFunnet.add(createBasicRapportEntry(behandling, RapportUtfall.UNSAFE_A003_UBALANSE,
                "A003 count ($antallA003) > behandling count ($antallBehandlinger)", antallA003, antallBehandlinger))
            return
        }

        // Hent behandlingsresultat for alle behandlinger på fagsaken
        val alleBehandlingerMedResultat = fagsak.behandlinger
            .map { beh ->
                val resultat = try {
                    behandlingsresultatService.hentBehandlingsresultat(beh.id)
                } catch (e: Exception) {
                    log.warn { "Kunne ikke hente behandlingsresultat for behandling ${beh.id}: ${e.message}" }
                    null
                }
                beh to resultat
            }
            .filter { it.second != null }
            .map { it.first to it.second!! }
            .sortedBy { it.first.registrertDato }

        if (alleBehandlingerMedResultat.isEmpty()) {
            log.warn { "Fagsak ${fagsak.saksnummer} har ingen behandlinger med resultat" }
            return
        }

        // Finn siste behandling og kategoriser
        val sisteBehandling = alleBehandlingerMedResultat.last()
        val sisteResultatType = sisteBehandling.second.type

        val henleggelseBehandlinger = alleBehandlingerMedResultat.filter { it.second.type == Behandlingsresultattyper.HENLEGGELSE }
        val alleErHenleggelse = alleBehandlingerMedResultat.all { it.second.type == Behandlingsresultattyper.HENLEGGELSE }
        val sisteErRegistrertUnntak = sisteResultatType == Behandlingsresultattyper.REGISTRERT_UNNTAK

        log.info { "Fagsak ${fagsak.saksnummer}: ${alleBehandlingerMedResultat.size} behandlinger, " +
            "${henleggelseBehandlinger.size} HENLEGGELSE, siste resultat=$sisteResultatType" }

        // Kategoriser og prosesser
        when {
            sisteErRegistrertUnntak -> {
                // Del 2a: Siste behandling er REGISTRERT_UNNTAK
                prosesserDel2a(fagsak.saksnummer, henleggelseBehandlinger, arkivsakID, dryRun, antallA003, antallBehandlinger)
            }
            alleErHenleggelse -> {
                // Del 2b: Alle behandlinger er HENLEGGELSE
                prosesserDel2b(fagsak.saksnummer, fagsak, alleBehandlingerMedResultat, arkivsakID, dryRun, antallA003, antallBehandlinger)
            }
            else -> {
                // Del 2 Annet: Blandet resultat - manuell vurdering
                log.info { "Fagsak ${fagsak.saksnummer} har blandet resultat - krever manuell vurdering" }
                del2Annet++
                sakerFunnet.add(createBasicRapportEntry(behandling, RapportUtfall.DEL2_ANNET,
                    "Blandet resultat: siste=$sisteResultatType, ${henleggelseBehandlinger.size} HENLEGGELSE av ${alleBehandlingerMedResultat.size}",
                    antallA003, antallBehandlinger))
            }
        }
    }

    /**
     * Del 2a: Siste behandling har REGISTRERT_UNNTAK.
     * - Saksstatus endres IKKE
     * - Kun MEDL-perioder på HENLEGGELSE-behandlinger avvises
     */
    private fun JobStatus.prosesserDel2a(
        saksnummer: String,
        henleggelseBehandlinger: List<Pair<Behandling, no.nav.melosys.domain.Behandlingsresultat>>,
        arkivsakID: Long,
        dryRun: Boolean,
        antallA003: Int,
        antallBehandlinger: Int
    ) {
        log.info { "Del 2a: Fagsak $saksnummer - siste behandling er REGISTRERT_UNNTAK, prosesserer ${henleggelseBehandlinger.size} HENLEGGELSE-behandlinger" }

        henleggelseBehandlinger.forEach { (beh, resultat) ->
            // Sjekk at denne behandlingens SED er invalidert i EESSI
            val erInvalidert = try {
                erSedInvalidertIEessi(beh, arkivsakID)
            } catch (e: Exception) {
                log.error(e) { "Kunne ikke sjekke EESSI for behandling ${beh.id}" }
                eessiFeil++
                jobMonitor.registerException(e)
                sakerFunnet.add(createBasicRapportEntry(beh, RapportUtfall.EESSI_FEIL, e.message, antallA003, antallBehandlinger))
                return@forEach
            }

            if (!erInvalidert) {
                log.info { "Behandling ${beh.id} på sak $saksnummer er ikke invalidert i EESSI, hopper over" }
                ikkeInvalidertIEessi++
                sakerFunnet.add(createBasicRapportEntry(beh, RapportUtfall.IKKE_INVALIDERT_I_EESSI, null, antallA003, antallBehandlinger))
                return@forEach
            }

            // Hent MEDL-status for hver periode og filtrer bort de som allerede er AVST
            val medlPerioderMedStatus = resultat.lovvalgsperioder
                .filter { it.medlPeriodeID != null }
                .map { periode ->
                    val status = medlPeriodeService.hentPeriodeStatus(periode.medlPeriodeID)
                    val skalRettes = status != "AVST"
                    val grunn = if (!skalRettes) "Allerede AVST" else null
                    MedlPeriodeInfo(
                        medlPeriodeId = periode.medlPeriodeID,
                        fom = periode.fom,
                        tom = periode.tom,
                        status = status,
                        skalRettes = skalRettes,
                        hoppetOverGrunn = grunn
                    )
                }

            val perioderSomSkalRettes = medlPerioderMedStatus.filter { it.skalRettes }
            val perioderHoppetOver = medlPerioderMedStatus.filter { !it.skalRettes }

            if (perioderHoppetOver.isNotEmpty()) {
                log.info { "Behandling ${beh.id} på sak $saksnummer: ${perioderHoppetOver.size} MEDL-perioder hoppet over (allerede AVST)" }
            }

            if (perioderSomSkalRettes.isEmpty()) {
                log.info { "Behandling ${beh.id} på sak $saksnummer har ingen MEDL-perioder å avvise (alle er AVST)" }
                sakerFunnet.add(createBasicRapportEntry(beh, RapportUtfall.DEL2A_RETTET, "Alle perioder allerede AVST", antallA003, antallBehandlinger, false, medlPerioderMedStatus))
                return@forEach
            }

            skalRettesOpp++
            log.info { "Del 2a: Behandling ${beh.id} (sak $saksnummer) skal avvise ${perioderSomSkalRettes.size} MEDL-perioder (saksstatus endres IKKE)" }

            if (!dryRun) {
                rettOppBehandlingMedStatusSjekk(resultat, saksnummer)
                rettetOpp++
            }

            sakerFunnet.add(createBasicRapportEntry(beh, RapportUtfall.DEL2A_RETTET, null, antallA003, antallBehandlinger, !dryRun, medlPerioderMedStatus))
        }
    }

    /**
     * Del 2b: Alle behandlinger er HENLEGGELSE.
     * - Saksstatus settes til ANNULLERT
     * - Alle MEDL-perioder avvises
     */
    private fun JobStatus.prosesserDel2b(
        saksnummer: String,
        fagsak: no.nav.melosys.domain.Fagsak,
        alleBehandlingerMedResultat: List<Pair<Behandling, no.nav.melosys.domain.Behandlingsresultat>>,
        arkivsakID: Long,
        dryRun: Boolean,
        antallA003: Int,
        antallBehandlinger: Int
    ) {
        log.info { "Del 2b: Fagsak $saksnummer - alle ${alleBehandlingerMedResultat.size} behandlinger er HENLEGGELSE" }

        // Sjekk at minst én behandling er invalidert i EESSI
        val invalidertBehandlinger = mutableListOf<Pair<Behandling, no.nav.melosys.domain.Behandlingsresultat>>()

        alleBehandlingerMedResultat.forEach { (beh, resultat) ->
            val erInvalidert = try {
                erSedInvalidertIEessi(beh, arkivsakID)
            } catch (e: Exception) {
                log.error(e) { "Kunne ikke sjekke EESSI for behandling ${beh.id}" }
                eessiFeil++
                jobMonitor.registerException(e)
                false
            }

            if (erInvalidert) {
                invalidertBehandlinger.add(beh to resultat)
            } else {
                log.info { "Behandling ${beh.id} på sak $saksnummer er ikke invalidert i EESSI" }
                ikkeInvalidertIEessi++
                sakerFunnet.add(createBasicRapportEntry(beh, RapportUtfall.IKKE_INVALIDERT_I_EESSI, null, antallA003, antallBehandlinger))
            }
        }

        if (invalidertBehandlinger.isEmpty()) {
            log.info { "Ingen behandlinger på fagsak $saksnummer er invalidert i EESSI, hopper over" }
            return
        }

        // Samle alle MEDL-perioder med status-sjekk
        val alleMedlPerioderMedStatus = invalidertBehandlinger.flatMap { (_, resultat) ->
            resultat.lovvalgsperioder
                .filter { it.medlPeriodeID != null }
                .map { periode ->
                    val status = medlPeriodeService.hentPeriodeStatus(periode.medlPeriodeID)
                    val skalRettes = status != "AVST"
                    val grunn = if (!skalRettes) "Allerede AVST" else null
                    MedlPeriodeInfo(
                        medlPeriodeId = periode.medlPeriodeID,
                        fom = periode.fom,
                        tom = periode.tom,
                        status = status,
                        skalRettes = skalRettes,
                        hoppetOverGrunn = grunn
                    )
                }
        }

        val perioderSomSkalRettes = alleMedlPerioderMedStatus.filter { it.skalRettes }
        val perioderHoppetOver = alleMedlPerioderMedStatus.filter { !it.skalRettes }

        if (perioderHoppetOver.isNotEmpty()) {
            log.info { "Fagsak $saksnummer: ${perioderHoppetOver.size} MEDL-perioder hoppet over (allerede AVST)" }
        }

        skalRettesOpp++
        log.info { "Del 2b: Fagsak $saksnummer skal settes til ANNULLERT og avvise ${perioderSomSkalRettes.size} MEDL-perioder" }

        if (!dryRun) {
            // Først sett saksstatus til ANNULLERT
            fagsakService.oppdaterStatus(fagsak, Saksstatuser.ANNULLERT)
            log.info { "Satt saksstatus til ANNULLERT for sak $saksnummer" }

            // Avvis alle MEDL-perioder (med status-sjekk)
            invalidertBehandlinger.forEach { (_, resultat) ->
                rettOppBehandlingMedStatusSjekk(resultat, saksnummer)
            }
            rettetOpp++
        }

        // Legg til rapport-entry for første behandling med alle MEDL-perioder
        val førsteBehandling = invalidertBehandlinger.first().first
        sakerFunnet.add(createBasicRapportEntry(førsteBehandling, RapportUtfall.DEL2B_RETTET,
            "Alle ${invalidertBehandlinger.size} behandlinger er HENLEGGELSE, saksstatus→ANNULLERT, ${perioderSomSkalRettes.size} perioder å rette",
            antallA003, antallBehandlinger, !dryRun, alleMedlPerioderMedStatus))
    }

    /**
     * Avviser MEDL-perioder for en behandling, men sjekker først at status ikke er AVST.
     */
    private fun rettOppBehandlingMedStatusSjekk(
        behandlingsresultat: no.nav.melosys.domain.Behandlingsresultat,
        saksnummer: String
    ) {
        behandlingsresultat.lovvalgsperioder.forEach { periode ->
            if (periode.medlPeriodeID != null) {
                try {
                    val status = medlPeriodeService.hentPeriodeStatus(periode.medlPeriodeID)
                    if (status == "AVST") {
                        log.info { "MEDL-periode ${periode.medlPeriodeID} for sak $saksnummer har allerede status AVST, hopper over" }
                        return@forEach
                    }
                    medlPeriodeService.avvisPeriode(periode.medlPeriodeID)
                    log.info { "Avvist MEDL-periode ${periode.medlPeriodeID} for sak $saksnummer (var: $status)" }
                } catch (e: Exception) {
                    log.error(e) { "Kunne ikke avvise MEDL-periode ${periode.medlPeriodeID}" }
                    throw e
                }
            }
        }
    }

    /**
     * Lager en enkel rapport-entry for en behandling.
     */
    private fun createBasicRapportEntry(
        behandling: Behandling,
        utfall: RapportUtfall,
        feilmelding: String? = null,
        antallA003: Int = 0,
        antallBehandlinger: Int = 0,
        rettetOpp: Boolean = false,
        medlPerioder: List<MedlPeriodeInfo> = emptyList()
    ): FeilMedlPeriodeRapportEntry {
        val fagsak = behandling.fagsak
        val sedDokument = behandling.finnSedDokument()
        val sedInfoFraDb = sedDokument.orElse(null)?.let {
            SedInfo(rinaSaksnummer = it.rinaSaksnummer, rinaDokumentID = it.rinaDokumentID, sedType = it.sedType?.name)
        }

        return FeilMedlPeriodeRapportEntry(
            saksnummer = fagsak.saksnummer,
            gsakSaksnummer = fagsak.gsakSaksnummer,
            saksstatus = fagsak.status.name,
            sakstype = fagsak.type.name,
            behandlingId = behandling.id,
            behandlingType = behandling.type.name,
            behandlingStatus = behandling.status.name,
            registrertDato = behandling.registrertDato,
            endretDato = behandling.endretDato,
            sedInfo = sedInfoFraDb,
            alleSederFraEessi = emptyList(),
            medlPerioder = medlPerioder,
            medlSammenligningInfo = null,
            antallA003 = antallA003,
            antallBehandlinger = antallBehandlinger,
            utfall = utfall,
            feilmelding = feilmelding,
            rettetOpp = rettetOpp
        )
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
        @Volatile var ikkeDel2Kriterie: Int = 0,
        @Volatile var del2Annet: Int = 0,
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
            ikkeDel2Kriterie = 0
            del2Annet = 0
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
            "ikkeDel2Kriterie" to ikkeDel2Kriterie,
            "del2Annet" to del2Annet,
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
        val tom: LocalDate?,
        val status: String? = null,
        val skalRettes: Boolean = true,
        val hoppetOverGrunn: String? = null
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
        EESSI_FEIL,
        UNSAFE_A003_UBALANSE,
        IKKE_DEL2_KRITERIE,     // Kun 1 behandling (Del 1 er ferdig)
        DEL2_ANNET,             // Blandet resultat - krever manuell vurdering
        DEL2A_RETTET,           // Siste behandling er REGISTRERT_UNNTAK - kun MEDL avvist
        DEL2B_RETTET            // Alle behandlinger er HENLEGGELSE - saksstatus + MEDL rettet
    }

    companion object {
        private val objectMapper = jacksonObjectMapper()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .registerModule(JavaTimeModule())
    }
}
