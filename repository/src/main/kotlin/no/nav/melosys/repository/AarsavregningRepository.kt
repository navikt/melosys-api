package no.nav.melosys.repository

import no.nav.melosys.domain.avgift.Aarsavregning
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AarsavregningRepository : JpaRepository<Aarsavregning, Long> {

    fun findByBehandlingsresultatId(behandlingID: Long): Aarsavregning?

    @Query(
        """
    SELECT COUNT(DISTINCT a.behandlingsresultat_id)
    FROM behandling b
    JOIN fagsak f ON b.saksnummer = f.saksnummer
    JOIN behandling b2 ON b2.saksnummer = f.saksnummer
    JOIN aarsavregning a ON b2.id = a.behandlingsresultat_id
    WHERE b.id = :behandlingId
      AND a.aar = :aar
      AND b2.beh_type = 'ÅRSAVREGNING'
      AND b2.status != 'AVSLUTTET'
    """, nativeQuery = true
    )
    fun finnAntallÅrsavregningerPåFagsakForÅr(@Param("behandlingId") behandlingId: Long, @Param("aar") år: Int): Int
}
