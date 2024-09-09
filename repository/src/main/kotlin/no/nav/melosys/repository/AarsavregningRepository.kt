package no.nav.melosys.repository

import no.nav.melosys.domain.avgift.Årsavregning
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AarsavregningRepository : JpaRepository<Årsavregning, Long> {

    fun findByBehandlingsresultatId(behandlingID: Long): Årsavregning?

    @Query(
        """
    SELECT COUNT(DISTINCT a.behandlingsresultat_id)
    FROM behandling b
    JOIN fagsak f ON b.saksnummer = f.saksnummer
    JOIN behandling b2 ON b2.saksnummer = f.saksnummer
    JOIN aarsavregning a ON b2.id = a.behandlingsresultat_id
    JOIN behandlingsresultat br ON b2.id = br.behandling_id
    WHERE b.id = :behandlingId
      AND b2.beh_type = 'ÅRSAVREGNING'
      AND b2.id != :behandlingId
      AND br.resultat_type = 'FERDIGBEHANDLET'
    """, nativeQuery = true
    )
    fun finnAndreFerdigbehandledeÅrsavregningerPåFagsak(@Param("behandlingId") behandlingId: Long): Int
}
