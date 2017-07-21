package no.nav.melosys.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingStatus;

public interface BehandlingRepository extends CrudRepository<Behandling, Long> {

    List<Behandling> findByStatusNot(BehandlingStatus status);

}
