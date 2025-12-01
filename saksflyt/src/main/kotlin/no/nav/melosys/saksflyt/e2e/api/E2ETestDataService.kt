package no.nav.melosys.saksflyt.e2e.api

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import mu.KotlinLogging
import no.nav.melosys.domain.Aktoer
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.repository.FagsakRepository
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.oppgave.OppgaveService
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.util.*

private val log = KotlinLogging.logger { }

/**
 * Service for å opprette forhåndsdefinerte test-saker i Oracle-databasen.
 * Brukes av frontend Playwright e2e-tester via E2ESupportController.
 *
 * VIKTIG: Disse sakene brukes av frontend Playwright e2e-tester (testdataUtils.ts).
 * Hver e2e-test får sin egen dedikerte sak for full isolasjon.
 * Endringer her må synkroniseres med frontend testdataUtils.ts!
 *
 * Saksoversikt:
 * - MEL-1001 til MEL-1011: Avtaleland UNDER_BEHANDLING (11 saker)
 * - MEL-1012: Avtaleland OPPRETTET (1 sak)
 * - MEL-1013 til MEL-1022: Utenfor avtaleland UNDER_BEHANDLING (10 saker)
 * - MEL-1023 til MEL-1050: Utenfor avtaleland med årsavregning (28 saker)
 * - MEL-1051 til MEL-1053: EU/EØS IKKE_YRKESAKTIV UNDER_BEHANDLING (3 saker)
 * - MEL-1054 til MEL-1056: EU/EØS Pensjonist med trygdeavgift (3 saker)
 * - MEL-1057 til MEL-1062: OPPRETTET saker for "knytt til eksisterende" tester (6 saker)
 * - MEL-1063 til MEL-1068: AVSLUTTET saker for "knytt til eksisterende" tester (6 saker)
 * - MEL-1069: Avtaleland for varselmelding-test (1 sak)
 * - MEL-1070: Utenfor avtaleland AVSLUTTET (1 sak)
 * - MEL-1071: EU/EØS IKKE_YRKESAKTIV for regresjon-test (1 sak)
 */
