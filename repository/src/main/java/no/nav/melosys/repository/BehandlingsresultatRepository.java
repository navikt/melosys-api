package no.nav.melosys.repository;

import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BehandlingsresultatRepository extends JpaRepository<Behandlingsresultat, Long> {

    /**
     * Final targeted UPDATE for type field to ensure correct value after concurrent operations.
     * This should be called as the LAST operation in fattVedtak transactions to override
     * any stale data that may have been flushed by concurrent requests.
     *
     * @see docs/debugging/2025-11-29-REFINED-PLAN-FINAL-UPDATE.md
     */
    @Modifying
    @Query("UPDATE Behandlingsresultat b SET b.type = :type WHERE b.id = :id")
    void finalUpdateType(@Param("id") Long id, @Param("type") Behandlingsresultattyper type);

    @EntityGraph(attributePaths = {"avklartefakta"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<Behandlingsresultat> findWithAvklartefaktaById(Long behandlingID);

    @EntityGraph(attributePaths = {"kontrollresultater"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<Behandlingsresultat> findWithKontrollresultaterById(Long behandlingID);

    @EntityGraph(attributePaths = {"anmodningsperioder"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<Behandlingsresultat> findWithAnmodningsperioderById(Long behandlingID);

    List<Behandlingsresultat> findAllByFakturaserieReferanse(String fakturaserieReferanse);

    @EntityGraph(attributePaths = {"lovvalgsperioder", "medlemskapsperioder"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<Behandlingsresultat> findWithLovvalgOgMedlemskapsperioderById(Long behandlingID);

    @Query(
        """
            SELECT b
            FROM Behandlingsresultat b
            JOIN b.vedtakMetadata vm
            JOIN b.medlemskapsperioder mp
            WHERE YEAR(mp.fom) <= :year AND YEAR(mp.tom) >= :year
        """
    )
    List<Behandlingsresultat> findAllWithVedtakMetadataAndMedlemskapsperiodeOverlappingYear(int year);

    @Query(
        """
            SELECT b
            FROM Behandlingsresultat b
            JOIN b.behandling behandling
            JOIN behandling.fagsak fagsak
            JOIN fagsak.aktører aktør
            WHERE aktør.aktørId = :aktorId
        """
    )
    List<Behandlingsresultat> findAllByAktorId(String aktorId);
}
