package no.nav.melosys.service.vedtak.etterregistrering

import mu.KotlinLogging
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.integrasjon.hendelser.KafkaMelosysHendelseProducer
import no.nav.melosys.service.JobMonitor
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.persondata.PersondataService
import no.nav.melosys.service.vedtak.VedtakHendelseMeldingBygger
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.jvm.optionals.getOrNull

private val log = KotlinLogging.logger { }

/**
 * Sender vedtaksmeldingen på nytt for behandlinger melosys-skattehendelser ikke har registrert.
 * Meldingen bygges av [VedtakHendelseMeldingBygger], som steget SendMeldingOmVedtak også bruker.
 * Med `skarp = false` slås identen opp og meldingen bygges, men ingenting sendes.
 *
 * Hver behandling leses i sin egen lesetransaksjon, så en kjøring over tusenvis av behandlinger
 * ikke holder alle i minnet, og meldingen sendes etter at transaksjonen er lukket.
 */
@Component
class EtterregistreringAvVedtaksmeldinger(
    private val utvalg: EtterregistreringUtvalg,
    private val behandlingService: BehandlingService,
    private val persondataService: PersondataService,
    private val vedtakHendelseMeldingBygger: VedtakHendelseMeldingBygger,
    private val kafkaMelosysHendelseProducer: KafkaMelosysHendelseProducer,
    transactionManager: PlatformTransactionManager,
) {
    private val lesetransaksjon = TransactionTemplate(transactionManager).apply { isReadOnly = true }

    private val jobMonitor = JobMonitor(
        jobName = "EtterregistreringAvVedtaksmeldinger",
        stats = JobStatus()
    )

    private val rapport = Rapport()

    private val reservert = AtomicBoolean(false)

    /**
     * Kontrolleren reserverer før kjøringen legges i kø. `taskExecutor` har én tråd og ubegrenset kø,
     * så uten reservasjonen ville et nytt kall mens den første venter, fått 200 og kjørt etterpå.
     */
    fun reserver(): Boolean = reservert.compareAndSet(false, true)

    fun frigi() = reservert.set(false)

    @Async("taskExecutor")
    fun kjørAsynkront(parametre: EtterregistreringParametre) {
        kjør(parametre)
    }

    fun kjør(parametre: EtterregistreringParametre) = try {
        kjørSomSystem(parametre)
    } finally {
        frigi()
    }

    private fun kjørSomSystem(parametre: EtterregistreringParametre) = runAsSystem {
        jobMonitor.execute(maxErrorsBeforeStop = parametre.maksFeil) {
            this.parametre = parametre
            rapport.nullstill(parametre)

            val behandlinger = try {
                utvalg.finnBehandlinger(parametre.periodeTomFra).filter(parametre::omfatter)
            } catch (e: Exception) {
                log.error(e) { "Utvalget for etterregistrering feilet, ingen meldinger er sendt" }
                feilVedUtvalg = e.javaClass.simpleName
                jobMonitor.registerException(FeilUtenPersonopplysninger(e))
                return@execute
            }
            antallIUtvalget = behandlinger.size
            rapport.registrerUtvalg(behandlinger)
            log.info { "Starter etterregistrering for ${behandlinger.size} behandlinger, $parametre" }

            val identPerSak = HashMap<String, String?>()
            for (behandling in behandlinger) {
                if (parametre.maksAntall != null && antallSendt >= parametre.maksAntall) {
                    avbruttAarsak = "maksAntall=${parametre.maksAntall} er nådd"
                    break
                }
                behandle(behandling, parametre.skarp, identPerSak)
                antallBehandlet++
                if (jobMonitor.shouldStop) {
                    avbruttAarsak = "Stoppet etter for mange feil (maksFeil=${parametre.maksFeil})"
                    break
                }
            }
        }
    }

    private fun JobStatus.behandle(behandling: UtvalgtBehandling, skarp: Boolean, identPerSak: MutableMap<String, String?>) {
        try {
            val hendelse = lesetransaksjon.execute {
                val lagretBehandling = behandlingService.hentBehandling(behandling.behandlingId)
                val folkeregisterident = if (behandling.saksnummer in identPerSak) {
                    identPerSak[behandling.saksnummer]
                } else {
                    lagretBehandling.fagsak.finnFolkeregisterident().also { identPerSak[behandling.saksnummer] = it }
                }
                folkeregisterident?.let { vedtakHendelseMeldingBygger.bygg(lagretBehandling, it) }
            }
            if (hendelse == null) {
                antallUtenIdent++
                rapport.registrerUtenIdent(behandling)
                return
            }
            if (skarp) {
                kafkaMelosysHendelseProducer.produserBestillingsmelding(hendelse)
            }
            antallSendt++
            rapport.registrerSendt(behandling)
        } catch (e: Exception) {
            log.error(e) { "Etterregistrering feilet for behandling ${behandling.behandlingId} i sak ${behandling.saksnummer}" }
            antallFeilet++
            rapport.registrerFeil(behandling, e)
            jobMonitor.registerException(FeilUtenPersonopplysninger(e))
        }
    }

    private fun Fagsak.finnFolkeregisterident(): String? =
        try {
            persondataService.finnFolkeregisterident(hentBrukersAktørID()).getOrNull()
        } catch (_: IkkeFunnetException) {
            null
        }

    private fun <T> runAsSystem(block: () -> T): T {
        val processId = UUID.randomUUID()
        ThreadLocalAccessInfo.beforeExecuteProcess(processId, "etterregistreringAvVedtaksmeldinger")
        return try {
            block()
        } finally {
            ThreadLocalAccessInfo.afterExecuteProcess(processId)
        }
    }

    /** Status ligger i minnet på poden som fikk kallet; `pod` viser hvilken. */
    fun status(): Map<String, Any?> = jobMonitor.status() + mapOf("pod" to (System.getenv("HOSTNAME") ?: "ukjent"))

    fun rapport(): EtterregistreringRapport = rapport.øyeblikksbilde()

    /** Status og rapport viser feiltypen, ikke meldingen, fordi meldingen kan inneholde personopplysninger. */
    private class FeilUtenPersonopplysninger(feil: Exception) : RuntimeException(feil.javaClass.simpleName)

    inner class JobStatus(
        @Volatile var parametre: EtterregistreringParametre? = null,
        @Volatile var feilVedUtvalg: String? = null,
        @Volatile var antallIUtvalget: Int = 0,
        @Volatile var antallBehandlet: Int = 0,
        /** I simulering: meldinger som ville blitt sendt. */
        @Volatile var antallSendt: Int = 0,
        @Volatile var antallUtenIdent: Int = 0,
        @Volatile var antallFeilet: Int = 0,
        /** Satt når kjøringen stoppet før utvalget var gjennomgått. Null betyr fullført. */
        @Volatile var avbruttAarsak: String? = null,
    ) : JobMonitor.Stats {
        override fun reset() {
            parametre = null
            feilVedUtvalg = null
            antallIUtvalget = 0
            antallBehandlet = 0
            antallSendt = 0
            antallUtenIdent = 0
            antallFeilet = 0
            avbruttAarsak = null
        }

        override fun asMap(): Map<String, Any?> = mapOf(
            "skarp" to parametre?.skarp,
            "periodeTomFra" to parametre?.periodeTomFra?.toString(),
            "vedtakFra" to parametre?.vedtakFra?.toString(),
            "vedtakTil" to parametre?.vedtakTil?.toString(),
            "maksAntall" to parametre?.maksAntall,
            "maksFeil" to parametre?.maksFeil,
            "feilVedUtvalg" to feilVedUtvalg,
            "antallIUtvalget" to antallIUtvalget,
            "antallBehandlet" to antallBehandlet,
            "antallSendt" to antallSendt,
            "antallUtenIdent" to antallUtenIdent,
            "antallFeilet" to antallFeilet,
            "avbruttAarsak" to avbruttAarsak,
        )
    }

    /** Skrives fra jobbtråden mens /rapport kan lese samtidig, derfor synkronisert. */
    private class Rapport {
        private var parametre: EtterregistreringParametre? = null
        private val perVedtaksmåned = LinkedHashMap<String, Månedstall>()
        private val sendteBehandlingIder = mutableListOf<Long>()
        private val feil = mutableListOf<EtterregistreringFeil>()

        @Synchronized
        fun nullstill(parametre: EtterregistreringParametre) {
            this.parametre = parametre
            perVedtaksmåned.clear()
            sendteBehandlingIder.clear()
            feil.clear()
        }

        @Synchronized
        fun registrerUtvalg(behandlinger: List<UtvalgtBehandling>) =
            behandlinger.forEach { måned(it).iUtvalget++ }

        @Synchronized
        fun registrerSendt(behandling: UtvalgtBehandling) {
            måned(behandling).sendt++
            sendteBehandlingIder += behandling.behandlingId
        }

        @Synchronized
        fun registrerUtenIdent(behandling: UtvalgtBehandling) {
            måned(behandling).utenIdent++
        }

        @Synchronized
        fun registrerFeil(behandling: UtvalgtBehandling, e: Exception) {
            måned(behandling).feilet++
            feil += EtterregistreringFeil(behandling.behandlingId, behandling.saksnummer, e.javaClass.simpleName)
        }

        @Synchronized
        fun øyeblikksbilde() = EtterregistreringRapport(
            skarp = parametre?.skarp,
            perVedtaksmaaned = perVedtaksmåned.values.map { it.tilRad() },
            sendteBehandlingIder = sendteBehandlingIder.toList(),
            feil = feil.toList(),
        )

        private fun måned(behandling: UtvalgtBehandling) =
            perVedtaksmåned.getOrPut(behandling.vedtaksmåned()) { Månedstall(behandling.vedtaksmåned()) }

        private fun UtvalgtBehandling.vedtaksmåned() = vedtaksdato?.toString()?.substring(0, 7) ?: UTEN_DATO

        private class Månedstall(val måned: String, var iUtvalget: Int = 0, var sendt: Int = 0, var utenIdent: Int = 0, var feilet: Int = 0) {
            fun tilRad() = VedtaksmånedRad(måned, iUtvalget, sendt, utenIdent, feilet)
        }
    }

    companion object {
        const val UTEN_DATO = "uten dato"
    }
}

