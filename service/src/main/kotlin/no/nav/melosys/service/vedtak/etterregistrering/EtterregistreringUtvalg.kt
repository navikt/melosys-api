package no.nav.melosys.service.vedtak.etterregistrering

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Avsluttede FTRL-behandlinger med vedtak og minst én innvilget medlemskapsperiode som har fom og slutter
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
                    vedtaksdato = LocalDate.parse(rad[2] as String),
                )
            }

    private companion object {
        // Steget som sender vedtaksmeldingen, kjører bare ved iverksetting, så behandlingen må ha vedtaksdato
        // eller fullført iverksetting. Uten vedtaksdato brukes datoen iverksettingen startet.
        // Henleggelse fjerner ikke de innvilgede periodene, resultattypen kan endres etter vedtaket, og en
        // annullert sak beholder periodene i de tidligere vedtakene. Avslag og ferdigbehandling er ikke vedtak
        // om medlemskap, selv om periodene kan stå igjen.
        val UTVALG_SQL = """
            SELECT b.id, b.saksnummer, TO_CHAR(COALESCE(vm.vedtak_dato, i.startet), 'YYYY-MM-DD')
            FROM behandling b
            JOIN fagsak f ON f.saksnummer = b.saksnummer
            JOIN behandlingsresultat br ON br.behandling_id = b.id
            LEFT JOIN vedtak_metadata vm ON vm.behandlingsresultat_id = b.id
            LEFT JOIN (
                SELECT pi.behandling_id, MIN(pi.registrert_dato) AS startet
                FROM prosessinstans pi
                WHERE pi.prosess_type IN ('IVERKSETT_VEDTAK_FTRL', 'IVERKSETT_VEDTAK_AARSAVREGNING', 'IVERKSETT_VEDTAK_IKKE_YRKESAKTIV')
                  AND pi.status = 'FERDIG'
                GROUP BY pi.behandling_id
            ) i ON i.behandling_id = b.id
            WHERE f.fagsak_type = 'FTRL'
              AND f.status <> 'ANNULLERT'
              AND b.status = 'AVSLUTTET'
              AND br.resultat_type NOT IN ('HENLEGGELSE', 'HENLEGGELSE_BORTFALT', 'ANNULLERT', 'AVSLAG_MANGLENDE_OPPL', 'FERDIGBEHANDLET')
              AND COALESCE(vm.vedtak_dato, i.startet) IS NOT NULL
              AND EXISTS (
                  SELECT 1 FROM medlemskapsperiode mp
                  WHERE mp.behandlingsresultat_id = b.id
                    AND mp.innvilgelse_resultat = 'INNVILGET'
                    AND mp.fom_dato IS NOT NULL
                    AND mp.tom_dato >= :periodeTomFra
              )
            ORDER BY COALESCE(vm.vedtak_dato, i.startet), b.id
        """.trimIndent()
    }
}

data class UtvalgtBehandling(
    val behandlingId: Long,
    val saksnummer: String,
    /** Vedtaksdatoen, ellers datoen iverksettingen startet. */
    val vedtaksdato: LocalDate,
)
