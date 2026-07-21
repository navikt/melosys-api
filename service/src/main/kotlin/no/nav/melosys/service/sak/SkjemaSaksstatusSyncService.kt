package no.nav.melosys.service.sak

import mu.KotlinLogging
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.integrasjon.melosysskjema.MelosysSkjemaApiClient
import no.nav.melosys.repository.SkjemaSakMappingRepository
import no.nav.melosys.skjema.types.common.Saksstatus
import no.nav.melosys.skjema.types.m2m.BulkOppdaterSaksstatusRequest
import no.nav.melosys.skjema.types.m2m.SaksstatusOppdatering
import org.springframework.stereotype.Service
import java.util.UUID

private val log = KotlinLogging.logger { }

/**
 * Synkroniserer brukervendt saksstatus (MOTTATT/AVSLUTTET) til melosys-skjema-api for saker som
 * stammer fra skjema-api (dvs. har rader i skjema_sak_mapping).
 *
 * Statusmappingen ligger kun her, slik at løpende synk (event-listener) og massesynk
 * (admin-endepunkt) alltid er konsistente. Saksnummer sendes alltid med, slik at innsendinger i
 * skjema-api som mangler saksnummer backfilles samtidig. Mottakssiden er idempotent.
 */
@Service
class SkjemaSaksstatusSyncService(
    private val skjemaSakMappingRepository: SkjemaSakMappingRepository,
    private val melosysSkjemaApiClient: MelosysSkjemaApiClient
) {

    companion object {
        /** Maks antall oppdateringer per bulk-kall, avtalt med skjema-api (validering på mottakssiden). */
        const val MAKS_OPPDATERINGER_PER_BATCH = 1000

        /**
         * Mapper intern saksstatus til brukervendt skjema-api-status.
         * Besluttet av produkteier: kun OPPRETTET vises som MOTTATT — alt annet, inkludert
         * LOVVALG_AVKLART og henlagt/annullert, vises som AVSLUTTET for innsender.
         */
        fun tilSkjemaSaksstatus(saksstatus: Saksstatuser): Saksstatus = when (saksstatus) {
            Saksstatuser.OPPRETTET -> Saksstatus.MOTTATT
            else -> Saksstatus.AVSLUTTET
        }
    }

    /**
     * Løpende synk: sender oppdatert saksstatus til skjema-api hvis fagsaken har skjema-mapping.
     * Gjør ingenting for saker uten mapping-rad (saker som ikke stammer fra skjema-api).
     */
    fun synkroniserSaksstatusForFagsak(fagsak: Fagsak) {
        val mappinger = skjemaSakMappingRepository.findByFagsak_Saksnummer(fagsak.saksnummer)
        if (mappinger.isEmpty()) {
            return
        }

        val saksstatus = tilSkjemaSaksstatus(fagsak.status)
        if (mappinger.size == 1) {
            melosysSkjemaApiClient.oppdaterSaksstatus(mappinger.first().skjemaId, fagsak.saksnummer, saksstatus)
        } else {
            melosysSkjemaApiClient.bulkOppdaterSaksstatus(
                BulkOppdaterSaksstatusRequest(
                    mappinger.map { SaksstatusOppdatering(it.skjemaId, fagsak.saksnummer, saksstatus) }
                )
            )
        }
        log.info { "Synkroniserte saksstatus $saksstatus til skjema-api for ${mappinger.size} skjema på sak ${fagsak.saksnummer}" }
    }

    /**
     * Massesynk av alle skjema-sak-mappinger. Med [dryRun] bygges kun rapporten — ingen kall til
     * skjema-api. Uten dry-run sendes bulk-kall i batcher à [MAKS_OPPDATERINGER_PER_BATCH].
     * Idempotent — trygt å kjøre flere ganger (resynk ved drift/avvik og tapte events).
     */
    fun massesynk(dryRun: Boolean): SkjemaSaksstatusSynkRapport {
        val mappinger = skjemaSakMappingRepository.findAllMedFagsak()
        val oppdateringer = mappinger.map {
            SaksstatusOppdatering(it.skjemaId, it.saksnummer, tilSkjemaSaksstatus(it.fagsak.status))
        }

        val rapport = SkjemaSaksstatusSynkRapport(
            dryRun = dryRun,
            antallTotalt = oppdateringer.size,
            antallMottatt = oppdateringer.count { it.saksstatus == Saksstatus.MOTTATT },
            antallAvsluttet = oppdateringer.count { it.saksstatus == Saksstatus.AVSLUTTET },
            perMelosysStatus = mappinger.groupingBy { it.fagsak.status }.eachCount()
        )

        if (dryRun) {
            log.info { "Dry-run av saksstatus-massesynk: ${rapport.antallTotalt} skjema (${rapport.antallMottatt} MOTTATT, ${rapport.antallAvsluttet} AVSLUTTET)" }
            return rapport
        }

        var antallOppdatert = 0
        val ukjenteSkjemaIder = mutableListOf<UUID>()
        oppdateringer.chunked(MAKS_OPPDATERINGER_PER_BATCH).forEach { batch ->
            val resultat = melosysSkjemaApiClient.bulkOppdaterSaksstatus(BulkOppdaterSaksstatusRequest(batch))
            antallOppdatert += resultat.antallOppdatert
            ukjenteSkjemaIder += resultat.ukjenteSkjemaIder
        }

        log.info {
            "Saksstatus-massesynk fullført: ${rapport.antallTotalt} skjema sendt, $antallOppdatert innsendinger oppdatert, " +
                "${ukjenteSkjemaIder.size} ukjente skjemaId-er"
        }
        return rapport.copy(antallOppdatert = antallOppdatert, ukjenteSkjemaIder = ukjenteSkjemaIder)
    }
}

/**
 * Rapport fra massesynk. [antallOppdatert] og [ukjenteSkjemaIder] settes kun ved reell synk
 * (dryRun=false) og aggregeres fra skjema-api-responsene. [antallOppdatert] kan avvike fra
 * [antallTotalt] siden skjema-api oppdaterer alle innsendinger på samme saksnummer per rad.
 */
data class SkjemaSaksstatusSynkRapport(
    val dryRun: Boolean,
    val antallTotalt: Int,
    val antallMottatt: Int,
    val antallAvsluttet: Int,
    val perMelosysStatus: Map<Saksstatuser, Int>,
    val antallOppdatert: Int? = null,
    val ukjenteSkjemaIder: List<UUID>? = null
)
