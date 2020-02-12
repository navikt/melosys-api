package no.nav.melosys.repository;

import java.util.Set;

import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Registerkontroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface RegisterkontrollRepository extends JpaRepository<Registerkontroll, Long> {

    @Transactional(propagation = Propagation.SUPPORTS)
    Set<Registerkontroll> findByBehandlingsresultatId(long behandlingsid);

    int deleteByBehandlingsresultat(Behandlingsresultat behandlingsresultat);
}
