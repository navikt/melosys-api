package no.nav.melosys.repository;

import java.util.List;

import no.nav.melosys.domain.behandling.Behandlingsresultat;
import no.nav.melosys.domain.Lovvalgsperiode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface LovvalgsperiodeRepository extends JpaRepository<Lovvalgsperiode, Long> {

    @Transactional(propagation = Propagation.SUPPORTS)
    List<Lovvalgsperiode> findByBehandlingsresultatId(long id);

    int deleteByBehandlingsresultat(Behandlingsresultat behandlingsresultat);
}
