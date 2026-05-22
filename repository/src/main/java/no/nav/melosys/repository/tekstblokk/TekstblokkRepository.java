package no.nav.melosys.repository.tekstblokk;

import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.tekstblokk.Tekstblokk;
import no.nav.melosys.domain.tekstblokk.TekstblokkType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TekstblokkRepository extends JpaRepository<Tekstblokk, Long> {

    @EntityGraph(attributePaths = "tags")
    List<Tekstblokk> findAllByOrderByTittelAsc();

    @EntityGraph(attributePaths = "tags")
    List<Tekstblokk> findAllByTypeOrderByTittelAsc(TekstblokkType type);

    @EntityGraph(attributePaths = "tags")
    @Override
    Optional<Tekstblokk> findById(Long id);
}
