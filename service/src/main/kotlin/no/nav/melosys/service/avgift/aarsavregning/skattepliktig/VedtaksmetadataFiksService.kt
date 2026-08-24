package no.nav.melosys.service.avgift.aarsavregning.skattepliktig

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

/**
 * Kastes når en skarp kjøring avvises av en sikkerhetssele. Controlleren gjør dette om til 400.
 */
class VedtaksmetadataFiksAvvist(melding: String) : RuntimeException(melding)

/**
 * Datafiks for MELOSYS-8174 (Q4a/Q4b i fiksplanen): setter inn manglende rader i `vedtak_metadata`
 * for avsluttede behandlinger som blokkerer skattepliktig årsavregning.
 *
 * `ÅrsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning` kaller
 * `hentVedtakMetadata()` ubetinget i filter/sortering, så én rad uten vedtaksmetadata velter hele
 * saken med «vedtakMetadata er påkrevd for Behandlingsresultat» før den blir faglig vurdert.
 *
 * Fiksen kjøres med native SQL, ikke via JPA, av to grunner:
 *  - `registrert_av`/`endret_av` må bli [PATCH_MARKOER] slik at fiksen kan rulles tilbake ([angre]).
 *    JPA-auditing ville satt saksbehandler/«MELOSYS» i stedet.
 *  - `vedtak_dato` skal være proxyen `behandlingsresultat.endret_dato`, ikke `Instant.now()` som
 *    `Behandlingsresultat.settVedtakMetadata` bruker.
 *
 * Innsettingen er idempotent (`NOT EXISTS`), så en utilsiktet ny kjøring gir null nye rader.
 */
@Component
class VedtaksmetadataFiksService {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    /** Q4a — read-only preview av nøyaktig hvilke rader [utfoer] vil sette inn. */
    @Transactional(readOnly = true)
    fun forhaandsvis(saksnummer: List<String>): VedtaksmetadataFiksResultat {
        val kandidater = hentKandidater(saksnummer)
        return VedtaksmetadataFiksResultat(
            skarp = false,
            saksnummer = saksnummer,
            antallRaderFunnet = kandidater.size,
            antallRaderInnsatt = 0,
            rader = kandidater,
            utenMetadataPerSak = tellUtenMetadata(saksnummer),
            ukjentBehType = kandidater.filterNot { it.behType in KJENTE_BEH_TYPER }.map { it.behandlingsresultatId },
            sorteringspaavirkning = sorteringspaavirkning(saksnummer),
        )
    }

