package no.nav.melosys.service.vedtak

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

/**
 * Datafiks for MELOSYS-8351: legger inn manglende rader i `vedtak_metadata` for vedtak fattet i
 * vedtaksflyten for ikke-yrkesaktive i EØS og trygdeavtale (`IVERKSETT_VEDTAK_IKKE_YRKESAKTIV`).
 * Flyten lagret ikke raden før MELOSYS-8337.
 *
 * Vedtaksdatoen er `behandlingsresultat.endret_dato`. Vedtak der prosessinstansen mangler eller ligger mer
 * enn 3 timer fra den datoen, og vedtak med annen behandlingstype enn førstegang og ny vurdering, holdes
 * utenfor og listes i svaret.
 *
 * Kjør ikke skarpt før melosys-api#3551 (MELOSYS-8337) er i prod. Først da ser årsavregningen, faktureringen og
 * A1-statistikken bort fra ikke-yrkesaktive vedtak som har vedtaksmetadata.
 *
 * Native SQL fordi `registrert_av` og `endret_av` skal være [PATCH_MARKØR]; JPA-auditing ville satt
 * innlogget bruker. Native SQL publiserer heller ingen `VedtakMetadataLagretEvent`. Eneste lytter er
 * A1-statistikken, og ikke-yrkesaktive skal ikke ha A1.
 */
@Component
class IkkeYrkesaktivVedtaksmetadataFiksService {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Transactional(readOnly = true)
    fun forhåndsvis(): IkkeYrkesaktivFiksResultat {
        val grupper = hentGrupper()
        return IkkeYrkesaktivFiksResultat(
            skarp = false,
            antallFunnet = grupper.antallSomFikses(),
            antallInnsatt = 0,
            antallIgjen = grupper.antallSomFikses(),
            grupper = grupper,
            holdesUtenfor = hentHoldesUtenfor(),
        )
    }

    @Transactional
    fun utfør(maksAntallRader: Int): IkkeYrkesaktivFiksResultat {
        require(maksAntallRader in 1..MAKS_ANTALL_RADER) { "maksAntallRader må være mellom 1 og $MAKS_ANTALL_RADER" }

        val antallFør = hentGrupper().antallSomFikses()
        val forventet = minOf(antallFør, maksAntallRader)

        log.info { "Datafiks $PATCH_MARKØR: $antallFør vedtak mangler vedtaksmetadata, legger inn høyst $maksAntallRader" }

        val antallInnsatt = entityManager.createNativeQuery(INSERT_SQL)
            .setParameter("markoer", PATCH_MARKØR)
            .setParameter("maks", maksAntallRader)
            .executeUpdate()

        val grupperEtter = hentGrupper()
        val antallIgjen = grupperEtter.antallSomFikses()
        val avvik = antallInnsatt != forventet || antallIgjen != antallFør - antallInnsatt
        if (avvik) {
            log.warn {
                "Datafiks $PATCH_MARKØR: la inn $antallInnsatt rader, forventet $forventet. " +
                    "$antallIgjen igjen, før kjøringen $antallFør. Dataene er endret under kjøringen; kontroller før neste kall."
            }
        } else {
            log.info { "Datafiks $PATCH_MARKØR: la inn $antallInnsatt rader, $antallIgjen igjen" }
        }

        return IkkeYrkesaktivFiksResultat(
            skarp = true,
            antallFunnet = antallFør,
            antallInnsatt = antallInnsatt,
            antallIgjen = antallIgjen,
            grupper = grupperEtter,
            holdesUtenfor = hentHoldesUtenfor(),
            avvik = avvik,
        )
    }

    /** Sletter bare rader der både `registrert_av` og `endret_av` er [PATCH_MARKØR]. En rad noen har skrevet til siden, blir stående. */
    @Transactional
    fun angre(skarp: Boolean): IkkeYrkesaktivAngreResultat {
        val antallFunnet = tell(ANGRE_TELL_SQL)
        val antallSomIkkeKanAngres = tell(KAN_IKKE_ANGRES_SQL)

        if (!skarp) {
            return IkkeYrkesaktivAngreResultat(
                skarp = false,
                antallFunnet = antallFunnet,
                antallSlettet = 0,
                antallSomIkkeKanAngres = antallSomIkkeKanAngres,
            )
        }

        log.info { "Datafiks $PATCH_MARKØR: sletter $antallFunnet rader, $antallSomIkkeKanAngres er endret etterpå og blir stående" }
        val antallSlettet = entityManager.createNativeQuery(ANGRE_SQL)
            .setParameter("markoer", PATCH_MARKØR)
            .executeUpdate()
        log.info { "Datafiks $PATCH_MARKØR: slettet $antallSlettet rader" }

        return IkkeYrkesaktivAngreResultat(
            skarp = true,
            antallFunnet = antallFunnet,
            antallSlettet = antallSlettet,
            antallSomIkkeKanAngres = antallSomIkkeKanAngres,
            avvik = antallSlettet != antallFunnet,
        )
    }

