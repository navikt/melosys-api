package no.nav.melosys.service.sak

import mu.KotlinLogging
import no.nav.melosys.domain.kodeverk.Saksstatuser
import no.nav.melosys.integrasjon.melosysskjema.MelosysSkjemaApiClient
import no.nav.melosys.repository.SkjemaSakMappingRepository
import no.nav.melosys.repository.SkjemaSakMappingRepository.SaksstatusSynkRad
import no.nav.melosys.saksflytapi.ProsessinstansService
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
    private val melosysSkjemaApiClient: MelosysSkjemaApiClient,
    private val prosessinstansService: ProsessinstansService
) {

    companion object {
        /** Maks antall oppdateringer per bulk-kall, avtalt med skjema-api (validering på mottakssiden). */
        const val MAKS_OPPDATERINGER_PER_BATCH = 1000

        /**
         * Mapper intern saksstatus til brukervendt skjema-api-status — REN fagsak-mapping per
         * produkteierbeslutning 2026-07-21: kun OPPRETTET vises som MOTTATT — alt annet,
         * inkludert LOVVALG_AVKLART og henlagt/annullert, vises som AVSLUTTET for innsender.
         *
         * Behandlingsnivået inngår bevisst IKKE: at en revurdering på saken ikke resetter
         * ferdigbehandlede innsendinger garanteres av mottakssiden i melosys-skjema-api
         * (monotoni: AVSLUTTET nedgraderes aldri, oppdatering per skjemaId). Se
         * plan-dokumentet «faglige-avklaringer» for art13- og gjenbrukssak-spørsmålene.
         *
         * Uttømmende when uten else med vilje: en fremtidig kodeverk-bump med ny status skal gi
         * kompileringsfeil her (bevisst mapping-beslutning), ikke stille bli AVSLUTTET.
         */
        fun tilSkjemaSaksstatus(saksstatus: Saksstatuser): Saksstatus {
            return when (saksstatus) {
                Saksstatuser.OPPRETTET -> Saksstatus.MOTTATT

                Saksstatuser.LOVVALG_AVKLART,
                Saksstatuser.MEDLEMSKAP_AVKLART,
                Saksstatuser.TRYGDEAVGIFT_AVKLART,
                Saksstatuser.AVSLUTTET,
                Saksstatuser.VIDERESENDT,
                Saksstatuser.OPPHØRT,
                Saksstatuser.HENLAGT,
                Saksstatuser.HENLAGT_BORTFALT,
                Saksstatuser.ANNULLERT -> Saksstatus.AVSLUTTET
            }
        }
    }

    /**
     * Bestiller en SYNK_SKJEMA_SAKSSTATUS-prosessinstans for saken hvis den er skjema-koblet
     * (har rader i skjema_sak_mapping) — no-op ellers. Kalles i samme transaksjon som
     * status-/behandlingsendringen (outbox-semantikk: bestillingen committes atomisk og kan ikke
     * tapes ved krasj), mens selve HTTP-kallet skjer i steget etter commit med rekjøringsstøtte.
     *
     * Brukes av [SkjemaSaksstatusEventListener] — synk trigges kun av fagsak-statusendringer,
     * siden utledet skjema-status er en ren funksjon av fagsakstatus.
     */
    fun bestillSynkHvisSkjemakoblet(saksnummer: String) {
        if (!skjemaSakMappingRepository.existsByFagsak_Saksnummer(saksnummer)) {
            return
        }
        log.info { "Bestiller synk av saksstatus til skjema-api for sak $saksnummer" }
        prosessinstansService.opprettProsessinstansSynkSkjemaSaksstatus(saksnummer)
    }

    private fun tilOppdateringer(rader: List<SaksstatusSynkRad>): List<SaksstatusOppdatering> =
        rader.map { SaksstatusOppdatering(it.skjemaId, it.saksnummer, tilSkjemaSaksstatus(it.saksstatus)) }

    /**
     * Løpende synk: sender fagsakens GJELDENDE saksstatus til skjema-api for alle skjema koblet
     * til saken. No-op for saker uten mapping-rad (saker som ikke stammer fra skjema-api).
     * Ett slankt projeksjonoppslag — hydrerer ikke fagsakens EAGER-samlinger.
     */
    fun synkroniserSaksstatusForSaksnummer(saksnummer: String) {
        val rader = skjemaSakMappingRepository.finnSaksstatusSynkRaderForSaksnummer(saksnummer)
        if (rader.isEmpty()) {
            log.debug { "Sak $saksnummer har ingen skjema-mapping — hopper over saksstatus-synk" }
            return
        }

        tilOppdateringer(rader)
            .chunked(MAKS_OPPDATERINGER_PER_BATCH)
            .forEach { melosysSkjemaApiClient.bulkOppdaterSaksstatus(BulkOppdaterSaksstatusRequest(it)) }

        log.info { "Synkroniserte saksstatus til skjema-api for ${rader.size} skjema på sak $saksnummer" }
    }

    /**
     * Massesynk av alle skjema-sak-mappinger. Med [dryRun] bygges kun rapporten — ingen kall til
     * skjema-api. Uten dry-run sendes bulk-kall i batcher à [MAKS_OPPDATERINGER_PER_BATCH].
     * Feiler en batch, stoppes videre sending og rapporten returneres med tallene så langt pluss
     * feilinformasjon. Idempotent — trygt å kjøre flere ganger (resynk ved drift/avvik).
     */
    fun massesynk(dryRun: Boolean): SkjemaSaksstatusSynkRapport {
        val rader = skjemaSakMappingRepository.finnAlleSaksstatusSynkRader()
        val oppdateringer = tilOppdateringer(rader)
        val fordeling = oppdateringer.groupingBy { it.saksstatus }.eachCount()

        val rapport = SkjemaSaksstatusSynkRapport(
            dryRun = dryRun,
            antallTotalt = oppdateringer.size,
            antallMottatt = fordeling[Saksstatus.MOTTATT] ?: 0,
            antallAvsluttet = fordeling[Saksstatus.AVSLUTTET] ?: 0,
            perMelosysStatus = rader.groupingBy { it.saksstatus }.eachCount()
        )

        if (dryRun) {
            log.info { "Dry-run av saksstatus-massesynk: ${rapport.antallTotalt} skjema (${rapport.antallMottatt} MOTTATT, ${rapport.antallAvsluttet} AVSLUTTET)" }
            return rapport
        }

        var antallOppdatert = 0
        val ukjenteSkjemaIder = mutableListOf<UUID>()
        val konfliktSkjemaIder = mutableListOf<UUID>()
        val batcher = oppdateringer.chunked(MAKS_OPPDATERINGER_PER_BATCH)
        batcher.forEachIndexed { indeks, batch ->
            try {
                val resultat = melosysSkjemaApiClient.bulkOppdaterSaksstatus(BulkOppdaterSaksstatusRequest(batch))
                antallOppdatert += resultat.antallOppdatert
                ukjenteSkjemaIder += resultat.ukjenteSkjemaIder
                konfliktSkjemaIder += resultat.konfliktSkjemaIder
            } catch (e: Exception) {
                val feiletBatch = indeks + 1
                log.error(e) { "Saksstatus-massesynk feilet i batch $feiletBatch av ${batcher.size} — stopper videre sending" }
                return rapport.copy(
                    antallOppdatert = antallOppdatert,
                    ukjenteSkjemaIder = ukjenteSkjemaIder,
                    konfliktSkjemaIder = konfliktSkjemaIder,
                    feiletBatch = feiletBatch,
                    feilmelding = "Batch $feiletBatch av ${batcher.size} feilet (${e.javaClass.simpleName}) — " +
                        "resten ble ikke sendt. Synken er idempotent og kan trygt kjøres på nytt. Se logg for detaljer."
                )
            }
        }

        log.info {
            "Saksstatus-massesynk fullført: ${rapport.antallTotalt} skjema sendt, $antallOppdatert innsendinger oppdatert, " +
                "${ukjenteSkjemaIder.size} ukjente skjemaId-er, ${konfliktSkjemaIder.size} saksnummer-konflikter"
        }
        return rapport.copy(
            antallOppdatert = antallOppdatert,
            ukjenteSkjemaIder = ukjenteSkjemaIder,
            konfliktSkjemaIder = konfliktSkjemaIder
        )
    }
}