    /**
     * Q4b — datafiksen. Endrer prod.
     *
     * Tre sikkerhetsseler, alle bevisste etter review 20.08:
     *  - tomt scope gjør ingenting i stedet for å falle tilbake på en default,
     *  - kandidatantallet må ligge innenfor [maksAntallRader] (samme rolle som `maksAntall` på `/run`),
     *  - alle kandidater må ha en `beh_type` vi vet hvilken vedtakstype hører til; ellers ville
     *    ELSE-grenen i SQL-en stilltiende skrevet ENDRINGSVEDTAK på f.eks. en KLAGE.
     */
    @Transactional
    fun utfoer(saksnummer: List<String>, maksAntallRader: Int): VedtaksmetadataFiksResultat {
        if (saksnummer.isEmpty()) throw VedtaksmetadataFiksAvvist("Skarp kjøring krever eksplisitt saksnummer-liste")

        val kandidater = hentKandidater(saksnummer)
        // Måles før innsettingen: etterpå er patch-radene ekte rader i vedtak_metadata, og
        // «før»-bildet kan ikke gjenskapes.
        val paavirkning = sorteringspaavirkning(saksnummer)
        if (kandidater.size > maksAntallRader) {
            throw VedtaksmetadataFiksAvvist(
                "Fant ${kandidater.size} rader, men maksAntallRader er $maksAntallRader. " +
                    "Kjør preview først, og hev maksAntallRader eksplisitt hvis tallet er riktig."
            )
        }
        val ukjente = kandidater.filterNot { it.behType in KJENTE_BEH_TYPER }
        if (ukjente.isNotEmpty()) {
            throw VedtaksmetadataFiksAvvist(
                "Kandidater med ukjent beh_type (vedtakstype kan ikke utledes trygt): " +
                    ukjente.joinToString { "${it.behandlingsresultatId}=${it.behType}" }
            )
        }

        paavirkning.filter { it.patchenVinnerNyeste }.forEach {
            log.warn {
                "Datafiks $PATCH_MARKOER: for sak ${it.saksnummer} blir en patchet rad " +
                    "(behandlingsresultat ${it.nyestePatchetId}, ${it.nyestePatchetDato}) nyere enn den nyeste " +
                    "raden med ekte vedtaksdato (${it.nyesteFoerId}, ${it.nyesteFoerDato}). Da bytter " +
                    "ÅrsavregningService hvilken behandling avgiftsgrunnlaget hentes fra."
            }
        }

        log.info {
            "Datafiks $PATCH_MARKOER: setter inn ${kandidater.size} rader i vedtak_metadata for saker $saksnummer " +
                "(behandlingsresultat ${kandidater.map { it.behandlingsresultatId }})"
        }

        val antallInnsatt = entityManager.createNativeQuery(INSERT_SQL)
            .setParameter("resultattyper", RESULTATTYPER)
            .setParameter("saksnummer", saksnummer)
            .setParameter("markoer", PATCH_MARKOER)
            .executeUpdate()

        val avvik = antallInnsatt != kandidater.size
        if (avvik) {
            log.warn {
                "Datafiks $PATCH_MARKOER: satte inn $antallInnsatt rader, men forhåndsvisningen viste " +
                    "${kandidater.size}. Noe har endret dataene under kjøringen — kontroller før du går videre."
            }
        } else {
            log.info { "Datafiks $PATCH_MARKOER: satte inn $antallInnsatt rader" }
        }

        return VedtaksmetadataFiksResultat(
            skarp = true,
            saksnummer = saksnummer,
            antallRaderFunnet = kandidater.size,
            antallRaderInnsatt = antallInnsatt,
            rader = kandidater,
            utenMetadataPerSak = tellUtenMetadata(saksnummer),
            avvik = avvik,
            sorteringspaavirkning = paavirkning,
        )
    }

