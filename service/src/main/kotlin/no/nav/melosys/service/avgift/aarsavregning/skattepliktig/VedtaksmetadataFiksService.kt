package no.nav.melosys.service.avgift.aarsavregning.skattepliktig

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

/**
 * Datafiks for MELOSYS-8174 (Q4a/Q4b i fiksplanen): setter inn manglende rader i `vedtak_metadata`
 * for avsluttede behandlinger som blokkerer skattepliktig årsavregning.
 *
 * `ÅrsavregningService.hentGjeldendeBehandlingsresultaterForÅrsavregning` kaller
 * `hentVedtakMetadata()` ubetinget i filter/sortering, så én rad uten vedtaksmetadata velter hele
 * saken med «vedtakMetadata er påkrevd for Behandlingsresultat» før den blir faglig vurdert.
 *
 * Fiksen kjøres med native SQL, ikke via JPA, av to grunner:
 *  - `registrert_av`/`endret_av` må bli [PATCH_MARKOER] slik at fiksen kan rulles tilbake i én
 *    setning ([angre]). JPA-auditing ville satt saksbehandler/«MELOSYS» i stedet.
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
            gjenstaaendeUtenMetadata = tellUtenMetadata(saksnummer),
        )
    }

    /** Q4b — datafiksen. Endrer prod. */
    @Transactional
    fun utfoer(saksnummer: List<String>): VedtaksmetadataFiksResultat {
        val kandidater = hentKandidater(saksnummer)
        log.info {
            "Datafiks $PATCH_MARKOER: setter inn ${kandidater.size} rader i vedtak_metadata for saker $saksnummer " +
                "(behandlingsresultat ${kandidater.map { it.behandlingsresultatId }})"
        }

        val antallInnsatt = entityManager.createNativeQuery(INSERT_SQL)
            .setParameter("resultattyper", RESULTATTYPER)
            .setParameter("saksnummer", saksnummer)
            .setParameter("markoer", PATCH_MARKOER)
            .executeUpdate()

        if (antallInnsatt != kandidater.size) {
            log.warn {
                "Datafiks $PATCH_MARKOER: satte inn $antallInnsatt rader, men forhåndsvisningen viste " +
                    "${kandidater.size}. Kontroller resultatet før du går videre."
            }
        }
        log.info { "Datafiks $PATCH_MARKOER: satte inn $antallInnsatt rader" }

        return VedtaksmetadataFiksResultat(
            skarp = true,
            saksnummer = saksnummer,
            antallRaderFunnet = kandidater.size,
            antallRaderInnsatt = antallInnsatt,
            rader = kandidater,
            gjenstaaendeUtenMetadata = tellUtenMetadata(saksnummer),
        )
    }

    /** Angreknappen: sletter alle rader fiksen har satt inn, uansett sak. */
    @Transactional
    fun angre(): Int {
        val antallSlettet = entityManager.createNativeQuery(
            "DELETE FROM vedtak_metadata WHERE registrert_av = :markoer"
        ).setParameter("markoer", PATCH_MARKOER).executeUpdate()
        log.info { "Datafiks $PATCH_MARKOER: rullet tilbake $antallSlettet rader" }
        return antallSlettet
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

    /** Q6a — etterkontroll: hvor mange defekte rader står igjen per sak. Skal være tom etter fiksen. */
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
         * MEL-409394 er tatt ut av scope — den er avklart separat (år-løs årsavregning, ikke
         * manglende vedtaksmetadata).
         */
        val STANDARD_SAKER = listOf("MEL-448193", "MEL-545776", "MEL-632908")

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

        private const val ETTERKONTROLL_SQL = """
            SELECT b.saksnummer, COUNT(*)
            FROM behandling b
            JOIN behandlingsresultat br ON br.behandling_id = b.id
            $KANDIDAT_WHERE
            GROUP BY b.saksnummer
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

data class VedtaksmetadataFiksResultat(
    val skarp: Boolean,
    val saksnummer: List<String>,
    val antallRaderFunnet: Int,
    val antallRaderInnsatt: Int,
    val rader: List<VedtaksmetadataFiksRad>,
    val gjenstaaendeUtenMetadata: Map<String, Int>,
    val markoer: String = VedtaksmetadataFiksService.PATCH_MARKOER,
    val angreEndepunkt: String = "POST /admin/aarsavregninger/saker/skattepliktige/vedtaksmetadata-fiks/angre",
)
