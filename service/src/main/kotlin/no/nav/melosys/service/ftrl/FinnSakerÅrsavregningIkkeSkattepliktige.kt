package no.nav.melosys.service.ftrl

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.service.JobMonitor
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

private val log = KotlinLogging.logger { }

@Component
class FinnSakerÅrsavregningIkkeSkattepliktige(
    private val sakerForÅrsavregningRepository: SakerÅrsavregningIkkeSkattepliktigeRepository
) {
    private val jobMonitor = JobMonitor(
        jobName = "FinnSakerForÅrsavregningIkkeSkattepliktige",
        stats = JobStatus()
    )

    @Async("taskExecutor")
    @Transactional(readOnly = true)
    fun finnSakerAsynkront(dryrun: Boolean, antallFeilFørStopAvJob: Int, saksnummer: String?) {
        require(antallFeilFørStopAvJob >= 0) { "antallFeilFørStopAvJob må være positiv" }
        finnSaker(dryrun, antallFeilFørStopAvJob)
    }

    @Synchronized
    @Transactional(readOnly = true)
    fun finnSaker(dryrun: Boolean, antallFeilFørStopAvJob: Int = 0) = runAsSystem {
        jobMonitor.execute(antallFeilFørStopAvJob) {
            finnSakerHvorÅrsavregningSkalOpprettes()
                .onEach { antallFunnet++ }
                .filterNot { dryrun }
                .forEach {
                    if (jobMonitor.shouldStop) return@execute

                    println("sak: ${it.fagsak.saksnummer}, behandling: ${it.id} ")
                    antallProsessert++
                }
        }
    }

    private fun finnSakerHvorÅrsavregningSkalOpprettes(): List<Behandling> =
        sakerForÅrsavregningRepository.finnFTRLBehandling(
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
                Behandlingsresultattyper.FASTSATT_TRYGDEAVGIFT
            ),
            fomDato = LocalDate.of(2024, 1, 1) // Fra til
        ).sortedBy { it.fagsak.saksnummer }
            .also { jobMonitor.stats.dbQueryStoppedAt = LocalDateTime.now() }

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
        @Volatile var antallProsessert: Int = 0,
        @Volatile var dbQueryStoppedAt: LocalDateTime? = null,
    ) : JobMonitor.Stats {
        override fun reset() {
            antallFunnet = 0
            antallProsessert = 0
            dbQueryStoppedAt = null
        }

        override fun asMap(): Map<String, Any?> = mapOf(
            "dbQueryRuntime" to jobMonitor.durationUntil(dbQueryStoppedAt),
            "antallFunnet" to antallFunnet,
            "antallProsessert" to antallProsessert,
        )
    }
}

interface SakerÅrsavregningIkkeSkattepliktigeRepository : CrudRepository<Behandling, Long> {
    /**
     * Finner FTRL-saker (Foreign Tax Relief Liability) som krever årsavregning for ikke-skattepliktige personer.
     *
     * Forretningslogikk:
     * - Identifiserer saker hvor personer er klassifisert som ikke-skattepliktige til Norge
     * - Inkluderer kun saker med endelige vedtak (fullførte behandlinger og saksstatuser)
     * - Ekskluderer saker hvor skattefastsetting allerede er bestemt
     * - Filtrerer på medlemsperioder som starter fra 2024 og fremover
     *
     * Spørring forklaring:
     * Spørringen utfører flere JOINs for å traversere domenemodellen:
     * 1. Behandlingsresultat -> Behandling: Kobler behandlingsresultater til deres behandlinger
     * 2. Behandlingsresultat -> Medlemskapsperioder: Henter medlemsperioder fra behandlingsresultater
     * 3. Behandlingsresultat -> VedtakMetadata: Sikrer at det finnes vedtaksmetadata
     * 4. Behandling -> Fagsak: Kobler behandlinger tilbake til deres saker
     * 5. Medlemskapsperioder -> Trygdeavgiftsperioder: Henter trygdeavgiftsperioder
     * 6. Trygdeavgiftsperioder -> GrunnlagSkatteforholdTilNorge: Henter grunnlag for skatteforhold til Norge
     *
     * Filtreringsvilkår:
     * - f.type = 'FTRL': Kun Foreign Tax Relief Liability-saker
     * - f.status in sakStatuser: Saker med spesifikke avsluttede statuser
     * - b.status in behandlingsStatuser: Behandlinger som er fullført eller har midlertidige vedtak
     * - br.type not in ekskluderteBehandlingsresultater: Ekskluderer saker som allerede er skattefastsatt
     * - mp.fom >= fomDato: Kun medlemsperioder fra spesifisert dato og fremover
     * - stn.skatteplikttype = 'IKKE_SKATTEPLIKTIG': Kun ikke-skattepliktige personer
     *
     * @param sakStatuser Liste over saksstatuser å inkludere (typisk avsluttede statuser)
     * @param behandlingsStatuser Liste over behandlingsstatuser å inkludere (typisk fullførte)
     * @param ekskluderteBehandlingsresultater Behandlingsresultattyper å ekskludere (f.eks. allerede skattefastsatt)
     * @param fomDato Startdato for medlemsperioder som skal vurderes
     * @return Liste over distinkte FTRL-saker som krever årsavregning
     */
    // sorter og se om dette finnes flere behandlinger, og da må det siste
    @Query(
        """
        select distinct b
            FROM Behandlingsresultat br
            JOIN br.behandling b
            JOIN br.medlemskapsperioder mp
            JOIN br.vedtakMetadata vm
            JOIN b.fagsak f
            JOIN mp.trygdeavgiftsperioder tap
            JOIN tap.grunnlagSkatteforholdTilNorge stn
        where f.type = 'FTRL'
            and f.status in :sakStatuser
            and b.status in :behandlingsStatuser
            and br.type not in :ekskluderteBehandlingsresultater
            and mp.fom >= :fomDato
            and stn.skatteplikttype = 'IKKE_SKATTEPLIKTIG'
        """
    )
    fun finnFTRLBehandling(
        @Param("sakStatuser") sakStatuser: List<Saksstatuser>,
        @Param("behandlingsStatuser") behandlingsStatuser: List<Behandlingsstatus>,
        @Param("ekskluderteBehandlingsresultater") ekskluderteBehandlingsresultater: List<Behandlingsresultattyper>,
        @Param("fomDato") fomDato: LocalDate,
    ): List<Behandling>
}