    /**
     * Angreknappen. Sletter kun rader der BÅDE `registrert_av` og `endret_av` er [PATCH_MARKOER].
     *
     * `registrert_av` alene er ikke nok: den er `@CreatedBy` og settes kun ved insert, så en rad som
     * senere har fått en ekte vedtaksdato skrevet av en saksbehandler ville blitt slettet med
     * saksbehandlerens data. Slike rader telles i `antallEndretEtterpaa` og røres ikke.
     *
     * Tom `saksnummer`-liste betyr «alle markerte rader»; `skarp = false` viser hva som ville blitt slettet.
     */
    @Transactional
    fun angre(saksnummer: List<String>, skarp: Boolean): VedtaksmetadataAngreResultat {
        val kandidater = hentAngreKandidater(saksnummer)
        val endretEtterpaa = tellEndretEtterpaa(saksnummer)

        if (!skarp) {
            return VedtaksmetadataAngreResultat(
                skarp = false,
                saksnummer = saksnummer,
                antallRaderFunnet = kandidater.size,
                antallSlettet = 0,
                rader = kandidater,
                antallEndretEtterpaa = endretEtterpaa,
            )
        }

        log.info {
            "Datafiks $PATCH_MARKOER: ruller tilbake ${kandidater.size} rader " +
                "(scope=${saksnummer.ifEmpty { listOf("ALLE") }}, urørt fordi de er endret etterpå: $endretEtterpaa)"
        }

        val sql = if (saksnummer.isEmpty()) ANGRE_SQL else ANGRE_SQL + ANGRE_SAKSFILTER
        val query = entityManager.createNativeQuery(sql).setParameter("markoer", PATCH_MARKOER)
        if (saksnummer.isNotEmpty()) query.setParameter("saksnummer", saksnummer)
        val antallSlettet = query.executeUpdate()

        log.info { "Datafiks $PATCH_MARKOER: slettet $antallSlettet rader" }

        return VedtaksmetadataAngreResultat(
            skarp = true,
            saksnummer = saksnummer,
            antallRaderFunnet = kandidater.size,
            antallSlettet = antallSlettet,
            rader = kandidater,
            antallEndretEtterpaa = endretEtterpaa,
            avvik = antallSlettet != kandidater.size,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun hentKandidater(saksnummer: List<String>): List<VedtaksmetadataFiksRad> {
        if (saksnummer.isEmpty()) return emptyList()

        val rader = entityManager.createNativeQuery(PREVIEW_SQL)
            .setParameter("resultattyper", RESULTATTYPER)
            .setParameter("saksnummer", saksnummer)
            .resultList as List<Array<Any?>>

        return rader.map { rad ->
            VedtaksmetadataFiksRad(
                saksnummer = rad[0] as String,
                behandlingsresultatId = (rad[1] as Number).toLong(),
                behType = rad[2] as String?,
                resultatType = rad[3] as String?,
                blirVedtakDato = rad[4] as String?,
                blirKlagefrist = rad[5] as String?,
                blirVedtakType = rad[6] as String,
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    /**
     * Viser hva patchen gjør med vedtaksdato-sorteringen, per sak.
     *
     * `vedtak_dato` er ikke dekorasjon: `ÅrsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning`
     * sorterer på den og plukker den siste som `sisteBehandlingsresultatMedAvgift` — altså behandlingen
     * avgiftsgrunnlaget hentes fra. Fiksen bruker `endret_dato` som proxy, og den er `@LastModifiedDate`:
     * alltid ≥ ekte vedtaksdato, og den driver videre for hver senere skriving på raden. Patchede rader
     * ser derfor systematisk nyere ut enn de er, mens radene som ikke patches beholder ekte dato —
     * sorteringen sammenligner to ulike klokker.
     *
     * Merk hva dette *ikke* er: ingen simulering av avgiftsgrunnlaget. Den faktiske utvelgelsen filtrerer
     * også på år, periodeoverlapp og trygdeavgiftsperioder. Dette er datosorteringen alene — mekanismen
     * som står i fare — og den er ment å gi operatøren grunnlag for å stoppe, ikke en garanti for at alt
     * er trygt når [SorteringspaavirkningRad.patchenVinnerNyeste] er false.
     */
    private fun sorteringspaavirkning(saksnummer: List<String>): List<SorteringspaavirkningRad> {
        if (saksnummer.isEmpty()) return emptyList()

        val foer = nyestePerSak(EKSISTERENDE_NYESTE_SQL, saksnummer)
        val patchet = nyestePerSak(PATCH_NYESTE_SQL, saksnummer)

        return patchet.keys.sorted().map { sak ->
            val foerRad = foer[sak]
            val patchetRad = patchet.getValue(sak)
            SorteringspaavirkningRad(
                saksnummer = sak,
                nyesteFoerId = foerRad?.first,
                nyesteFoerDato = foerRad?.second,
                nyestePatchetId = patchetRad.first,
                nyestePatchetDato = patchetRad.second,
                // Datoformatet er sorterbart som tekst (YYYY-MM-DD HH24:MI:SS).
                patchenVinnerNyeste = foerRad == null || patchetRad.second > foerRad.second,
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun nyestePerSak(sql: String, saksnummer: List<String>): Map<String, Pair<Long, String>> =
        (entityManager.createNativeQuery(sql)
            .setParameter("resultattyper", RESULTATTYPER)
            .setParameter("saksnummer", saksnummer)
            .resultList as List<Array<Any?>>)
            .associate { rad -> (rad[0] as String) to Pair((rad[1] as Number).toLong(), rad[2] as String) }

    private fun hentAngreKandidater(saksnummer: List<String>): List<VedtaksmetadataAngreRad> {
        val sql = if (saksnummer.isEmpty()) ANGRE_PREVIEW_SQL else ANGRE_PREVIEW_SQL + ANGRE_PREVIEW_SAKSFILTER
        val query = entityManager.createNativeQuery(sql).setParameter("markoer", PATCH_MARKOER)
        if (saksnummer.isNotEmpty()) query.setParameter("saksnummer", saksnummer)

        return (query.resultList as List<Array<Any?>>).map { rad ->
            VedtaksmetadataAngreRad(
                saksnummer = rad[0] as String,
                behandlingsresultatId = (rad[1] as Number).toLong(),
                vedtakDato = rad[2] as String?,
                vedtakType = rad[3] as String?,
            )
        }
    }

    private fun tellEndretEtterpaa(saksnummer: List<String>): Int {
        val sql = if (saksnummer.isEmpty()) ENDRET_ETTERPAA_SQL else ENDRET_ETTERPAA_SQL + ANGRE_PREVIEW_SAKSFILTER
        val query = entityManager.createNativeQuery(sql).setParameter("markoer", PATCH_MARKOER)
        if (saksnummer.isNotEmpty()) query.setParameter("saksnummer", saksnummer)
        return (query.singleResult as Number).toInt()
    }

    /** Q6a — hvor mange defekte rader står igjen per sak. Tom etter en vellykket skarp kjøring. */
    @Suppress("UNCHECKED_CAST")
    private fun tellUtenMetadata(saksnummer: List<String>): Map<String, Int> {
        if (saksnummer.isEmpty()) return emptyMap()

        val rader = entityManager.createNativeQuery(ETTERKONTROLL_SQL)
            .setParameter("resultattyper", RESULTATTYPER)
            .setParameter("saksnummer", saksnummer)
            .resultList as List<Array<Any?>>

        return rader.associate { rad -> (rad[0] as String) to (rad[1] as Number).toInt() }
    }

    companion object {
        /** Skrives i registrert_av/endret_av og er nøkkelen angreknappen sletter på. */
        const val PATCH_MARKOER = "MELOSYS-8174-PATCH"

        /**
         * Sakene fra fiksplanen 18.08.2026 som fag har bekreftet har trygdeavgift til Nav for 2024.
         * Brukes kun som default i preview — skarp kjøring krever eksplisitt liste.
         * MEL-409394 er tatt ut av scope; den er avklart separat (år-løs årsavregning, ikke
         * manglende vedtaksmetadata).
         */
        val STANDARD_SAKER = listOf("MEL-448193", "MEL-545776", "MEL-632908")

        /** Maks antall saksnummer per kall. Holder oss også godt unna Oracles grense på 1000 uttrykk i IN. */
        const val MAKS_ANTALL_SAKER = 25

        /** Default tak på antall rader en skarp kjøring får sette inn. Kan heves eksplisitt i requesten. */
        const val DEFAULT_MAKS_ANTALL_RADER = 10

        /**
         * Behandlingstyper vi trygt kan utlede vedtakstype for: FØRSTEGANG gir FØRSTEGANGSVEDTAK,
         * NY_VURDERING og ENDRET_PERIODE gir ENDRINGSVEDTAK (samme mønster som Flyway-patchen V7.6_04).
         * Andre typer — KLAGE, ANKE, SATSENDRING … — har egne vedtakstyper i kodeverket og avvises.
         */
        val KJENTE_BEH_TYPER = listOf("FØRSTEGANG", "NY_VURDERING", "ENDRET_PERIODE")

        /** Resultattypene ÅrsavregningService slår opp på — det er kun disse som kan velte en sak. */
        private val RESULTATTYPER = listOf("FASTSATT_TRYGDEAVGIFT", "FASTSATT_LOVVALGSLAND", "MEDLEM_I_FOLKETRYGDEN")

        /** Delt av preview og insert, slik at forhåndsvisningen treffer nøyaktig de samme radene. */
        private const val KANDIDAT_WHERE = """
            WHERE b.status = 'AVSLUTTET'
              AND br.resultat_type IN (:resultattyper)
              AND NOT EXISTS (SELECT 1 FROM vedtak_metadata vm WHERE vm.behandlingsresultat_id = br.behandling_id)
              AND b.saksnummer IN (:saksnummer)
        """

        /** vedtak_type utledes av beh_type — kodeverkskoden er FØRSTEGANG, ikke FØRSTEGANGSBEHANDLING. */
        private const val VEDTAK_TYPE_CASE =
            "CASE WHEN b.beh_type = 'FØRSTEGANG' THEN 'FØRSTEGANGSVEDTAK' ELSE 'ENDRINGSVEDTAK' END"

        /**
         * vedtak_dato er en proxy: den ekte vedtaksdatoen finnes ikke lenger, så vi bruker
         * behandlingsresultat.endret_dato. Klagefrist +42 dager følger Flyway-patchen V7.6_04.
         * Datoene formateres i SQL for å slippe JDBC-typemapping i rapporten.
         */
        private const val PREVIEW_SQL = """
            SELECT b.saksnummer,
                   br.behandling_id,
                   b.beh_type,
                   br.resultat_type,
                   TO_CHAR(br.endret_dato, 'YYYY-MM-DD HH24:MI:SS'),
                   TO_CHAR(TRUNC(CAST(br.endret_dato AS DATE)) + 42, 'YYYY-MM-DD'),
                   $VEDTAK_TYPE_CASE
            FROM behandling b
            JOIN behandlingsresultat br ON br.behandling_id = b.id
            $KANDIDAT_WHERE
            ORDER BY b.saksnummer, br.endret_dato
        """

        private const val INSERT_SQL = """
            INSERT INTO vedtak_metadata
                (behandlingsresultat_id, vedtak_dato, vedtak_klagefrist, vedtak_type,
                 registrert_dato, endret_dato, registrert_av, endret_av)
            SELECT br.behandling_id,
                   br.endret_dato,
                   TRUNC(CAST(br.endret_dato AS DATE)) + 42,
                   $VEDTAK_TYPE_CASE,
                   SYSTIMESTAMP, SYSTIMESTAMP, :markoer, :markoer
            FROM behandling b
            JOIN behandlingsresultat br ON br.behandling_id = b.id
            $KANDIDAT_WHERE
        """

        /** Nyeste rad med ekte vedtaksdato per sak — klokka patch-radene sammenlignes mot. */
        private const val EKSISTERENDE_NYESTE_SQL = """
            SELECT saksnummer, behandlingsresultat_id, dato FROM (
                SELECT b.saksnummer AS saksnummer,
                       br.behandling_id AS behandlingsresultat_id,
                       TO_CHAR(vm.vedtak_dato, 'YYYY-MM-DD HH24:MI:SS') AS dato,
                       ROW_NUMBER() OVER (PARTITION BY b.saksnummer ORDER BY vm.vedtak_dato DESC) AS rn
                FROM behandling b
                JOIN behandlingsresultat br ON br.behandling_id = b.id
                JOIN vedtak_metadata vm ON vm.behandlingsresultat_id = br.behandling_id
                WHERE b.status = 'AVSLUTTET'
                  AND br.resultat_type IN (:resultattyper)
                  AND b.saksnummer IN (:saksnummer)
            ) WHERE rn = 1
        """

        /** Nyeste kandidat per sak, med datoen patchen faktisk ville skrevet. */
        private const val PATCH_NYESTE_SQL = """
            SELECT saksnummer, behandlingsresultat_id, dato FROM (
                SELECT b.saksnummer AS saksnummer,
                       br.behandling_id AS behandlingsresultat_id,
                       TO_CHAR(br.endret_dato, 'YYYY-MM-DD HH24:MI:SS') AS dato,
                       ROW_NUMBER() OVER (PARTITION BY b.saksnummer ORDER BY br.endret_dato DESC) AS rn
                FROM behandling b
                JOIN behandlingsresultat br ON br.behandling_id = b.id
                $KANDIDAT_WHERE
            ) WHERE rn = 1
        """

        private const val ETTERKONTROLL_SQL = """
            SELECT b.saksnummer, COUNT(*)
            FROM behandling b
            JOIN behandlingsresultat br ON br.behandling_id = b.id
            $KANDIDAT_WHERE
            GROUP BY b.saksnummer
        """

        /** Kun rader som fortsatt er urørte: endret_av flyttes av enhver senere skriving (@LastModifiedBy). */
        private const val ANGRE_PREVIEW_SQL = """
            SELECT b.saksnummer,
                   vm.behandlingsresultat_id,
                   TO_CHAR(vm.vedtak_dato, 'YYYY-MM-DD HH24:MI:SS'),
                   vm.vedtak_type
            FROM vedtak_metadata vm
            JOIN behandling b ON b.id = vm.behandlingsresultat_id
            WHERE vm.registrert_av = :markoer
              AND vm.endret_av = :markoer
        """
        private const val ANGRE_PREVIEW_SAKSFILTER = " AND b.saksnummer IN (:saksnummer)"

        private const val ENDRET_ETTERPAA_SQL = """
            SELECT COUNT(*)
            FROM vedtak_metadata vm
            JOIN behandling b ON b.id = vm.behandlingsresultat_id
            WHERE vm.registrert_av = :markoer
              AND vm.endret_av <> :markoer
        """

        private const val ANGRE_SQL = """
            DELETE FROM vedtak_metadata
            WHERE registrert_av = :markoer
              AND endret_av = :markoer
        """
        private const val ANGRE_SAKSFILTER = """
              AND behandlingsresultat_id IN (SELECT b.id FROM behandling b WHERE b.saksnummer IN (:saksnummer))
        """
    }
}

data class VedtaksmetadataFiksRad(
    val saksnummer: String,
    val behandlingsresultatId: Long,
    val behType: String?,
    val resultatType: String?,
    val blirVedtakDato: String?,
    val blirKlagefrist: String?,
    val blirVedtakType: String,
)

/**
 * Hva patchen gjør med vedtaksdato-sorteringen for én sak.
 *
 * [patchenVinnerNyeste] = true betyr at en oppdiktet dato legger seg øverst i sorteringen og bytter
 * hvilken behandling `ÅrsavregningService` regner som nyest. Da skal saken ha ekte vedtaksdato satt
 * manuelt før den patches — ikke kjøres skarpt.
 */
data class SorteringspaavirkningRad(
    val saksnummer: String,
    /** Null når saken ikke har én eneste rad med ekte vedtaksdato å sammenligne mot. */
    val nyesteFoerId: Long?,
    val nyesteFoerDato: String?,
    val nyestePatchetId: Long,
    val nyestePatchetDato: String,
    val patchenVinnerNyeste: Boolean,
)

data class VedtaksmetadataFiksResultat(
    val skarp: Boolean,
    val saksnummer: List<String>,
    val antallRaderFunnet: Int,
    val antallRaderInnsatt: Int,
    /** Kandidatene fra preview-spørringen. Ved `avvik = true` er dette ikke det samme som det som ble skrevet. */
    val rader: List<VedtaksmetadataFiksRad>,
    /** Q6a-etterkontrollen, per sak: rader uten vedtaksmetadata slik det står nå. Tom = ingen igjen. */
    val utenMetadataPerSak: Map<String, Int>,
    /** Kandidater der vedtakstypen ikke kan utledes trygt. Blokkerer skarp kjøring. */
    val ukjentBehType: List<Long> = emptyList(),
    /** True hvis antall innsatte rader ikke stemmer med forhåndsvisningen. */
    val avvik: Boolean = false,
    /** Per sak: bytter patchen ut hvilken behandling som er nyest i vedtaksdato-sorteringen? */
    val sorteringspaavirkning: List<SorteringspaavirkningRad> = emptyList(),
    val markoer: String = VedtaksmetadataFiksService.PATCH_MARKOER,
    val angreEndepunkt: String = "POST /admin/aarsavregninger/saker/skattepliktige/vedtaksmetadata-fiks/angre",
)

data class VedtaksmetadataAngreRad(
    val saksnummer: String,
    val behandlingsresultatId: Long,
    val vedtakDato: String?,
    val vedtakType: String?,
)

data class VedtaksmetadataAngreResultat(
    val skarp: Boolean,
    /** Tom liste = alle markerte rader, uansett sak. */
    val saksnummer: List<String>,
    val antallRaderFunnet: Int,
    val antallSlettet: Int,
    val rader: List<VedtaksmetadataAngreRad>,
    /** Patch-rader som er endret etterpå (ekte data skrevet oppå) — disse røres aldri. */
    val antallEndretEtterpaa: Int,
    val avvik: Boolean = false,
    val markoer: String = VedtaksmetadataFiksService.PATCH_MARKOER,
)
