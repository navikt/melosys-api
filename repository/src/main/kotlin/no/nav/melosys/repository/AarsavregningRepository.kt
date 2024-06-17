package no.nav.melosys.repository

import no.nav.melosys.domain.avgift.Aarsavregning
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface AarsavregningRepository : JpaRepository<Aarsavregning, Long> {

    @Query("""
        SELECT COUNT(a.id) > 0
        FROM aarsavregning a
        JOIN behandling b ON a.id = b.id
        WHERE a.aar = :år
          AND b.saksnummer = (SELECT beh.fagsak_saksnummer FROM behandling beh WHERE beh.id = :behandlingId)
          AND b.beh_type = 'ÅRSAVREGNING'
          AND b.status <> 'AVSLUTTET'
    """, nativeQuery = true)
    fun eksisterendeÅrsavregningFinnesPåÅr(@Param("behandlingId") behandlingId: Long, @Param("aar") år: Int): Boolean
}
