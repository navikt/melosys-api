package no.nav.melosys.repository;

import java.util.Optional;

import no.nav.melosys.domain.Behandlingsresultat;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.CrudRepository;

public interface BehandlingsresultatRepository extends CrudRepository<Behandlingsresultat, Long> {
    @EntityGraph(attributePaths={"avklartefakta"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<Behandlingsresultat> findWithAvklartefaktaById(Long behandlingID);

    @EntityGraph(attributePaths={"kontrollresultater"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<Behandlingsresultat> findWithKontrollresultaterById(Long behandlingID);
}
