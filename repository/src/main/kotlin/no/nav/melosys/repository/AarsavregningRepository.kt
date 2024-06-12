package no.nav.melosys.repository

import no.nav.melosys.domain.avgift.Aarsavregning
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface AarsavregningRepository : JpaRepository<Aarsavregning, Long> {

    fun findByIdAndAar(id: Long, aar: Int): Optional<Aarsavregning>

    @Query("""
        SELECT COUNT(a.id) > 0
        FROM aarsavregning a
        JOIN behandling b ON a.id = b.id
        WHERE a.aar = :year
          AND b.saksnummer = (SELECT beh.fagsak_saksnummer FROM behandling beh WHERE beh.id = :behandlingId)
          AND b.beh_type = 'ÅRSAVREGNING'
          AND b.status <> 'AVSLUTTET'
    """, nativeQuery = true)
    fun existsAarsavregningByBehandlingAndYear(@Param("behandlingId") behandlingId: Long, @Param("year") year: Int): Boolean
}