@Service
@Profile("local-mock")
class E2ETestDataService(
    private val fagsakRepository: FagsakRepository,
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    @Value("\${OppgaveAPI_v1.url:http://localhost:8083/api/v1}") private val oppgaveUrl: String
) {
    @PersistenceContext
    private lateinit var entityManager: EntityManager

    private val oppgaveWebClient: WebClient by lazy {
        WebClient.builder().baseUrl(oppgaveUrl).build()
    }

    /**
     * Resetter testdata: sletter eksisterende og oppretter på nytt.
     * Returnerer full metadata inkludert behandlingID for alle saker.
     */
    @Transactional
    fun resetTestData(): ResetResult {
        log.info { "Resetter test-saker ($FIRST_CASE_ID til $LAST_CASE_ID)..." }

        val deleted = clearTestData()
        val initResult = initializeTestData()

        // Hent full metadata for alle saker
        val metadata = hentFullMetadata()

        log.info { "Reset fullført: slettet $deleted, opprettet ${initResult.created}" }

        return ResetResult(
            cleared = deleted,
            created = initResult.created,
            wasReset = deleted > 0,
            metadata = metadata
        )
    }

    /**
     * Sletter alle test-saker (MEL-1001 til MEL-1071) med tilhørende data.
     * Bruker native SQL for å håndtere komplekse relasjoner.
     *
     * Tabellstruktur (verifisert mot migration-filer):
     * - fagsak: primærnøkkel = saksnummer (VARCHAR)
     * - behandling: primærnøkkel = id, foreign key saksnummer -> fagsak
     * - behandlingsresultat: primærnøkkel = behandling_id (1:1 relasjon)
     * - child-tabeller av behandlingsresultat bruker beh_resultat_id ELLER behandlingsresultat_id
     */
    @Transactional
    fun clearTestData(): Int {
        val saksnummerListe = (1001..1071).map { "MEL-$it" }
        val saksnummerIn = saksnummerListe.joinToString(",") { "'$it'" }

        // Slett oppgaver fra oppgave-mock først (eksterne oppgaver)
        slettAlleOppgaverFraMock()

        // Finn behandling-IDer for test-sakene
        @Suppress("UNCHECKED_CAST")
        val behandlingIds = entityManager.createNativeQuery(
            "SELECT id FROM behandling WHERE saksnummer IN ($saksnummerIn)"
        ).resultList as List<Number>

        if (behandlingIds.isEmpty()) {
            log.info { "Ingen test-saker å slette" }
            return 0
        }

        val behandlingIdIn = behandlingIds.joinToString(",") { it.toString() }

        log.info { "Sletter ${behandlingIds.size} behandlinger med tilhørende data..." }

        // Slett i riktig rekkefølge (fra barn til foreldre)
        // behandlingsresultat.behandling_id ER primærnøkkelen, så beh_resultat_id = behandling_id
        //
        // VIKTIG: Rekkefølgen er kritisk pga. foreign key-constraints!
        // Verifisert mot alle V*.sql migrations i database/src/main/resources/db/migration/
        val deleteStatements = listOf(
            // Nivå 1: Dypeste barn (trygdeavgiftsperiode har FK til flere tabeller)
            // V62 + V137 + V142: FK til helseutgift_dekkes_periode, lovvalg_periode, medlemskapsperiode
            "DELETE FROM trygdeavgiftsperiode WHERE medlemskapsperiode_id IN " +
                "(SELECT id FROM medlemskapsperiode WHERE behandlingsresultat_id IN ($behandlingIdIn))",
            "DELETE FROM trygdeavgiftsperiode WHERE helseutgift_dekkes_periode_id IN " +
                "(SELECT id FROM helseutgift_dekkes_periode WHERE beh_resultat_id IN ($behandlingIdIn))",
            "DELETE FROM trygdeavgiftsperiode WHERE lovvalg_periode_id IN " +
                "(SELECT id FROM lovvalg_periode WHERE beh_resultat_id IN ($behandlingIdIn))",

            // Nivå 2: medlemskapsperiode (V109 la til BEHANDLINGSRESULTAT_ID)
            "DELETE FROM medlemskapsperiode WHERE behandlingsresultat_id IN ($behandlingIdIn)",

            // Nivå 3: Child-tabeller av behandlingsresultat med kolonnenavn "beh_resultat_id"
            // (V5.1_11, V4.3_02, V5.1_13, V1.0_08, V1.0_07, V1.0_09, V4.2_03, V132)
            "DELETE FROM utpekingsperiode WHERE beh_resultat_id IN ($behandlingIdIn)",
            "DELETE FROM anmodningsperiode WHERE beh_resultat_id IN ($behandlingIdIn)",
            "DELETE FROM kontrollresultat WHERE beh_resultat_id IN ($behandlingIdIn)",
            "DELETE FROM avklartefakta WHERE beh_resultat_id IN ($behandlingIdIn)",
            "DELETE FROM lovvalg_periode WHERE beh_resultat_id IN ($behandlingIdIn)",
            "DELETE FROM vilkaarsresultat WHERE beh_resultat_id IN ($behandlingIdIn)",
            "DELETE FROM behandlingsres_begrunnelse WHERE beh_resultat_id IN ($behandlingIdIn)",
            "DELETE FROM helseutgift_dekkes_periode WHERE beh_resultat_id IN ($behandlingIdIn)",

            // Nivå 3: Child-tabeller av behandlingsresultat med kolonnenavn "behandlingsresultat_id"
            // (V104, V5.1_05)
            "DELETE FROM aarsavregning WHERE behandlingsresultat_id IN ($behandlingIdIn)",
            "DELETE FROM vedtak_metadata WHERE behandlingsresultat_id IN ($behandlingIdIn)",

            // Nivå 4: Behandlingsresultat selv (V1.0_06) - primærnøkkel er behandling_id
            "DELETE FROM behandlingsresultat WHERE behandling_id IN ($behandlingIdIn)",

            // Nivå 5: saksopplysning_kilde har FK til saksopplysning (V7.1_06)
            "DELETE FROM saksopplysning_kilde WHERE saksopplysning_id IN " +
                "(SELECT id FROM saksopplysning WHERE behandling_id IN ($behandlingIdIn))",

            // Nivå 6: Tabeller med direkte foreign key til behandling.id
            // (V1.0_05, V1.0_12, V38)
            // NB: behandling_historikk ble droppet i V37
            // NB: BEHANDLINGSGRUNNLAG ble omdøpt til MOTTATTEOPPLYSNINGER i V36
            "DELETE FROM saksopplysning WHERE behandling_id IN ($behandlingIdIn)",
            "DELETE FROM tidligere_medlemsperiode WHERE behandling_id IN ($behandlingIdIn)",
            "DELETE FROM mottatteopplysninger WHERE behandling_id IN ($behandlingIdIn)",
            "DELETE FROM behandlingsaarsak WHERE behandling_id IN ($behandlingIdIn)",

            // Nivå 7: prosessinstans_hendelser -> prosessinstans -> behandling (V3.0_01)
            // NB: FK fra prosessinstans_hendelser til prosessinstans ble droppet i V4.5_01
            "DELETE FROM prosessinstans_hendelser WHERE prosessinstans_id IN " +
                "(SELECT uuid FROM prosessinstans WHERE behandling_id IN ($behandlingIdIn))",
            "DELETE FROM prosessinstans WHERE behandling_id IN ($behandlingIdIn)",

            // Nivå 8: Behandling (V1.0_03)
            "DELETE FROM behandling WHERE id IN ($behandlingIdIn)",

            // Nivå 9: Aktør (V1.0_02) - foreign key til fagsak.saksnummer
            "DELETE FROM aktoer WHERE saksnummer IN ($saksnummerIn)",

            // Nivå 10: Fagsak (V1.0_01)
            "DELETE FROM fagsak WHERE saksnummer IN ($saksnummerIn)"
        )

        var totalDeleted = 0
        deleteStatements.forEach { sql ->
            try {
                val deleted = entityManager.createNativeQuery(sql).executeUpdate()
                if (deleted > 0) {
                    log.debug { "Slettet $deleted rader: ${sql.take(60)}..." }
                    totalDeleted += deleted
                }
            } catch (e: Exception) {
                // Ignorer "table does not exist" feil (ORA-00942) - tabellen kan ha blitt droppet
                val message = e.cause?.cause?.message ?: e.message ?: ""
                if (message.contains("ORA-00942")) {
                    log.debug { "Tabell eksisterer ikke (ignorerer): ${sql.take(40)}..." }
                } else {
                    throw e
                }
            }
        }

        // Tøm caches etter sletting
        entityManager.flush()
        entityManager.clear()

        log.info { "Slettet totalt $totalDeleted rader" }
        return behandlingIds.size
    }

    /**
     * Sletter oppgaver fra oppgave-mock-tjenesten for gitte saksnummer.
     */
    private fun slettAlleOppgaverFraMock() {
        try {
            val response = oppgaveWebClient.delete()
                .uri("/oppgaver")
                .retrieve()
                .bodyToMono(Map::class.java)
                .block()

            log.info { "Slettet alle oppgaver fra mock: $response" }
        } catch (e: Exception) {
            log.warn(e) { "Kunne ikke slette alle oppgaver fra mock (ignorerer): ${e.message}" }
        }
    }

    /**
     * Henter full metadata for alle test-saker.
     */
    fun hentFullMetadata(): Map<String, SakMetadata> {
        val saksnummerListe = (1001..1071).map { "MEL-$it" }
        val saksnummerIn = saksnummerListe.joinToString(",") { "'$it'" }

        @Suppress("UNCHECKED_CAST")
        val results = entityManager.createNativeQuery(
            """
            SELECT b.saksnummer, b.id, f.fagsak_type, f.tema, b.beh_tema, b.beh_type, b.status
            FROM behandling b
            JOIN fagsak f ON b.saksnummer = f.saksnummer
            WHERE b.saksnummer IN ($saksnummerIn)
            ORDER BY b.saksnummer
            """.trimIndent()
        ).resultList as List<Array<Any>>

        return results.associate { row ->
            val saksnummer = row[0] as String
            val metadata = SakMetadata(
                behandlingID = (row[1] as Number).toLong(),
                sakstype = row[2] as String,
                sakstema = row[3] as String,
                behandlingstema = row[4] as String,
                behandlingstype = row[5] as String,
                behandlingsstatus = row[6] as String
            )
            saksnummer to metadata
        }
    }

    /**
     * Initialiserer alle test-saker. Idempotent - hopper over saker som allerede eksisterer.
     * @return Antall saker som ble opprettet (0 hvis alle allerede eksisterte)
     */
    @Transactional
    fun initializeTestData(): InitResult {
        // Sjekk om testdata allerede eksisterer (idempotent)
        if (fagsakRepository.existsById(LAST_CASE_ID)) {
            log.info { "Test-saker eksisterer allerede - hopper over initialisering" }
            return InitResult(created = 0, skipped = TOTAL_CASES, alreadyExisted = true)
        }

        log.info { "Initialiserer test-saker ($FIRST_CASE_ID til $LAST_CASE_ID)..." }

        val processId = UUID.randomUUID()
        var created = 0

        try {
            ThreadLocalAccessInfo.beforeExecuteProcess(processId, "E2ETestDataService", TEST_SAKSBEHANDLER, "Test Saksbehandler")

            // MEL-1001 til MEL-1011: Avtaleland UNDER_BEHANDLING
            (1001..1011).forEach {
                if (opprettSak("MEL-$it", Sakstyper.TRYGDEAVTALE, Sakstemaer.MEDLEMSKAP_LOVVALG)) created++
            }

            // MEL-1012: Avtaleland OPPRETTET
            if (opprettSak("MEL-1012", Sakstyper.TRYGDEAVTALE, Sakstemaer.MEDLEMSKAP_LOVVALG,
                    behandlingsstatus = Behandlingsstatus.OPPRETTET)) created++

            // MEL-1013 til MEL-1022: Utenfor avtaleland UNDER_BEHANDLING
            (1013..1022).forEach {
                if (opprettSak("MEL-$it", Sakstyper.FTRL, Sakstemaer.MEDLEMSKAP_LOVVALG)) created++
            }

            // MEL-1023 til MEL-1050: Utenfor avtaleland med årsavregning
            (1023..1050).forEach {
                if (opprettSak("MEL-$it", Sakstyper.FTRL, Sakstemaer.MEDLEMSKAP_LOVVALG,
                        behandlingstype = Behandlingstyper.ÅRSAVREGNING)) created++
            }

            // MEL-1051 til MEL-1053: EU/EØS IKKE_YRKESAKTIV
            (1051..1053).forEach {
                if (opprettSak("MEL-$it", Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG,
                        behandlingstema = Behandlingstema.IKKE_YRKESAKTIV)) created++
            }

            // MEL-1054 til MEL-1056: EU/EØS Pensjonist med trygdeavgift
            (1054..1056).forEach {
                if (opprettSak("MEL-$it", Sakstyper.EU_EOS, Sakstemaer.TRYGDEAVGIFT,
                        behandlingstema = Behandlingstema.PENSJONIST)) created++
            }

            // MEL-1057 til MEL-1058: Avtaleland OPPRETTET
            (1057..1058).forEach {
                if (opprettSak("MEL-$it", Sakstyper.TRYGDEAVTALE, Sakstemaer.MEDLEMSKAP_LOVVALG,
                        behandlingsstatus = Behandlingsstatus.OPPRETTET)) created++
            }

            // MEL-1059 til MEL-1060: Utenfor avtaleland OPPRETTET
            (1059..1060).forEach {
                if (opprettSak("MEL-$it", Sakstyper.FTRL, Sakstemaer.MEDLEMSKAP_LOVVALG,
                        behandlingsstatus = Behandlingsstatus.OPPRETTET)) created++
            }

            // MEL-1061: EU/EØS IKKE_YRKESAKTIV OPPRETTET
            if (opprettSak("MEL-1061", Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG,
                    behandlingstema = Behandlingstema.IKKE_YRKESAKTIV,
                    behandlingsstatus = Behandlingsstatus.OPPRETTET)) created++

            // MEL-1062: EU/EØS Pensjonist OPPRETTET
            if (opprettSak("MEL-1062", Sakstyper.EU_EOS, Sakstemaer.TRYGDEAVGIFT,
                    behandlingstema = Behandlingstema.PENSJONIST,
                    behandlingsstatus = Behandlingsstatus.OPPRETTET)) created++

            // MEL-1063: Avtaleland AVSLUTTET (HENLAGT)
            if (opprettSak("MEL-1063", Sakstyper.TRYGDEAVTALE, Sakstemaer.MEDLEMSKAP_LOVVALG,
                    behandlingsstatus = Behandlingsstatus.AVSLUTTET,
                    saksstatus = Saksstatuser.HENLAGT)) created++

            // MEL-1064: Avtaleland AVSLUTTET
            if (opprettSak("MEL-1064", Sakstyper.TRYGDEAVTALE, Sakstemaer.MEDLEMSKAP_LOVVALG,
                    behandlingsstatus = Behandlingsstatus.AVSLUTTET)) created++

            // MEL-1065 til MEL-1066: Utenfor avtaleland AVSLUTTET
            (1065..1066).forEach {
                if (opprettSak("MEL-$it", Sakstyper.FTRL, Sakstemaer.MEDLEMSKAP_LOVVALG,
                        behandlingsstatus = Behandlingsstatus.AVSLUTTET)) created++
            }

            // MEL-1067: EU/EØS IKKE_YRKESAKTIV AVSLUTTET
            if (opprettSak("MEL-1067", Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG,
                    behandlingstema = Behandlingstema.IKKE_YRKESAKTIV,
                    behandlingsstatus = Behandlingsstatus.AVSLUTTET)) created++

            // MEL-1068: EU/EØS Pensjonist AVSLUTTET
            if (opprettSak("MEL-1068", Sakstyper.EU_EOS, Sakstemaer.TRYGDEAVGIFT,
                    behandlingstema = Behandlingstema.PENSJONIST,
                    behandlingsstatus = Behandlingsstatus.AVSLUTTET)) created++

            // MEL-1069: Avtaleland for varselmelding-test
            if (opprettSak("MEL-1069", Sakstyper.TRYGDEAVTALE, Sakstemaer.MEDLEMSKAP_LOVVALG)) created++

            // MEL-1070: Utenfor avtaleland AVSLUTTET
            if (opprettSak("MEL-1070", Sakstyper.FTRL, Sakstemaer.MEDLEMSKAP_LOVVALG,
                    behandlingsstatus = Behandlingsstatus.AVSLUTTET)) created++

            // MEL-1071: EU/EØS IKKE_YRKESAKTIV for regresjon-test
            if (opprettSak("MEL-1071", Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG,
                    behandlingstema = Behandlingstema.IKKE_YRKESAKTIV)) created++

            log.info { "Suksessfullt initialisert $created test-saker for testperson: $TEST_FNR" }
            return InitResult(created = created, skipped = TOTAL_CASES - created, alreadyExisted = false)

        } finally {
            ThreadLocalAccessInfo.afterExecuteProcess(processId)
        }
    }

    /**
     * Oppretter en sak med gitte parametere. Returnerer true hvis saken ble opprettet.
     */
    private fun opprettSak(
        saksnummer: String,
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingsstatus: Behandlingsstatus = Behandlingsstatus.UNDER_BEHANDLING,
        behandlingstema: Behandlingstema = Behandlingstema.YRKESAKTIV,
        behandlingstype: Behandlingstyper = Behandlingstyper.FØRSTEGANG,
        saksstatus: Saksstatuser = Saksstatuser.OPPRETTET
    ): Boolean {
        if (fagsakRepository.existsById(saksnummer)) {
            log.debug { "Sak $saksnummer eksisterer allerede" }
            return false
        }

        val fagsak = Fagsak(
            saksnummer,
            null,
            sakstype,
            sakstema,
            saksstatus,
            null,
            HashSet(),
            ArrayList()
        )

        val bruker = Aktoer().apply {
            setFagsak(fagsak)
            personIdent = TEST_FNR
            aktørId = TEST_AKTOR_ID
            rolle = Aktoersroller.BRUKER
        }
        fagsak.leggTilAktør(bruker)

        val savedFagsak = fagsakRepository.save(fagsak)

        val behandling = behandlingService.nyBehandling(
            savedFagsak,
            behandlingsstatus,
            behandlingstype,
            behandlingstema,
            null, null,
            LocalDate.now(),
            Behandlingsaarsaktyper.SØKNAD,
            null
        )

        // Opprett oppgave for OPPRETTET og UNDER_BEHANDLING (ikke for AVSLUTTET)
        if (behandlingsstatus != Behandlingsstatus.AVSLUTTET) {
            oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
                behandling, null, TEST_AKTOR_ID, TEST_SAKSBEHANDLER, null
            )
        }

        log.debug { "$saksnummer: opprettet ($sakstype, $sakstema, $behandlingsstatus)" }
        return true
    }

    data class InitResult(
        val created: Int,
        val skipped: Int,
        val alreadyExisted: Boolean
    )

    data class ResetResult(
        val cleared: Int,
        val created: Int,
        val wasReset: Boolean,
        val metadata: Map<String, SakMetadata>
    )

    data class SakMetadata(
        val behandlingID: Long,
        val sakstype: String,
        val sakstema: String,
        val behandlingstema: String,
        val behandlingstype: String,
        val behandlingsstatus: String
    )

    companion object {
        const val TEST_FNR = "30056928150"
        const val TEST_AKTOR_ID = "1111111111111"
        const val TEST_SAKSBEHANDLER = "Z123456"
        const val FIRST_CASE_ID = "MEL-1001"
        const val LAST_CASE_ID = "MEL-1071"
        const val TOTAL_CASES = 71
    }
}
