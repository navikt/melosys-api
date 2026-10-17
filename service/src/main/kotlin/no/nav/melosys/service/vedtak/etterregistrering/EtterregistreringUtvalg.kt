package no.nav.melosys.service.vedtak.etterregistrering

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Samme utvalg som Q-14 i melosys-kode-wiki, så simuleringen kan sammenlignes med spørringen:
 * avsluttede FTRL-behandlinger med minst én innvilget medlemskapsperiode som har fom og slutter
 * på eller etter [periodeTomFra]. Bare slike perioder registrerer melosys-skattehendelser.
 */
@Component
class EtterregistreringUtvalg {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Suppress("UNCHECKED_CAST")
    @Transactional(readOnly = true)
    fun finnBehandlinger(periodeTomFra: LocalDate): List<UtvalgtBehandling> =
        (entityManager.createNativeQuery(UTVALG_SQL)
            .setParameter("periodeTomFra", periodeTomFra)
            .resultList as List<Array<Any?>>)
            .map { rad ->
                UtvalgtBehandling(
                    behandlingId = (rad[0] as Number).toLong(),
                    saksnummer = rad[1] as String,
                    vedtaksdato = (rad[2] as String?)?.let(LocalDate::parse),
                )
            }

    private companion object {
        // Uten vedtaksdato brukes datoen iverksettingen startet, som i Q-14.
        val UTVALG_SQL = """
            SELECT b.id, b.saksnummer, TO_CHAR(COALESCE(vm.vedtak_dato, i.startet), 'YYYY-MM-DD')
            FROM behandling b
            JOIN fagsak f ON f.saksnummer = b.saksnummer
            LEFT JOIN vedtak_metadata vm ON vm.behandlingsresultat_id = b.id
            LEFT JOIN (
                SELECT pi.behandling_id, MIN(pi.registrert_dato) AS startet
                FROM prosessinstans pi
                WHERE pi.prosess_type IN ('IVERKSETT_VEDTAK_FTRL', 'IVERKSETT_VEDTAK_AARSAVREGNING', 'IVERKSETT_VEDTAK_IKKE_YRKESAKTIV')
                  AND pi.status = 'FERDIG'
                GROUP BY pi.behandling_id
            ) i ON i.behandling_id = b.id
            WHERE f.fagsak_type = 'FTRL'
              AND b.status = 'AVSLUTTET'
              AND EXISTS (
                  SELECT 1 FROM medlemskapsperiode mp
                  WHERE mp.behandlingsresultat_id = b.id
                    AND mp.innvilgelse_resultat = 'INNVILGET'
                    AND mp.fom_dato IS NOT NULL
                    AND mp.tom_dato >= :periodeTomFra
              )
            ORDER BY COALESCE(vm.vedtak_dato, i.startet) NULLS LAST, b.id
        """.trimIndent()
    }
}

data class UtvalgtBehandling(
    val behandlingId: Long,
    val saksnummer: String,
    /** Vedtaksdatoen, ellers datoen iverksettingen startet. Null når ingen av dem finnes. */
    val vedtaksdato: LocalDate?,
)