/**
 * Rapport fra massesynk. [antallOppdatert], [ukjenteSkjemaIder] og [konfliktSkjemaIder] settes
 * kun ved reell synk (dryRun=false) og aggregeres fra skjema-api-responsene.
 * [konfliktSkjemaIder] er rader skjema-api hoppet over fordi innsendingen allerede har et annet
 * saksnummer (saksnummer er immutabelt på mottakssiden). [antallOppdatert] kan avvike fra
 * [antallTotalt] siden skjema-api oppdaterer alle innsendinger på samme saksnummer per rad —
 * radene er sortert på saksnummer nettopp for å begrense at én sak krysser en batch-grense,
 * men når det likevel skjer kan [antallOppdatert] avvike marginalt (dobbelttelling av
 * innsendinger som oppdateres i begge batchene).
 * [feiletBatch]/[feilmelding] settes hvis en batch feilet — tallene gjelder da batchene før feilen.
 */
data class SkjemaSaksstatusSynkRapport(
    val dryRun: Boolean,
    val antallTotalt: Int,
    val antallMottatt: Int,
    val antallAvsluttet: Int,
    val perMelosysStatus: Map<Saksstatuser, Int>,
    val antallOppdatert: Int? = null,
    val ukjenteSkjemaIder: List<UUID>? = null,
    val konfliktSkjemaIder: List<UUID>? = null,
    val feiletBatch: Int? = null,
    val feilmelding: String? = null
)
