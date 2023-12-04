package no.nav.melosys.repository;

import no.nav.melosys.domain.Behandlingsresultat;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface BehandlingsresultatRepository extends CrudRepository<Behandlingsresultat, Long> {
    @EntityGraph(attributePaths = {"avklartefakta"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<Behandlingsresultat> findWithAvklartefaktaById(Long behandlingID);

    @EntityGraph(attributePaths = {"kontrollresultater"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<Behandlingsresultat> findWithKontrollresultaterById(Long behandlingID);

    @EntityGraph(attributePaths = {"anmodningsperioder"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<Behandlingsresultat> findWithAnmodningsperioderById(Long behandlingID);

    List<Behandlingsresultat> findAllByFakturaserieReferanse(String fakturaserieReferanse);

    @EntityGraph(attributePaths = {"lovvalgsperioder"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<Behandlingsresultat> findWithLovvalgsperioderById(Long behandlingID);
}
