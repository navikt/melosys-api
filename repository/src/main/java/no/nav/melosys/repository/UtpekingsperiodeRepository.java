package no.nav.melosys.repository;

import java.util.List;

import no.nav.melosys.domain.Utpekingsperiode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UtpekingsperiodeRepository extends JpaRepository<Utpekingsperiode, Long> {

    List<Utpekingsperiode> findByBehandlingsresultat_Id(Long behandlingID);
}
