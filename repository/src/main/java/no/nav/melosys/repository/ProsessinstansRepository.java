package no.nav.melosys.repository;

import java.util.Optional;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import org.springframework.data.repository.CrudRepository;

public interface ProsessinstansRepository extends CrudRepository<Prosessinstans, Long> {
    Optional<Prosessinstans> findByStegIsNotNullAndStegIsNotAndBehandling_Id(ProsessSteg prosessSteg, long id);
    Optional<Prosessinstans> findByStegIsNotNullAndTypeAndBehandling_Id(ProsessType prosessType, long id);
}