    private fun List<IkkeYrkesaktivFiksGruppe>.antallSomFikses() =
        filter { it.gruppe == Gruppe.FIKSES.name }.sumOf { it.antall }

    @Suppress("UNCHECKED_CAST")
    private fun hentGrupper(): List<IkkeYrkesaktivFiksGruppe> =
        (entityManager.createNativeQuery(GRUPPE_SQL).resultList as List<Array<Any?>>).map { rad ->
            IkkeYrkesaktivFiksGruppe(
                gruppe = rad[0] as String,
                sakstype = rad[1] as String,
                behandlingstype = rad[2] as String?,
                antall = (rad[3] as Number).toInt(),
                eldsteEndretDato = rad[4] as String?,
                nyesteEndretDato = rad[5] as String?,
            )
        }

    @Suppress("UNCHECKED_CAST")
    private fun hentHoldesUtenfor(): List<IkkeYrkesaktivHoldtUtenfor> =
        (entityManager.createNativeQuery(HOLDES_UTENFOR_SQL).resultList as List<Array<Any?>>).map { rad ->
            IkkeYrkesaktivHoldtUtenfor(
                gruppe = rad[0] as String,
                saksnummer = rad[1] as String,
                behandlingId = (rad[2] as Number).toLong(),
                sakstype = rad[3] as String,
                behandlingstype = rad[4] as String?,
                endretDato = rad[5] as String?,
                prosessinstansDato = rad[6] as String?,
            )
        }

    private fun tell(sql: String): Int =
        (entityManager.createNativeQuery(sql).setParameter("markoer", PATCH_MARKØR).singleResult as Number).toInt()

    /** Gruppene i svaret. Samme navn som i Q-1 på planen for MELOSYS-8351, så tallene kan sammenlignes direkte. */
    enum class Gruppe { FIKSES, UTEN_PROSESSINSTANS, AVVIK_OVER_3_TIMER, ANNEN_BEHANDLINGSTYPE }

