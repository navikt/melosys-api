package no.nav.melosys.repository;

import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import org.springframework.data.repository.CrudRepository;

public interface ProsessinstansRepository extends CrudRepository<Prosessinstans, Long> {
    List<Prosessinstans> findByStegIsNot(ProsessSteg steg);
    Optional<Prosessinstans> findByBehandling_IdAndStegIsNotAndStegIsNot(long id, ProsessSteg prosessSteg, ProsessSteg prosessSteg1);
    Optional<Prosessinstans> findByTypeAndBehandling_IdAndStegIsNotAndStegIsNot(ProsessType prosessType, long id, ProsessSteg prosessSteg, ProsessSteg prosessSteg1);
}
