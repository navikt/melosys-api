package no.nav.melosys.repository;

import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.ProsessType;
import org.springframework.data.repository.CrudRepository;

import no.nav.melosys.domain.Prosessinstans;

public interface ProsessinstansRepository extends CrudRepository<Prosessinstans, Long> {
    List<Prosessinstans> findByBehandling_Id(long id);
    Optional<Prosessinstans> findByBehandling_Id_And_StegIsNotNull(long id);
    Optional<Prosessinstans> findByBehandling_Id_And_Type(long id, ProsessType prosessType);
}