    companion object {
        const val PATCH_MARKØR = "MELOSYS-8351-PATCH"
        const val MAKS_ANTALL_RADER = 500

        /**
         * Én rad per avsluttet ikke-yrkesaktivt vedtak i EØS eller trygdeavtale uten vedtaksmetadata.
         * Prosessinstansen sammenlignes med den eldste per behandling. Bare FØRSTEGANG og NY_VURDERING
         * har en kjent vedtakstype; andre behandlingstyper holdes utenfor.
         */
        private val KANDIDATER = """
            SELECT b.id AS behandling_id,
                   b.saksnummer,
                   f.fagsak_type,
                   b.beh_type,
                   br.endret_dato,
                   pi.registrert_dato AS prosessinstans_dato,
                   CASE
                       WHEN pi.behandling_id IS NULL THEN '${Gruppe.UTEN_PROSESSINSTANS}'
                       WHEN ABS(CAST(br.endret_dato AS DATE) - CAST(pi.registrert_dato AS DATE)) * 24 > 3 THEN '${Gruppe.AVVIK_OVER_3_TIMER}'
                       WHEN b.beh_type = 'FØRSTEGANG' OR b.beh_type = 'NY_VURDERING' THEN '${Gruppe.FIKSES}'
                       ELSE '${Gruppe.ANNEN_BEHANDLINGSTYPE}'
                   END AS gruppe
            FROM behandling b
            JOIN fagsak f               ON f.saksnummer = b.saksnummer
            JOIN behandlingsresultat br ON br.behandling_id = b.id
            LEFT JOIN (
                SELECT behandling_id, MIN(registrert_dato) AS registrert_dato
                FROM prosessinstans
                WHERE prosess_type = 'IVERKSETT_VEDTAK_IKKE_YRKESAKTIV'
                GROUP BY behandling_id
            ) pi ON pi.behandling_id = b.id
            WHERE b.status = 'AVSLUTTET'
              AND f.fagsak_type IN ('EU_EOS', 'TRYGDEAVTALE')
              AND b.beh_tema = 'IKKE_YRKESAKTIV'
              AND br.fastsatt_av_land = 'NO'
              AND br.resultat_type IN ('FASTSATT_LOVVALGSLAND', 'MEDLEM_I_FOLKETRYGDEN')
              AND NOT EXISTS (SELECT 1 FROM vedtak_metadata vm WHERE vm.behandlingsresultat_id = br.behandling_id)
        """

        private val GRUPPE_SQL = """
            SELECT k.gruppe, k.fagsak_type, k.beh_type, COUNT(*),
                   TO_CHAR(MIN(k.endret_dato), 'YYYY-MM-DD HH24:MI:SS'),
                   TO_CHAR(MAX(k.endret_dato), 'YYYY-MM-DD HH24:MI:SS')
            FROM ($KANDIDATER) k
            GROUP BY k.gruppe, k.fagsak_type, k.beh_type
            ORDER BY k.gruppe, k.fagsak_type, k.beh_type
        """

        private val HOLDES_UTENFOR_SQL = """
            SELECT k.gruppe, k.saksnummer, k.behandling_id, k.fagsak_type, k.beh_type,
                   TO_CHAR(k.endret_dato, 'YYYY-MM-DD HH24:MI:SS'),
                   TO_CHAR(k.prosessinstans_dato, 'YYYY-MM-DD HH24:MI:SS')
            FROM ($KANDIDATER) k
            WHERE k.gruppe <> '${Gruppe.FIKSES}'
            ORDER BY k.gruppe, k.endret_dato, k.behandling_id
        """

        /** Eldste først, så porsjonene kommer i fast rekkefølge. Klagefrist 42 dager som i datafiksen for MELOSYS-8174. */
        private val INSERT_SQL = """
            INSERT INTO vedtak_metadata
                (behandlingsresultat_id, vedtak_dato, vedtak_klagefrist, vedtak_type,
                 registrert_dato, endret_dato, registrert_av, endret_av)
            SELECT s.behandling_id,
                   s.endret_dato,
                   TRUNC(CAST(s.endret_dato AS DATE)) + 42,
                   CASE s.beh_type WHEN 'FØRSTEGANG' THEN 'FØRSTEGANGSVEDTAK' WHEN 'NY_VURDERING' THEN 'ENDRINGSVEDTAK' END,
                   SYSTIMESTAMP, SYSTIMESTAMP, :markoer, :markoer
            FROM (
                SELECT k.behandling_id, k.endret_dato, k.beh_type
                FROM ($KANDIDATER) k
                WHERE k.gruppe = '${Gruppe.FIKSES}'
                ORDER BY k.endret_dato, k.behandling_id
            ) s
            WHERE ROWNUM <= :maks
        """

        private const val ANGRE_TELL_SQL = """
            SELECT COUNT(*) FROM vedtak_metadata WHERE registrert_av = :markoer AND endret_av = :markoer
        """

        /** `IS NULL` fordi både `=` og `<>` gir UNKNOWN mot NULL; en rad med tømt `endret_av` skal telles her. */
        private const val KAN_IKKE_ANGRES_SQL = """
            SELECT COUNT(*) FROM vedtak_metadata
            WHERE registrert_av = :markoer AND (endret_av IS NULL OR endret_av <> :markoer)
        """

        private const val ANGRE_SQL = """
            DELETE FROM vedtak_metadata WHERE registrert_av = :markoer AND endret_av = :markoer
        """
    }
}

data class IkkeYrkesaktivFiksGruppe(
    val gruppe: String,
    val sakstype: String,
    val behandlingstype: String?,
    val antall: Int,
    val eldsteEndretDato: String?,
    val nyesteEndretDato: String?,
)

data class IkkeYrkesaktivHoldtUtenfor(
    val gruppe: String,
    val saksnummer: String,
    val behandlingId: Long,
    val sakstype: String,
    val behandlingstype: String?,
    val endretDato: String?,
    /** Eldste `IVERKSETT_VEDTAK_IKKE_YRKESAKTIV` for behandlingen. Null i gruppen UTEN_PROSESSINSTANS. */
    val prosessinstansDato: String?,
)

data class IkkeYrkesaktivFiksResultat(
    val skarp: Boolean,
    /** Vedtak i gruppen FIKSES før kjøringen. */
    val antallFunnet: Int,
    val antallInnsatt: Int,
    /** Vedtak i gruppen FIKSES etter kjøringen. Gjenta skarpe kall til denne er 0. */
    val antallIgjen: Int,
    /** Alle vedtak uten vedtaksmetadata per gruppe, sakstype og behandlingstype, etter kjøringen. */
    val grupper: List<IkkeYrkesaktivFiksGruppe>,
    val holdesUtenfor: List<IkkeYrkesaktivHoldtUtenfor>,
    /** Antall innsatt eller igjen stemmer ikke med tellingen før kjøringen. */
    val avvik: Boolean = false,
    val markoer: String = IkkeYrkesaktivVedtaksmetadataFiksService.PATCH_MARKØR,
)

data class IkkeYrkesaktivAngreResultat(
    val skarp: Boolean,
    val antallFunnet: Int,
    val antallSlettet: Int,
    /** Rader fra datafiksen som er endret etterpå. De slettes ikke. */
    val antallSomIkkeKanAngres: Int,
    val avvik: Boolean = false,
    val markoer: String = IkkeYrkesaktivVedtaksmetadataFiksService.PATCH_MARKØR,
)
