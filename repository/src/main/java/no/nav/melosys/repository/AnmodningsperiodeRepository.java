package no.nav.melosys.repository;

import java.util.List;

import no.nav.melosys.domain.Anmodningsperiode;
import org.springframework.data.repository.CrudRepository;

public interface AnmodningsperiodeRepository extends CrudRepository<Anmodningsperiode, Long> {
    List<Anmodningsperiode> findByBehandlingsresultatId(long id);

}
