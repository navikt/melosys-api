package no.nav.melosys.repository

import no.nav.melosys.domain.avgift.Aarsavregning
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.util.*

interface AarsavregningRepository : JpaRepository<Aarsavregning, Long> {

    @Query("""
        SELECT COUNT(a.behandlingsresultat_id)
        FROM aarsavregning a
        JOIN behandling b ON a.behandlingsresultat_id = b.id
        WHERE a.aar = :aar
          AND b.saksnummer = (
                SELECT beh.saksnummer
                FROM behandling beh
                WHERE beh.id = :behandlingId
            )
          AND b.beh_type = 'ÅRSAVREGNING'
          AND b.status <> 'AVSLUTTET'
    """, nativeQuery = true)
    fun eksisterendeÅrsavregningFinnesPåÅr(@Param("behandlingId") behandlingId: Long, @Param("aar") år: Int): BigDecimal
}
