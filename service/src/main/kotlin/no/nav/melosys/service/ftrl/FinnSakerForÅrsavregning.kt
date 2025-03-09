package no.nav.melosys.service.ftrl

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.hendelser.KafkaMelosysHendelseProducer
import no.nav.melosys.integrasjon.hendelser.MelosysHendelse
import no.nav.melosys.integrasjon.hendelser.Periode
import no.nav.melosys.integrasjon.hendelser.VedtakHendelseMelding
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.jvm.optionals.getOrNull

private val log = KotlinLogging.logger { }

@Component
class FinnSakerForÅrsavregning(
    private val sakerForÅrsavregningRepository: SakerForÅrsavregningRepository,
    private val kafkaMelosysHendelseProducer: KafkaMelosysHendelseProducer,
    private val persondataService: PersondataService,
    private val behandlingsresultatService: BehandlingsresultatService,
) {
    private val jobMonitor = JobMonitor(
        jobName = "FinnSakerForÅrsavregning",
        stats = JobStatus()
    )

    @Async("taskScheduler")
    @Transactional(readOnly = true)
    fun finnSakerOgLeggPåKøAsynkront(dryrun: Boolean, antallFeilFørStopAvJob: Int = 0) {
        finnSakerOgLeggPåKø(dryrun, antallFeilFørStopAvJob)
    }

    @Synchronized
    @Transactional(readOnly = true)
    fun finnSakerOgLeggPåKø(dryrun: Boolean, antallFeilFørStopAvJob: Int = 0) = runAsSystem {
        jobMonitor.execute(antallFeilFørStopAvJob) {
            hentMelosysHendelser()
                .onEach { antallFunnet++ }
                .filterNot { dryrun }
                .forEach {
                    if (jobMonitor.shouldStop) return@execute
                    kafkaMelosysHendelseProducer.produserBestillingsmelding(it)
                    meldingerSentAntall++
                }
        }
    }

    private fun finnFolkeregisterident(ident: String): String? =
        try {
            persondataService.finnFolkeregisterident(ident).getOrNull()
        } catch (_: IkkeFunnetException) {
            null
        }

    private fun Fagsak.hentFolkeregisterident(): String? =
        finnFolkeregisterident(this.hentBrukersAktørID()) ?: run {
            jobMonitor.stats.folkeregisteridentIkkeFunnet++
            log.warn("Fant ikke folkeregisterident for sak: ${this.saksnummer}")
            null
        }

    fun finnFolkeregisteridentMedBehandlinger(): Sequence<Pair<String, Behandling>> =
        finnSakerHvorÅrsavregningSkalOpprettes().flatMap { sak ->
            sak.hentFolkeregisterident()?.let { ident ->
                sak.behandlinger.map { behandling -> ident to behandling }
            } ?: emptyList()
        }

    fun hentMelosysHendelser(): Sequence<MelosysHendelse> =
        finnFolkeregisteridentMedBehandlinger().mapNotNull { (folkeregisterident, behandling) ->
            runCatching {
                val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)

                MelosysHendelse(
                    melding = VedtakHendelseMelding(
                        folkeregisterIdent = folkeregisterident,
                        sakstype = behandling.fagsak.type,
                        sakstema = behandling.fagsak.tema,
                        behandligsresultatType = behandlingsresultat.type,
                        vedtakstype = behandlingsresultat.vedtakMetadata?.vedtakstype,
                        medlemskapsperioder = behandlingsresultat.medlemskapsperioder
                            .filter { it.fom != null && it.tom != null && it.innvilgelsesresultat == InnvilgelsesResultat.INNVILGET }
                            .map { Periode(it.fom, it.tom, it.innvilgelsesresultat) },
                        lovvalgsperioder = emptyList()
                    )
                )
            }.onFailure {
                log.error(it) { "Feil ved opprettelse av MelosysHendelse for behandling:${behandling.id}" }
                jobMonitor.registerException(it)
            }.getOrNull()
        }

    private fun finnSakerHvorÅrsavregningSkalOpprettes(): Sequence<Fagsak> = sakerForÅrsavregningRepository.finnFagsaker(
        sakStatuser = listOf(
            Saksstatuser.LOVVALG_AVKLART,
            Saksstatuser.AVSLUTTET,
            Saksstatuser.MEDLEMSKAP_AVKLART
        ),
        behandlingsStatuser = listOf(
            Behandlingsstatus.AVSLUTTET,
            Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
        ),
        ekskluderteBehandlingsresultater = listOf(
            Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL,
            Behandlingsresultattyper.FERDIGBEHANDLET
        ),
        fomDato = LocalDate.of(2023, 1, 1)
    ).also { jobMonitor.stats.dbQueryStoppedAt = LocalDateTime.now() }.asSequence()

    private fun <T> runAsSystem(prosessSteg: String = "finnSakerHvorÅrsavregningSkalOpprettes", block: () -> T): T {
        val processId = UUID.randomUUID()
        ThreadLocalAccessInfo.beforeExecuteProcess(processId, prosessSteg)
        return try {
            block()
        } finally {
            ThreadLocalAccessInfo.afterExecuteProcess(processId)
        }
    }

    fun status() = jobMonitor.status()

    inner class JobStatus(
        @Volatile var antallFunnet: Int = 0,
        @Volatile var meldingerSentAntall: Int = 0,
        @Volatile var dbQueryStoppedAt: LocalDateTime? = null,
        @Volatile var folkeregisteridentIkkeFunnet: Int = 0
    ) : JobMonitor.Stats {
        override fun reset() {
            antallFunnet = 0
            meldingerSentAntall = 0
            dbQueryStoppedAt = null
            folkeregisteridentIkkeFunnet = 0
        }

        override fun asMap(): Map<String, Any?> = mapOf(
            "dbQueryRuntime" to jobMonitor.durationUntil(dbQueryStoppedAt),
            "antallFunnet" to antallFunnet,
            "meldingerSentAntall" to meldingerSentAntall,
            "folkeregisteridentIkkeFunnet" to folkeregisteridentIkkeFunnet
        )
    }
}

interface SakerForÅrsavregningRepository : CrudRepository<Fagsak, Long> {
    @Query(
        """
        select distinct f
            FROM Behandlingsresultat br
            JOIN br.behandling b
            JOIN br.medlemskapsperioder mp
            JOIN br.vedtakMetadata vm
            JOIN b.fagsak f
        where f.type= 'FTRL'
            and f.status in :sakStatuser
            and b.status in :behandlingsStatuser
            and br.type not in :ekskluderteBehandlingsresultater
            and mp.fom >= :fomDato
        """
    )
    fun finnFagsaker(
        @Param("sakStatuser") sakStatuser: List<Saksstatuser>,
        @Param("behandlingsStatuser") behandlingsStatuser: List<Behandlingsstatus>,
        @Param("ekskluderteBehandlingsresultater") ekskluderteBehandlingsresultater: List<Behandlingsresultattyper>,
        @Param("fomDato") fomDato: LocalDate,
    ): List<Fagsak>
}
