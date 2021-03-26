package no.nav.melosys.repository;

import no.nav.melosys.domain.behandling.Behandlingsnotat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BehandlingsnotatRepository extends JpaRepository<Behandlingsnotat, Long> {
}
