package no.nav.melosys.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingStatus;

public interface BehandlingRepository extends CrudRepository<Behandling, Long> {

    List<Behandling> findByStatusNot(BehandlingStatus status);

    @Query("select b from Behandling b, Fagsak f where b.fagsak.saksnummer = f.saksnummer and f.gsakSaksnummer = ?1") //$NON-NLS-1$
    List<Behandling> findBySaksnummer(String saksnummer);
}
