package no.nav.melosys.repository;

import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Behandlingsresultat;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BehandlingsresultatRepository extends JpaRepository<Behandlingsresultat, Long> {
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
            JOIN b.medlemskapsperioder mp
            WHERE YEAR(mp.fom) <= :year AND YEAR(mp.tom) >= :year
        """
    )
    List<Behandlingsresultat> findAllWithMedlemskapsperiodeOverlappingYear(int year);

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