data class EtterregistreringParametre(
    val periodeTomFra: LocalDate,
    val vedtakFra: LocalDate? = null,
    val vedtakTil: LocalDate? = null,
    val behandlingIder: Set<Long>? = null,
    val maksAntall: Int? = null,
    val maksFeil: Int,
    val skarp: Boolean,
) {
    /** Med vedtakFra eller vedtakTil er behandlinger uten dato utenfor. */
    fun omfatter(behandling: UtvalgtBehandling): Boolean {
        if (behandlingIder != null && behandling.behandlingId !in behandlingIder) return false
        if (vedtakFra == null && vedtakTil == null) return true
        val dato = behandling.vedtaksdato ?: return false
        return (vedtakFra == null || !dato.isBefore(vedtakFra)) && (vedtakTil == null || !dato.isAfter(vedtakTil))
    }
}

data class EtterregistreringRapport(
    val skarp: Boolean?,
    /** Sortert på vedtaksmåned, med behandlinger uten dato til slutt. */
    val perVedtaksmaaned: List<VedtaksmånedRad>,
    /** I simulering: behandlingene som ville blitt sendt. */
    val sendteBehandlingIder: List<Long>,
    val feil: List<EtterregistreringFeil>,
)

data class VedtaksmånedRad(val vedtaksmaaned: String, val iUtvalget: Int, val sendt: Int, val utenIdent: Int, val feilet: Int)

data class EtterregistreringFeil(val behandlingId: Long, val saksnummer: String, val feiltype: String)
