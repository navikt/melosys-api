package no.nav.melosys.service.ftrl

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
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
import java.time.Duration
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
    private val jobStatus = JobStatus()

    @Async("taskScheduler")
    @Transactional(readOnly = true)
    fun finnSakerOgLeggPåKøAsynkront(dryrun: Boolean, antallFeilFørStopAvJob: Int = 0) {
        finnSakerOgLeggPåKø(dryrun, antallFeilFørStopAvJob)
    }

    @Synchronized
    @Transactional(readOnly = true)
    fun finnSakerOgLeggPåKø(dryrun: Boolean, antallFeilFørStopAvJob: Int = 0) =
        jobStatus.monitor(antallFeilFørStopAvJob) {
            hentMelosysHendelseer()
                .onEach { antallFunnet++ }
                .filterNot { dryrun }
                .forEach {
                    if (stop) return@monitor
                    kafkaMelosysHendelseProducer.produserBestillingsmelding(it)
                    meldingerSentAntall++
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
            jobStatus.folkeregisteridentIkkeFunnet++
            log.warn("Fant ikke folkeregisterident for sak: ${this.saksnummer}")
            null
        }

    fun finnFolkeregisteridentMedBehandlinger(): List<Pair<String, Behandling>> =
        finnSakerHvorÅrsavregningSkalOpprettes().flatMap { sak ->
            sak.hentFolkeregisterident()?.let { ident ->
                sak.behandlinger.map { behandling -> ident to behandling }
            } ?: emptyList()
        }

    fun hentMelosysHendelseer(): List<MelosysHendelse> = executeProcess {
        finnFolkeregisteridentMedBehandlinger().map { (folkeregisterident, behandling) ->
            val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)

            MelosysHendelse(
                melding = VedtakHendelseMelding(
                    folkeregisterIdent = folkeregisterident,
                    sakstype = behandling.fagsak.type,
                    sakstema = behandling.fagsak.tema,
                    behandligsresultatType = behandlingsresultat.type,
                    vedtakstype = behandlingsresultat.vedtakMetadata?.vedtakstype,
                    medlemskapsperioder = behandlingsresultat.medlemskapsperioder
                        .mapNotNull { Periode(it.fom, it.tom, it.innvilgelsesresultat) },
                    lovvalgsperioder = emptyList()
                )
            )
        }
    }

    private fun finnSakerHvorÅrsavregningSkalOpprettes(): List<Fagsak> = sakerForÅrsavregningRepository.finnFagsaker(
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
    )

    private fun <T> executeProcess(prosessSteg: String = "finnSakerHvorÅrsavregningSkalOpprettes", block: () -> T): T {
        val processId = UUID.randomUUID()
        ThreadLocalAccessInfo.beforeExecuteProcess(processId, prosessSteg)
        return try {
            block()
        } finally {
            ThreadLocalAccessInfo.afterExecuteProcess(processId)
        }
    }

    fun status() = jobStatus.status()

    class JobStatus(
        @Volatile var antallFunnet: Int = 0,
        @Volatile var meldingerSentAntall: Int = 0,
        @Volatile var startedAt: LocalDateTime = LocalDateTime.MIN,
        @Volatile var stoppedAt: LocalDateTime = LocalDateTime.MIN,
        @Volatile var stop: Boolean = false,
        @Volatile var isRunning: Boolean = false,
        @Volatile var folkeregisteridentIkkeFunnet: Int = 0,
        @Volatile var exceptions: MutableMap<String, Int> = mutableMapOf(),
        @Volatile var antallFeil: Int = 0
    ) {
        fun monitor(antallFeilFørStopAvJob: Int, block: JobStatus.() -> Unit) {
            if (isRunning) {
                log.warn("finnSakerOgLeggPåKø er allerede i gang!")
                return
            }
            isRunning = true
            stop = false
            antallFunnet = 0
            meldingerSentAntall = 0
            folkeregisteridentIkkeFunnet = 0
            startedAt = LocalDateTime.now()
            exceptions.clear()
            try {
                runCatching { this.block() }
                    .onFailure {
                        log.error(it) { "Feil ved kjøring av finnSakerOgLeggPåKø" }
                        val msg = it.message ?: it::class.simpleName ?: "Ukjent feil"
                        exceptions[msg] = (exceptions[msg] ?: 0) + 1
                        if (antallFeil++ > antallFeilFørStopAvJob) {
                            stop = true
                            log.warn("Stopper prosessering pga. for mange feil")
                        }
                    }
            } finally {
                isRunning = false
                stoppedAt = LocalDateTime.now()
                val runtime = Duration.between(startedAt, stoppedAt)
                log.info { "Antall personer funnet $antallFunnet melosys hendelser sendt: $meldingerSentAntall kjøretid: ${runtime.format()}" }
            }
        }

        fun status(): Map<String, Any> = mapOf(
            "isRunning" to isRunning,
            "startedAt" to startedAt,
            "runtime" to Duration.between(
                if (startedAt == LocalDateTime.MIN) LocalDateTime.now() else startedAt,
                if (stoppedAt == LocalDateTime.MIN) LocalDateTime.now() else stoppedAt
            ).format(),
            "antallFunnet" to antallFunnet,
            "meldingerSentAntall" to meldingerSentAntall,
            "folkeregisteridentIkkeFunnet" to folkeregisteridentIkkeFunnet,
            "antallFeil" to antallFeil,
            "exceptions" to exceptions,
        )

        fun Duration.format(): String =
            if (toMillis() < 1000) "${toMillis()} ms" else String.format("%.2f sec", toMillis() / 1000.0)
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

