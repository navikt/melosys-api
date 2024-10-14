package no.nav.melosys.repository;

import java.util.List;

import no.nav.melosys.domain.Lovvalgsperiode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface LovvalgsperiodeRepository extends JpaRepository<Lovvalgsperiode, Long> {

    @Transactional(propagation = Propagation.SUPPORTS)
    List<Lovvalgsperiode> findByBehandlingsresultatId(long id);

    @Modifying
    @Query("DELETE from Lovvalgsperiode where behandlingsresultat.id = :behandlingsresultatId")
    void deleteByBehandlingsresultatId(@Param("behandlingsresultatId") Long behandlingsresultatId);
}
