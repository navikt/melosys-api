package no.nav.melosys.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import no.nav.melosys.domain.Prosessinstans;

public interface ProsessinstansRepository extends CrudRepository<Prosessinstans, Long> {
    List<Prosessinstans> findByBehandling_Id(long id);
}
