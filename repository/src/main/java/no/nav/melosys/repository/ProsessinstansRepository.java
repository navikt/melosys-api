package no.nav.melosys.repository;

import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import org.springframework.data.repository.CrudRepository;

public interface ProsessinstansRepository extends CrudRepository<Prosessinstans, Long> {
    List<Prosessinstans> findByStegIsNot(ProsessSteg steg);
    Optional<Prosessinstans> findByStegIsNotNullAndStegIsNotAndBehandling_Id(ProsessSteg prosessSteg, long id);
    Optional<Prosessinstans> findByTypeAndStegIsNotNullAndStegIsNotAndBehandling_Id(ProsessType prosessType, ProsessSteg prosessSteg, long id);
    List<Prosessinstans> findAllByStegIsNotNullAndStegIsNot(ProsessSteg prosessSteg);
}
