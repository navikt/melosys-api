package no.nav.melosys.service.ftrl

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.integrasjon.hendelser.KafkaMelosysHendelseProducer
import no.nav.melosys.integrasjon.hendelser.MelosysHendelse
import no.nav.melosys.integrasjon.hendelser.Periode
import no.nav.melosys.integrasjon.hendelser.VedtakHendelseMelding
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.persondata.PersondataService
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

private val log = KotlinLogging.logger { }

@Component
class FinnPersonerHvorÅrsavregningSkalOpprettes(
    private val fagsakRepository: FTRLFagsakRepository,
    private val kafkaMelosysHendelseProducer: KafkaMelosysHendelseProducer,
    private val persondataService: PersondataService,
    private val behandlingsresultatService: BehandlingsresultatService,
) {

    @Async
    @Transactional(readOnly = true)
    fun KjørFinnSakerOgLeggPåKøAsynkront() {
        finnSakerOgLeggPåKø()
    }

    @Synchronized
    @Transactional(readOnly = true)
    fun finnSakerOgLeggPåKø() {
        var count = 0
        hentMelosysHendelseer().forEach {
            count++
            kafkaMelosysHendelseProducer.produserBestillingsmelding(it)
        }
        log.info { "Antall melosys hendelser meldinger sendt: $count" }
    }

    private fun Fagsak.hentFolkeregisterident(): String? =
        persondataService.finnFolkeregisterident(this.hentBrukersAktørID()).orElseGet {
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

    private fun finnSakerHvorÅrsavregningSkalOpprettes(): List<Fagsak> = fagsakRepository.finnSaksnumreForStatusOgAar(
        sakstatus = listOf(
            Saksstatuser.LOVVALG_AVKLART,
            Saksstatuser.AVSLUTTET
        ),
        behandlingsstatus = listOf(
            Behandlingsstatus.AVSLUTTET,
            Behandlingsstatus.IVERKSETTER_VEDTAK,
            Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING
        ),
        behandlingsresultattypeFilterBort = listOf(
            Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL,
            Behandlingsresultattyper.FERDIGBEHANDLET
        ),
        fom = LocalDate.of(2023, 1, 1)
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
}

interface FTRLFagsakRepository : FagsakRepository {
    @Query(
        """
        select distinct f
            FROM Behandlingsresultat br
            JOIN br.behandling b
            JOIN br.medlemskapsperioder mp
            JOIN br.vedtakMetadata vm
            JOIN b.fagsak f
        where f.type= 'FTRL'
            and f.status in :status
            and b.status in :behandlingsstatus
            and br.type not in :behandlingsresultattypeFilterBort
            and mp.fom >= :fom
        """
    )
    fun finnSaksnumreForStatusOgAar(
        @Param("status") sakstatus: List<Saksstatuser>,
        @Param("behandlingsstatus") behandlingsstatus: List<Behandlingsstatus>,
        @Param("behandlingsresultattypeFilterBort") behandlingsresultattypeFilterBort: List<Behandlingsresultattyper>,
        @Param("fom") fom: LocalDate,
    ): List<Fagsak>
}

