package no.nav.melosys.repository;

import java.util.Set;

import no.nav.melosys.domain.behandling.Behandlingsresultat;
import no.nav.melosys.domain.Kontrollresultat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface KontrollresultatRepository extends JpaRepository<Kontrollresultat, Long> {

    @Transactional(propagation = Propagation.SUPPORTS)
    Set<Kontrollresultat> findByBehandlingsresultatId(long behandlingsid);

    int deleteByBehandlingsresultat(Behandlingsresultat behandlingsresultat);
}
